package io.th0rgal.oraxen.studio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.commands.ReloadCommand;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.MultipartBody;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages Oraxen Studio pairing, WebSocket Realtime listening, and bidirectional pull/push.
 */
public class StudioSyncService {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    private final OraxenPlugin plugin;
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private volatile StudioWebSocketClient activeSocket;
    private volatile StudioWebSocketClient pairingSocket;
    private volatile SchedulerUtil.ScheduledTask pairingPollTask;
    private volatile String connectedWorkspaceId = "";
    private volatile int lastSyncedRevision = 0;

    public StudioSyncService(OraxenPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        String secret = getSecret();
        if (secret.isEmpty() || !isAutoSyncEnabled()) {
            return;
        }
        SchedulerUtil.runTaskAsync(plugin, this::connectWorkspaceSocket);
    }

    public void stop() {
        cancelPairingWatcher();
        StudioWebSocketClient ws = activeSocket;
        activeSocket = null;
        if (ws != null) {
            ws.close();
        }
    }

    private void cancelPairingWatcher() {
        SchedulerUtil.ScheduledTask task = pairingPollTask;
        pairingPollTask = null;
        if (task != null) {
            task.cancel();
        }
        StudioWebSocketClient pws = pairingSocket;
        pairingSocket = null;
        if (pws != null) {
            pws.close();
        }
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

    public int getLastSyncedRevision() {
        return lastSyncedRevision;
    }

    private static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private void connectWorkspaceSocket() {
        String secret = getSecret();
        if (secret.isEmpty()) return;

        try (HttpClient client = createHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(getStudioBaseUrl() + "/api/studio/pair"))
                    .timeout(HTTP_TIMEOUT)
                    .header("Authorization", "Bearer " + secret)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Logs.logWarning("Studio connection check returned HTTP " + response.statusCode());
                return;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String realtimeUrl = json.has("realtime_url") ? json.get("realtime_url").getAsString() : "";
            String channel = json.has("channel") ? json.get("channel").getAsString() : "";
            connectedWorkspaceId = json.has("workspace_id") ? json.get("workspace_id").getAsString() : "";

            if (realtimeUrl.isEmpty() || channel.isEmpty()) {
                return;
            }

            StudioWebSocketClient old = activeSocket;
            if (old != null) old.close();

            StudioWebSocketClient wsClient = new StudioWebSocketClient(
                    realtimeUrl,
                    channel,
                    this::handleRealtimeEvent,
                    () -> Logs.logSuccess("Connected to Oraxen Studio live sync (" + channel + ")"));
            activeSocket = wsClient;
            wsClient.start();
        } catch (Exception e) {
            Logs.logWarning("Could not connect to Oraxen Studio live sync: " + e.getMessage());
        }
    }

    private void handleRealtimeEvent(String eventName, JsonObject payload) {
        if ("sync".equalsIgnoreCase(eventName)) {
            int rev = payload.has("rev") && !payload.get("rev").isJsonNull() ? payload.get("rev").getAsInt() : 0;
            Logs.logInfo("Oraxen Studio sync event received (revision #" + rev + "), pulling changes...");
            pullFromStudio(Bukkit.getConsoleSender());
        } else if ("request_push".equalsIgnoreCase(eventName)) {
            Logs.logInfo("Oraxen Studio requested server configuration push...");
            pushToStudio(Bukkit.getConsoleSender());
        }
    }

