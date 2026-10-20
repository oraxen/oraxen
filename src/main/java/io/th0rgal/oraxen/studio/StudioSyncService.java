package io.th0rgal.oraxen.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.commands.ReloadCommand;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.studio.StudioApiClient.FileEntry;
import io.th0rgal.oraxen.studio.StudioApiClient.StudioApiException;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Oraxen Studio pairing and live sync over the Studio server API v1.
 * <p>
 * Files move directly between this server and Studio storage through signed URLs,
 * and only files whose SHA-256 differs are transferred. Realtime messages are
 * treated as hints only: before pulling or pushing, the service re-reads its
 * connection state over authenticated HTTP, so a forged broadcast cannot trigger
 * a push. Pushes send the hashes of the last sync so Studio can refuse to
 * overwrite files that were edited in Studio in the meantime.
 */
public class StudioSyncService {

    private static final int DOWNLOAD_BATCH = 500;
    private static final int TRANSFER_THREADS = 4;

    private final OraxenPlugin plugin;
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private final AtomicBoolean checkScheduled = new AtomicBoolean(false);
    // A realtime hint arrived while a sync was running; check again once it finishes.
    private final AtomicBoolean recheckNeeded = new AtomicBoolean(false);
    // Bumped by start()/stop(): work started for an older generation must not install a socket.
    private final AtomicInteger generation = new AtomicInteger();
    private final AtomicBoolean pairingPollInFlight = new AtomicBoolean(false);
    private volatile boolean initialSyncHintShown = false;
    private StudioApiClient client;
    private volatile StudioWebSocketClient activeSocket;
    private volatile SchedulerUtil.ScheduledTask pairingPollTask;
    private volatile String connectedWorkspaceId = "";
    private volatile String connectedWorkspaceName = "";
    private volatile Map<String, FileEntry> pendingWriteBack;