    public void initiatePairing(@Nullable CommandSender sender) {
        SchedulerUtil.runTaskAsync(plugin, () -> {
            try (HttpClient client = createHttpClient()) {
                JsonObject reqBody = new JsonObject();
                String serverName = Bukkit.getServer().getName();
                reqBody.addProperty("server_name", serverName != null ? serverName : "Oraxen Server");
                reqBody.addProperty("oraxen_version", plugin.getPluginMeta().getVersion());

                HttpRequest request = HttpRequest.newBuilder(URI.create(getStudioBaseUrl() + "/api/studio/pair"))
                        .timeout(HTTP_TIMEOUT)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(reqBody.toString(), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    sendMessage(sender, "<prefix><#fa4943>Failed to initiate Studio pairing (HTTP " + response.statusCode() + ").");
                    return;
                }

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String code = json.get("code").getAsString();
                String connectUrl = json.get("connect_url").getAsString();
                String realtimeUrl = json.has("realtime_url") ? json.get("realtime_url").getAsString() : "";
                String pairChannel = json.has("pair_channel") ? json.get("pair_channel").getAsString() : "oraxen_pair_" + code;

                Logs.logInfo("Oraxen Studio pairing code: " + code + " -> " + connectUrl);
                sendMessage(sender,
                        "<prefix><#55ffa4>Click to connect your server to Oraxen Studio: "
                                + "<click:open_url:'" + connectUrl + "'><hover:show_text:'<green>Open Oraxen Studio in your browser'>"
                                + "<#43c9fa><underlined>" + connectUrl + "</underlined></#43c9fa></hover></click> "
                                + "<gray>(Code: <#facc43>" + code + "</#facc43>)");

                watchForPairingCompletion(code, realtimeUrl, pairChannel, sender);
            } catch (Exception e) {
                sendMessage(sender, "<prefix><#fa4943>Failed to contact Oraxen Studio: " + e.getMessage());
            }
        });
    }

    private void watchForPairingCompletion(
            String code,
            String realtimeUrl,
            String pairChannel,
            @Nullable CommandSender sender) {
        cancelPairingWatcher();
        AtomicBoolean completed = new AtomicBoolean(false);

        if (!realtimeUrl.isEmpty()) {
            StudioWebSocketClient pws = new StudioWebSocketClient(
                    realtimeUrl,
                    pairChannel,
                    (event, payload) -> handlePairingSocketEvent(event, payload, completed, sender),
                    null);
            pairingSocket = pws;
            pws.start();
        }

        // Fallback poll every 2 seconds (40 ticks) for up to 5 minutes
        final int[] attempts = {0};
        pairingPollTask = SchedulerUtil.runTaskTimerAsync(plugin, 40L, 40L,
                () -> pollPairingStatus(code, attempts, completed, sender));
    }

    private void handlePairingSocketEvent(
            String event,
            JsonObject payload,
            AtomicBoolean completed,
            @Nullable CommandSender sender) {
        if ("paired".equalsIgnoreCase(event) && payload.has("secret")) {
            completePairingFromJson(payload, completed, sender);
        }
    }

    private void pollPairingStatus(
            String code,
            int[] attempts,
            AtomicBoolean completed,
            @Nullable CommandSender sender) {
        if (completed.get() || ++attempts[0] > 150) {
            cancelPairingWatcher();
            return;
        }
        try (HttpClient client = createHttpClient()) {
            HttpRequest req = HttpRequest.newBuilder(
                            URI.create(getStudioBaseUrl() + "/api/studio/pair?code=" + code))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return;
            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (json.has("status") && "authorized".equals(json.get("status").getAsString()) && json.has("secret")) {
                completePairingFromJson(json, completed, sender);
            }
        } catch (Exception ignored) {
        }
    }

    private void completePairingFromJson(
            JsonObject json,
            AtomicBoolean completed,
            @Nullable CommandSender sender) {
        if (!completed.compareAndSet(false, true)) return;
        String secret = json.get("secret").getAsString();
        String wsName = json.has("workspace_name") ? json.get("workspace_name").getAsString() : "Workspace";
        String wsId = json.has("workspace_id") ? json.get("workspace_id").getAsString() : "";
        finishPairing(secret, wsId, wsName, sender);
    }

    private void finishPairing(String secret, String workspaceId, String workspaceName, @Nullable CommandSender sender) {
        cancelPairingWatcher();
        connectedWorkspaceId = workspaceId;
        SchedulerUtil.runTask(plugin, () -> {
            Settings.STUDIO_SECRET.setValue(secret, true);
            sendMessage(sender,
                    "<prefix><#55ffa4>Successfully connected server to Oraxen Studio workspace <#43c9fa>"
                            + workspaceName + "</#43c9fa>!");
            start();
        });
    }

    public void disconnect(@Nullable CommandSender sender) {
        stop();
        connectedWorkspaceId = "";
        Settings.STUDIO_SECRET.setValue("", true);
        sendMessage(sender, "<prefix><#55ffa4>Disconnected server from Oraxen Studio.");
    }