    public StudioSyncService(OraxenPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new PackGeneratedListener(), plugin);
    }

    public void start() {
        stop();
        if (getSecret().isEmpty() || !isAutoSyncEnabled()) return;
        int gen = generation.get();
        SchedulerUtil.runTaskAsync(plugin, () -> connectWorkspaceSocket(gen, 0));
    }

    public void stop() {
        generation.incrementAndGet();
        cancelPairingWatcher();
        StudioWebSocketClient ws = activeSocket;
        activeSocket = null;
        if (ws != null) ws.close();
    }

    /** Called when the plugin is disabled; unlike stop(), also releases the HTTP client. */
    public void shutdown() {
        stop();
        synchronized (this) {
            if (client != null) client.close();
            client = null;
        }
    }

    private void cancelPairingWatcher() {
        SchedulerUtil.ScheduledTask task = pairingPollTask;
        pairingPollTask = null;
        if (task != null) task.cancel();
    }

    public String getStudioBaseUrl() {
        Object val = Settings.STUDIO_URL.getValue();
        return StudioBundleUtils.normalizeStudioUrl(val == null ? "" : val.toString());
    }

    public String getSecret() {
        Object val = Settings.STUDIO_SECRET.getValue();
        return val == null ? "" : val.toString().trim();
    }

    public boolean isAutoSyncEnabled() {
        Object val = Settings.STUDIO_AUTO_SYNC.getValue();
        return val == null || Boolean.parseBoolean(val.toString());
    }

    public boolean isWebSocketConnected() {
        StudioWebSocketClient ws = activeSocket;
        return ws != null && ws.isConnected();
    }

    public String getConnectedWorkspaceId() {
        return connectedWorkspaceId;
    }

    public String getConnectedWorkspaceName() {
        return connectedWorkspaceName;
    }

    public int getLastSyncedRevision() {
        if (connectedWorkspaceId.isEmpty()) return 0;
        return StudioSyncState.load(dataFolder(), connectedWorkspaceId).rev();
    }

    /** One HTTP client per service, recreated if the Studio URL changes and closed in shutdown(). */
    private synchronized StudioApiClient api() {
        String url = getStudioBaseUrl();
        if (client == null || !client.baseUrl().equals(url)) {
            if (client != null) client.close();
            client = new StudioApiClient(url);
        }
        return client;
    }

    /** Ends a pull or push; runs a check that was requested while it was running. */
    private void finishSync() {
        finishSync(syncInProgress, recheckNeeded, this::scheduleCheck);
    }

    static void finishSync(AtomicBoolean syncInProgress, AtomicBoolean recheckNeeded, Runnable onRecheck) {
        syncInProgress.set(false);
        if (recheckNeeded.getAndSet(false)) onRecheck.run();
    }

    /** If a sync is running, defers a recheck to run once it finishes. */
    private boolean deferIfSyncInProgress() {
        return deferIfSyncInProgress(syncInProgress::get, recheckNeeded, this::scheduleCheck);
    }

    static boolean deferIfSyncInProgress(java.util.function.BooleanSupplier isSyncInProgress,
                                         AtomicBoolean recheckNeeded, Runnable onRecheck) {
        if (!isSyncInProgress.getAsBoolean()) return false;
        recheckNeeded.set(true);
        if (!isSyncInProgress.getAsBoolean() && recheckNeeded.compareAndSet(true, false)) {
            onRecheck.run();
        }
        return true;
    }

    @FunctionalInterface
    interface ManifestLoader {
        StudioApiClient.Manifest load() throws IOException, InterruptedException;
    }

    static boolean completePushSyncState(StudioSyncState state, Path dataFolder, String workspaceId, int committedRev,
                                         Map<String, FileEntry> local, boolean force,
                                         ManifestLoader manifestLoader) throws IOException, InterruptedException {
        // Record the uploaded base immediately so a retry does not treat our own
        // uploads as conflicts, but keep the previous revision until any Studio-only
        // files preserved by a non-forced push have been reconciled via pullFromStudio.
        Map<String, String> pushedHashes = StudioFileSync.hashes(local);
        state.recordSync(workspaceId, state.rev(), pushedHashes);
        state.save(dataFolder);

        boolean pullAfter = !force && manifestLoader.load().files().keySet().stream()
                .anyMatch(path -> StudioBundleUtils.isTrackableRelativePath(path) && !local.containsKey(path));
        if (!pullAfter) {
            state.recordSync(workspaceId, committedRev, pushedHashes);
            state.save(dataFolder);
        }
        return pullAfter;
    }

    private Path dataFolder() {
        return plugin.getDataFolder().toPath();
    }

    /** Fails with a clear message if this Studio deployment does not speak API v1. */
    private void ensureApiSupported(StudioApiClient api) throws IOException, InterruptedException {
        List<Integer> versions;
        try {
            versions = api.supportedVersions();
        } catch (StudioApiException e) {
            if (e.status() != 404) throw e; // outage or server error: not a version problem
            throw new UnsupportedStudioException("Oraxen Studio at " + api.baseUrl()
                    + " does not support this version of Oraxen yet (no API v" + StudioApiClient.API_VERSION + ")");
        }
        if (!versions.contains(StudioApiClient.API_VERSION)) {
            throw new UnsupportedStudioException("Oraxen Studio no longer supports API v" + StudioApiClient.API_VERSION
                    + " (supported: " + versions + "). Please update Oraxen.");
        }
    }

    private static final class UnsupportedStudioException extends IOException {
        UnsupportedStudioException(String message) {
            super(message);
        }
    }

    // ---------------------------------------------------------------- live sync

    private void connectWorkspaceSocket(int gen, int attempt) {
        String secret = getSecret();
        if (secret.isEmpty() || gen != generation.get()) return;
        try {
            StudioApiClient api = api();
            ensureApiSupported(api);
            StudioApiClient.Connection connection = api.connection(secret);
            if (gen != generation.get()) return; // stopped or restarted meanwhile
            connectedWorkspaceId = connection.workspaceId();
            connectedWorkspaceName = connection.workspaceName();

            StudioWebSocketClient old = activeSocket;
            if (old != null) old.close();
            StudioWebSocketClient wsClient = new StudioWebSocketClient(
                    connection.realtimeUrl(),
                    connection.channel(),
                    (event, payload) -> scheduleCheck(),
                    () -> {
                        Logs.logSuccess("Connected to Oraxen Studio live sync (" + connection.channel() + ")");
                        // Catch up on anything that happened while we were offline.
                        scheduleCheck();
                    });
            synchronized (this) {
                if (gen != generation.get()) {
                    wsClient.close();
                    return;
                }
                activeSocket = wsClient;
            }
            wsClient.start();
        } catch (UnsupportedStudioException e) {
            Logs.logWarning(e.getMessage());
        } catch (StudioApiException e) {
            if (e.status() == 401 || e.status() == 403) {
                Logs.logWarning("Oraxen Studio live sync is off: " + e.getMessage());
            } else {
                retryConnect(gen, attempt, e.getMessage());
            }
        } catch (Exception e) {
            retryConnect(gen, attempt, e.getMessage());
        }
    }

    /** Studio may be briefly unreachable (deploys, outages): retry with backoff up to 10 minutes apart. */
    private void retryConnect(int gen, int attempt, String reason) {
        long delaySeconds = Math.min(600, 15L << Math.min(attempt, 6));
        Logs.logWarning("Could not reach Oraxen Studio (" + reason + "); retrying in " + delaySeconds + "s.");
        SchedulerUtil.runTaskLaterAsync(plugin, delaySeconds * 20L, () -> connectWorkspaceSocket(gen, attempt + 1));
    }

    /** Coalesces bursts of realtime hints into one authenticated state check. */
    private void scheduleCheck() {
        if (!checkScheduled.compareAndSet(false, true)) return;
        SchedulerUtil.runTaskLaterAsync(plugin, 20L, () -> {
            checkScheduled.set(false);
            checkForChanges();
        });
    }

    private void checkForChanges() {
        String secret = getSecret();
        if (secret.isEmpty() || deferIfSyncInProgress()) return;
        try {
            StudioApiClient.Connection connection = api().connection(secret);
            StudioSyncState state = StudioSyncState.load(dataFolder(), connection.workspaceId());
            if (connection.pushRequestedAt() != null) {
                Logs.logInfo("Oraxen Studio requested this server's files, pushing...");
                pushToStudio(Bukkit.getConsoleSender(), true);
            } else if (state.base() == null) {
                // Never synced with this workspace (just paired, or upgraded from an older
                // Oraxen): replacing local files automatically could destroy the admin's work.
                if (!initialSyncHintShown) {
                    initialSyncHintShown = true;
                    Logs.logWarning("Oraxen Studio is connected but not synced yet. Run /oraxen studio push to upload "
                            + "this server's files, or /oraxen studio pull to download the workspace.");
                }
            } else if (connection.rev() != state.rev()) {
                Logs.logInfo("Oraxen Studio revision #" + connection.rev() + " is available, pulling changes...");
                pullFromStudio(Bukkit.getConsoleSender());
            }
        } catch (StudioApiException e) {
            Logs.logWarning("Oraxen Studio: " + e.getMessage());
        } catch (Exception e) {
            Logs.logWarning("Could not check Oraxen Studio for changes: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ pairing

    public void initiatePairing(@Nullable CommandSender sender) {
        SchedulerUtil.runTaskAsync(plugin, () -> {
            try {
                StudioApiClient api = api();
                ensureApiSupported(api);
                String serverName = Bukkit.getServer().getName();
                StudioApiClient.Pairing pairing = api.startPairing(
                        serverName != null ? serverName : "Oraxen Server", plugin.getPluginMeta().getVersion());

                Logs.logInfo("Oraxen Studio pairing code: " + pairing.code() + " -> " + pairing.connectUrl());
                sendMessage(sender,
                        "<prefix><#55ffa4>Click to connect your server to Oraxen Studio: "
                                + "<click:open_url:'" + pairing.connectUrl() + "'><hover:show_text:'<green>Open Oraxen Studio in your browser'>"
                                + "<#43c9fa><underlined>" + pairing.connectUrl() + "</underlined></#43c9fa></hover></click> "
                                + "<gray>(Code: <#facc43>" + pairing.code() + "</#facc43>)");
                watchForPairingCompletion(api, pairing, sender);
            } catch (Exception e) {
                sendMessage(sender, "<prefix><#fa4943>Failed to start Studio pairing: " + e.getMessage());
            }
        });
    }

    private void watchForPairingCompletion(StudioApiClient api, StudioApiClient.Pairing pairing,
                                           @Nullable CommandSender sender) {
        cancelPairingWatcher();
        AtomicBoolean completed = new AtomicBoolean(false);
        long periodTicks = Math.max(1, pairing.pollIntervalSeconds()) * 20L;
        long deadline = System.currentTimeMillis() + 10 * 60 * 1000L;
        pairingPollTask = SchedulerUtil.runTaskTimerAsync(plugin, periodTicks, periodTicks, () -> {
            if (completed.get() || !pairingPollInFlight.compareAndSet(false, true)) return;
            try {
                pollPairingOnce(api, pairing, sender, completed, deadline);
            } finally {
                pairingPollInFlight.set(false);
            }
        });
    }

    private void pollPairingOnce(StudioApiClient api, StudioApiClient.Pairing pairing, @Nullable CommandSender sender,
                                 AtomicBoolean completed, long deadline) {
        if (System.currentTimeMillis() > deadline) {
            cancelPairingWatcher();
            sendMessage(sender, "<prefix><#facc43>The Studio pairing code expired. Run <#43c9fa>/oraxen studio connect</#43c9fa> again.");
            return;
        }
        try {
            StudioApiClient.PairingResult result = api.pollPairing(pairing);
            if (result != null && completed.compareAndSet(false, true)) {
                finishPairing(result, sender);
            }
        } catch (StudioApiException e) {
            if (e.status() == 404 && completed.compareAndSet(false, true)) {
                cancelPairingWatcher();
                sendMessage(sender, "<prefix><#fa4943>Studio pairing failed: " + e.getMessage());
            }
        } catch (Exception ignored) {
            // Transient network error: keep polling until the code expires.
        }
    }

    private void finishPairing(StudioApiClient.PairingResult result, @Nullable CommandSender sender) {
        cancelPairingWatcher();
        connectedWorkspaceId = result.workspaceId();
        connectedWorkspaceName = result.workspaceName();
        initialSyncHintShown = true; // the success message below already asks for push or pull
        SchedulerUtil.runTask(plugin, () -> {
            Settings.STUDIO_SECRET.setValue(result.token(), true);
            try {
                StudioSyncState.clear(dataFolder());
            } catch (IOException e) {
                Logs.debug(e);
            }
            sendMessage(sender,
                    "<prefix><#55ffa4>Successfully connected server to Oraxen Studio workspace <#43c9fa>"
                            + result.workspaceName() + "</#43c9fa>! Run <#43c9fa>/oraxen studio push</#43c9fa> "
                            + "to upload this server's files, or <#43c9fa>/oraxen studio pull</#43c9fa> to download the workspace.");
            start();
        });
    }

    public void disconnect(@Nullable CommandSender sender) {
        String secret = getSecret();
        stop();
        connectedWorkspaceId = "";
        connectedWorkspaceName = "";
        Settings.STUDIO_SECRET.setValue("", true);
        try {
            StudioSyncState.clear(dataFolder());
        } catch (IOException e) {
            Logs.debug(e);
        }
        if (!secret.isEmpty()) {
            // Revoke the token in Studio too, so a leaked copy of settings.yml is useless.
            SchedulerUtil.runTaskAsync(plugin, () -> {
                try {
                    api().disconnect(secret);
                } catch (Exception e) {
                    Logs.debug(e);
                }
            });
        }
        sendMessage(sender, "<prefix><#55ffa4>Disconnected server from Oraxen Studio.");
    }

    // --------------------------------------------------------------------- push

    public void pushToStudio(@Nullable CommandSender sender) {
        pushToStudio(sender, false);
    }

    public void pushToStudio(@Nullable CommandSender sender, boolean force) {
        String secret = getSecret();
        if (secret.isEmpty()) {
            sendMessage(sender, "<prefix><#fa4943>Server is not connected to Oraxen Studio. Run <#43c9fa>/oraxen studio connect<#fa4943> first.");
            return;
        }
        if (!syncInProgress.compareAndSet(false, true)) {
            sendMessage(sender, "<prefix><#facc43>A Studio sync operation is already in progress.");
            return;
        }
        sendMessage(sender, "<prefix><#529ced>Pushing Oraxen configurations and pack assets to Oraxen Studio...");
        SchedulerUtil.runTaskAsync(plugin, () -> {
            String pushId = null;
            boolean pullAfter = false;
            StudioApiClient api = api();
            try {
                StudioApiClient.Connection connection = api.connection(secret);
                StudioSyncState state = StudioSyncState.load(dataFolder(), connection.workspaceId());
                List<String> unsupported = new ArrayList<>();
                Map<String, FileEntry> local = StudioFileSync.localManifest(dataFolder(), unsupported);
                if (!unsupported.isEmpty()) {
                    Logs.logWarning("Oraxen Studio skips " + unsupported.size() + " file(s) whose names contain %, ? or #: "
                            + String.join(", ", unsupported.subList(0, Math.min(5, unsupported.size()))));
                }

                StudioApiClient.PushPlan plan = api.beginPush(secret, local, state.base(), force);
                pushId = plan.pushId();
                runParallel(plan.uploads(), upload -> {
                    try {
                        api.upload(upload.url(), dataFolder().resolve(upload.path()), connection.storageApiKey());
                    } catch (IOException e) {
                        api.upload(upload.url(), dataFolder().resolve(upload.path()), connection.storageApiKey());
                    }
                });
                StudioApiClient.PushResult result = api.commitPush(secret, plan.pushId());
                pushId = null;

                pullAfter = completePushSyncState(state, dataFolder(), connection.workspaceId(), result.rev(),
                        local, force, () -> api.manifest(secret));
                sendMessage(sender, "<prefix><#55ffa4>Pushed <#43fa8c>" + result.uploaded()
                        + "<#55ffa4> file(s) to Oraxen Studio (revision #" + result.rev() + ", "
                        + plan.unchanged() + " unchanged, " + result.deleted() + " deleted).");
            } catch (StudioApiException e) {
                if ("conflict".equals(e.code())) {
                    sendMessage(sender, conflictMessage(e.details()));
                } else {
                    sendMessage(sender, "<prefix><#fa4943>Studio push failed: " + e.getMessage());
                }
            } catch (Exception e) {
                sendMessage(sender, "<prefix><#fa4943>Failed to push to Oraxen Studio: " + e.getMessage());
                Logs.debug(e);
            } finally {
                if (pushId != null) {
                    // Release the workspace lock instead of leaving it held until it expires.
                    try {
                        api.abortPush(secret, pushId);
                    } catch (Exception abortError) {
                        Logs.debug(abortError);
                    }
                }
                finishSync();
            }
            if (pullAfter) pullFromStudio(sender);
        });
    }

    private static String conflictMessage(JsonObject details) {
        List<String> paths = new ArrayList<>();
        JsonArray conflicts = details.has("conflicts") ? details.getAsJsonArray("conflicts") : new JsonArray();
        for (JsonElement c : conflicts) {
            if (paths.size() == 5) break;
            paths.add(c.getAsJsonObject().get("path").getAsString());
        }
        int count = details.has("conflict_count") ? details.get("conflict_count").getAsInt() : conflicts.size();
        return "<prefix><#facc43>Studio push stopped: " + count + " file(s) were changed in Studio since this "
                + "server last synced (" + String.join(", ", paths) + (count > paths.size() ? ", ..." : "") + "). "
                + "Run <#43c9fa>/oraxen studio pull</#43c9fa> first, or <#43c9fa>/oraxen studio push force</#43c9fa> "
                + "to overwrite Studio (previous versions are kept in Studio's history).";
    }

    // --------------------------------------------------------------------- pull

    public void pullFromStudio(@Nullable CommandSender sender) {
        String secret = getSecret();
        if (secret.isEmpty()) {
            sendMessage(sender, "<prefix><#fa4943>Server is not connected to Oraxen Studio. Run <#43c9fa>/oraxen studio connect<#fa4943> first.");
            return;
        }
        if (!syncInProgress.compareAndSet(false, true)) {
            sendMessage(sender, "<prefix><#facc43>A Studio sync operation is already in progress.");
            return;
        }
        sendMessage(sender, "<prefix><#529ced>Pulling latest changes from Oraxen Studio...");
        SchedulerUtil.runTaskAsync(plugin, () -> {
            boolean reloadScheduled = false;
            try {
                StudioApiClient api = api();
                if (connectedWorkspaceId.isEmpty()) initialSyncHintShown = true;
                StudioApiClient.Connection connection = api.connection(secret);
                StudioSyncState state = StudioSyncState.load(dataFolder(), connection.workspaceId());
                StudioApiClient.Manifest remote = api.manifest(secret);
                Map<String, FileEntry> local = StudioFileSync.localManifest(dataFolder());
                StudioFileSync.PullPlan plan = StudioFileSync.planPull(remote.files(), local, state.base());

                Map<String, String> urls = new HashMap<>();
                List<String> downloads = plan.download();
                for (int i = 0; i < downloads.size(); i += DOWNLOAD_BATCH) {
                    urls.putAll(api.downloadUrls(secret, downloads.subList(i, Math.min(downloads.size(), i + DOWNLOAD_BATCH))));
                }
                List<String> changed = StudioFileSync.applyPull(dataFolder(), plan, remote.files(), (path, target) -> {
                    String url = urls.get(path);
                    if (url == null) throw new IOException("Studio did not provide a download for " + path);
                    api.download(url, target);
                });

                state.recordSync(connection.workspaceId(), remote.rev(), StudioFileSync.hashes(remote.files()));
                state.save(dataFolder());
                if (!plan.localChanges().isEmpty()) {
                    Logs.logWarning("Oraxen Studio replaced " + plan.localChanges().size()
                            + " file(s) that were also edited on this server; the server versions are in plugins/Oraxen/.studio-backup/");
                }
                if (changed.isEmpty()) {
                    sendMessage(sender, "<prefix><#55ffa4>Already up to date with Oraxen Studio (revision #" + remote.rev() + ").");
                } else {
                    reloadScheduled = true;
                    scheduleReloadAfterPull(sender, changed.size(), remote.rev(), remote.files());
                }
            } catch (StudioApiException e) {
                sendMessage(sender, "<prefix><#fa4943>Studio pull failed: " + e.getMessage());
            } catch (Exception e) {
                sendMessage(sender, "<prefix><#fa4943>Failed to pull from Oraxen Studio: " + e.getMessage());
                Logs.debug(e);
            } finally {
                if (!reloadScheduled) finishSync();
            }
        });
    }

    private void scheduleReloadAfterPull(@Nullable CommandSender sender, int changedCount, int rev,
                                         Map<String, FileEntry> pulled) {
        SchedulerUtil.runTask(plugin, () -> {
            try {
                plugin.reloadConfigs();
                ReloadCommand.reloadRecipes(Bukkit.getConsoleSender());
                ReloadCommand.reloadHud(Bukkit.getConsoleSender());
                ReloadCommand.reloadPaintings(Bukkit.getConsoleSender());
                ReloadCommand.reloadDiscs(Bukkit.getConsoleSender());
                ReloadCommand.reloadItems(Bukkit.getConsoleSender());
                ReloadCommand.reloadPack(Bukkit.getConsoleSender());
                for (Player player : Bukkit.getOnlinePlayers()) {
                    SchedulerUtil.runForEntity(player,
                            () -> plugin.getFontManager().sendGlyphTabCompletion(player));
                }
                sendMessage(sender,
                        "<prefix><#55ffa4>Synced <#43fa8c>" + changedCount
                                + "<#55ffa4> changed file(s) from Oraxen Studio (revision #"
                                + rev + ") and refreshed resource pack.");
            } finally {
                finishSync();
            }
            scheduleWriteBack(pulled);
        });
    }

    /**
     * Pack generation rewrites pack/pack.mcmeta in several steps on a worker thread, so
     * the comparison waits for {@link OraxenPackGeneratedEvent} (or a timeout, in case
     * generation fails) before looking at the files.
     */
    private void scheduleWriteBack(Map<String, FileEntry> pulled) {
        if (!Settings.GENERATE.toBool()) {
            SchedulerUtil.runTaskAsync(plugin, () -> writeBackLoaderChanges(pulled));
            return;
        }
        pendingWriteBack = pulled;
        SchedulerUtil.runTaskLaterAsync(plugin, 20L * 60, () -> runPendingWriteBack(pulled));
    }

    private void runPendingWriteBack(Map<String, FileEntry> expected) {
        synchronized (this) {
            if (pendingWriteBack != expected) return;
            pendingWriteBack = null;
        }
        writeBackLoaderChanges(expected);
    }

    private final class PackGeneratedListener implements Listener {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onPackGenerated(OraxenPackGeneratedEvent event) {
            Map<String, FileEntry> pulled = pendingWriteBack;
            // The event fires before the zip is written; give the worker a moment to finish.
            if (pulled != null) SchedulerUtil.runTaskLaterAsync(plugin, 20L, () -> runPendingWriteBack(pulled));
        }
    }

    /**
     * Oraxen rewrites some configs while loading them (for example it stores the
     * characters it assigns to glyphs). Without sending those rewrites back, the next
     * pull would download the Studio version again and Oraxen would rewrite it again.
     * Pushing with the just-pulled hashes as base keeps a Studio edit made meanwhile
     * safe: it shows up as a conflict instead of being overwritten.
     */
    private void writeBackLoaderChanges(Map<String, FileEntry> pulled) {
        try {
            Map<String, FileEntry> local = StudioFileSync.localManifest(dataFolder());
            List<String> rewritten = new ArrayList<>();
            pulled.forEach((path, entry) -> {
                FileEntry now = local.get(path);
                if (now != null && !now.sha256().equalsIgnoreCase(entry.sha256())) rewritten.add(path);
            });
            if (rewritten.isEmpty()) return;
            Logs.logInfo("Oraxen updated " + rewritten.size() + " synced file(s) while loading them ("
                    + String.join(", ", rewritten.subList(0, Math.min(3, rewritten.size())))
                    + "); sending them back to Oraxen Studio.");
            pushToStudio(null, false);
        } catch (IOException e) {
            Logs.debug(e);
        }
    }

    // ------------------------------------------------------------------ helpers

    @FunctionalInterface
    private interface Transfer<T> {
        void run(T item) throws IOException, InterruptedException;
    }

    private static <T> void runParallel(List<T> items, Transfer<T> transfer) throws IOException, InterruptedException {
        if (items.isEmpty()) return;
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(TRANSFER_THREADS, items.size()), r -> {
            Thread t = new Thread(r, "Oraxen-Studio-Transfer");
            t.setDaemon(true);
            return t;
        });
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (T item : items) {
                futures.add(pool.submit(() -> {
                    transfer.run(item);
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof IOException io) throw io;
                    if (cause instanceof InterruptedException ie) throw ie;
                    throw new IOException(cause);
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private void sendMessage(@Nullable CommandSender sender, String miniMessage) {
        Runnable sendTask = () -> {
            var component = AdventureUtils.MINI_MESSAGE.deserialize(miniMessage);
            AdventureUtils.sendMessage(Bukkit.getConsoleSender(), component);
            if (sender != null && sender != Bukkit.getConsoleSender()) {
                AdventureUtils.sendMessage(sender, component);
            }
        };
        if (sender instanceof Player player) {
            SchedulerUtil.runForEntity(player, sendTask);
        } else if (SchedulerUtil.isGlobalThread()) {
            sendTask.run();
        } else {
            SchedulerUtil.runTask(plugin, sendTask);
        }
    }
}