    public void pushToStudio(@Nullable CommandSender sender) {
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
            File zipBundle = null;
            try {
                zipBundle = StudioBundleUtils.createWorkspaceZipBundle(plugin.getDataFolder().toPath());
                try (HttpClient client = createHttpClient();
                     MultipartBody body = MultipartBody.create()
                             .addPart("bundle", zipBundle, "oraxen-workspace.zip", "application/zip")) {

                    HttpRequest request = HttpRequest.newBuilder(URI.create(getStudioBaseUrl() + "/api/studio/push"))
                            .timeout(Duration.ofMinutes(2))
                            .header("Authorization", "Bearer " + secret)
                            .header("Content-Type", body.contentType())
                            .POST(body.bodyPublisher())
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200) {
                        sendMessage(sender, "<prefix><#fa4943>Studio push failed (HTTP " + response.statusCode() + ").");
                        return;
                    }

                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    int uploaded = json.has("uploaded_count") ? json.get("uploaded_count").getAsInt() : 0;
                    int rev = json.has("rev") ? json.get("rev").getAsInt() : 0;
                    lastSyncedRevision = rev;
                    sendMessage(sender, "<prefix><#55ffa4>Pushed <#43fa8c>" + uploaded
                            + "<#55ffa4> file(s) to Oraxen Studio (revision #" + rev + ").");
                }
            } catch (Exception e) {
                sendMessage(sender, "<prefix><#fa4943>Failed to push to Oraxen Studio: " + e.getMessage());
                Logs.debug(e);
            } finally {
                if (zipBundle != null) {
                    try {
                        Files.deleteIfExists(zipBundle.toPath());
                    } catch (Exception ignored) {
                    }
                }
                syncInProgress.set(false);
            }
        });
    }

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
            Path tempZip = null;
            try (HttpClient client = createHttpClient()) {
                tempZip = Files.createTempFile("oraxen-studio-pull-", ".zip");
                HttpRequest request = HttpRequest.newBuilder(URI.create(getStudioBaseUrl() + "/api/studio/pull"))
                        .timeout(Duration.ofMinutes(2))
                        .header("Authorization", "Bearer " + secret)
                        .GET()
                        .build();

                HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempZip));
                if (response.statusCode() != 200) {
                    sendMessage(sender, "<prefix><#fa4943>Studio pull failed (HTTP " + response.statusCode() + ").");
                    syncInProgress.set(false);
                    return;
                }

                int rev = parseRevisionHeader(response.headers().firstValue("X-Oraxen-Studio-Rev").orElse("0"));
                lastSyncedRevision = rev;

                Map<String, String> changedFiles = StudioBundleUtils.extractBundleToDataFolder(
                        tempZip, plugin.getDataFolder().toPath());
                scheduleReloadAfterPull(sender, changedFiles.size(), rev);
            } catch (Exception e) {
                syncInProgress.set(false);
                sendMessage(sender, "<prefix><#fa4943>Failed to pull from Oraxen Studio: " + e.getMessage());
                Logs.debug(e);
            } finally {
                deleteTempFileQuietly(tempZip);
            }
        });
    }

    private static int parseRevisionHeader(String revHeader) {
        try {
            return Integer.parseInt(revHeader);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static void deleteTempFileQuietly(@Nullable Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    private void scheduleReloadAfterPull(@Nullable CommandSender sender, int changedCount, int rev) {
        SchedulerUtil.runTask(plugin, () -> {
            try {
                plugin.reloadConfigs();
                ReloadCommand.reloadItems(Bukkit.getConsoleSender());
                ReloadCommand.reloadPack(Bukkit.getConsoleSender());
                for (Player player : Bukkit.getOnlinePlayers()) {
                    plugin.getFontManager().sendGlyphTabCompletion(player);
                }
                sendMessage(sender,
                        "<prefix><#55ffa4>Synced <#43fa8c>" + changedCount
                                + "<#55ffa4> changed file(s) from Oraxen Studio (revision #"
                                + rev + ") and refreshed resource pack.");
            } finally {
                syncInProgress.set(false);
            }
        });
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
