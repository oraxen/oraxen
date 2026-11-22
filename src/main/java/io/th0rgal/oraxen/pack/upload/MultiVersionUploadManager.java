package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackPreUploadEvent;
import io.th0rgal.oraxen.api.events.OraxenPackUploadEvent;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.pack.dispatch.MultiVersionPackSender;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.dispatch.PlayerVersionDetector;
import io.th0rgal.oraxen.pack.generation.PackVersion;
import io.th0rgal.oraxen.pack.generation.PackVersionManager;
import io.th0rgal.oraxen.pack.receive.PackReceiver;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Manages uploading and distributing multiple pack versions to players.
 */
public class MultiVersionUploadManager {

    private final OraxenPlugin plugin;
    private MultiVersionPackSender packSender;
    private PackReceiver receiver;
    private volatile boolean cancelled = false;
    private PackVersionManager versionManager;

    // Tracks SHA1 hashes from the previous upload to detect content changes.
    // Only sends packs to online players when at least one pack version changed,
    // matching the change-tracking behavior of UploadManager for single-pack mode.
    private static final Object trackingLock = new Object();
    private static Map<String, String> previousSHA1s = new HashMap<>();

    public MultiVersionUploadManager(OraxenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Uploads all pack versions and sends appropriate packs to online players.
     *
     * @param versionManager Pack version manager with all pack versions
     * @param reload Whether this is a reload operation
     * @param sendToPlayers Whether to send packs to online players
     */
    public void uploadAndSendToPlayers(PackVersionManager versionManager, boolean reload, boolean sendToPlayers) {
        this.versionManager = versionManager;
        cancelled = false;

        if (!Settings.UPLOAD.toBool()) {
            Logs.logWarning("Pack upload is disabled in settings");
            return;
        }

        if (isSelfHost()) {
            Logs.logError("SelfHost is incompatible with multi-version packs!");
            return;
        }

        PlayerVersionDetector.initialize();
        registerReceiverIfNeeded();

        SchedulerUtil.runTaskAsync(() -> {
            try {
                if (cancelled) return;
                UploadSummary summary = uploadAllVersions(versionManager);
                if (cancelled) return;
                if (summary.allCancelled()) {
                    Logs.logWarning("Multi-version pack upload was cancelled by a plugin");
                    return;
                }
                boolean contentChanged = summary.contentChanged();
                // Capture the parameter in a local so the lambda cannot observe a
                // later overwrite of the `this.versionManager` instance field.
                final PackVersionManager vm = versionManager;
                SchedulerUtil.runTask(() -> distributePacksToPlayers(vm, sendToPlayers, contentChanged));
            } catch (Exception e) {
                Logs.logError("Failed to upload and send multi-version packs: " + e.getMessage());
                Logs.debug(e);
            }
        });
    }

    private void registerReceiverIfNeeded() {
        if (Settings.RECEIVE_ENABLED.toBool() && receiver == null) {
            receiver = new PackReceiver();
            Bukkit.getPluginManager().registerEvents(receiver, plugin);
        }
    }

    private void distributePacksToPlayers(PackVersionManager vm, boolean sendToPlayers, boolean contentChanged) {
        if (cancelled) return;

        if (packSender != null) {
            packSender.unregister();
        }
        packSender = new MultiVersionPackSender(vm);

        boolean shouldRegister = PackSender.isAnyDispatchEnabled();

        if (shouldRegister) {
            packSender.register();
            if (sendToPlayers && PackSender.isAnyDispatchEnabled() && contentChanged) {
                MultiVersionPackSender sender = packSender;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    SchedulerUtil.runForEntity(player, () -> {
                        if (!cancelled) sender.sendPack(player);
                    });
                }
            }
        }

        Logs.logSuccess("Multi-version pack upload and distribution complete");
    }

    /**
     * Uploads all pack versions and checks if any content changed since the last upload.
     * Versions whose pre-upload event is cancelled are skipped and keep no upload data.
     */
    private UploadSummary uploadAllVersions(PackVersionManager versionManager) {
        Collection<PackVersion> versions = versionManager.getAllVersions();
        Logs.logInfo("Uploading " + versions.size() + " pack versions...");

        Map<String, String> currentSHA1s = new HashMap<>();
        HostingProvider firstProvider = HostingProviderFactory.createHostingProvider(false);
        boolean createProviderPerVersion = firstProvider.requiresNewInstancePerUpload();
        int versionIndex = 0;
        int cancelledVersions = 0;
        for (PackVersion packVersion : versions) {
            if (cancelled) return UploadSummary.ABORTED;
            try {
                HostingProvider provider = createProviderPerVersion && versionIndex++ > 0
                        ? HostingProviderFactory.createHostingProvider(false)
                        : firstProvider;
                VersionUploadResult result = uploadPackVersion(packVersion, provider);
                if (cancelled) return UploadSummary.ABORTED;
                if (result == VersionUploadResult.CANCELLED) {
                    cancelledVersions++;
                    continue;
                }
                String sha1Hex = packVersion.getPackSHA1Hex();
                if (sha1Hex != null) {
                    currentSHA1s.put(packVersion.getMinecraftVersion(), sha1Hex);
                }
            } catch (Exception e) {
                Logs.logError("Failed to upload pack for " + packVersion.getMinecraftVersion() + ": " + e.getMessage());
                if (Settings.DEBUG.toBool()) {
                    Logs.debug(e);
                }
            }
        }

        if (!versions.isEmpty() && cancelledVersions == versions.size()) {
            return new UploadSummary(true, false);
        }

        // Compare current SHA1s against previous to detect changes
        boolean anyChanged;
        synchronized (trackingLock) {
            anyChanged = !Objects.equals(currentSHA1s, previousSHA1s);
            previousSHA1s = currentSHA1s;
        }

        if (Settings.DEBUG.toBool()) {
            Logs.logInfo("Multi-version pack content " + (anyChanged ? "changed" : "unchanged"));
        }

        return new UploadSummary(false, anyChanged);
    }

    private VersionUploadResult uploadPackVersion(PackVersion packVersion, HostingProvider provider) throws IOException {
        if (cancelled) return VersionUploadResult.ABORTED;

        if (!new OraxenPackPreUploadEvent().callEvent()) {
            packVersion.setPackURL(null);
            packVersion.setPackSHA1(null);
            packVersion.setPackUUID(null);
            Logs.logWarning("Skipped pack upload for Minecraft " + packVersion.getMinecraftVersion() + ", cancelled by a plugin");
            return VersionUploadResult.CANCELLED;
        }
        if (cancelled) return VersionUploadResult.ABORTED;
        Logs.logInfo("Uploading pack for Minecraft " + packVersion.getMinecraftVersion() + "...");

        // Upload pack (provider calculates SHA-1 internally)
        boolean success = provider.uploadPack(packVersion.getPackFile(), packVersion.getMinecraftVersion());
        if (!success) {
            throw new IOException("Failed to upload pack");
        }
        if (cancelled) return VersionUploadResult.ABORTED;

        // Capture provider state NOW, before the next upload overwrites it.
        Supplier<String> urlSource = provider.getPackURLSource();
        byte[] sha1 = provider.getSHA1();
        UUID uuid = provider.getPackUUID();

        packVersion.setPackURLSource(urlSource);
        packVersion.setPackSHA1(sha1);
        packVersion.setPackUUID(uuid);

        // Snapshot the provider so the event exposes this upload's data,
        // not whatever the shared provider holds when the main thread processes it.
        HostingProvider snapshot = new HostingProviderSnapshot(urlSource, sha1, provider.getOriginalSHA1(), uuid);
        OraxenPackUploadEvent uploadEvent = new OraxenPackUploadEvent(snapshot);
        SchedulerUtil.runTask(uploadEvent::callEvent);

        Logs.logSuccess("  Uploaded: " + packVersion.getMinecraftVersion() + " -> " + urlSource.get());
        return VersionUploadResult.UPLOADED;
    }

    private enum VersionUploadResult { UPLOADED, CANCELLED, ABORTED }

    private record UploadSummary(boolean allCancelled, boolean contentChanged) {
        static final UploadSummary ABORTED = new UploadSummary(false, false);
    }

    /** Immutable snapshot of a {@link HostingProvider}'s state at a point in time. */
    private static final class HostingProviderSnapshot implements HostingProvider {
        private final Supplier<String> packURL;
        private final byte[] sha1;
        private final String originalSHA1;
        private final UUID packUUID;

        HostingProviderSnapshot(Supplier<String> packURL, byte[] sha1, String originalSHA1, UUID packUUID) {
            this.packURL = packURL;
            this.sha1 = sha1;
            this.originalSHA1 = originalSHA1;
            this.packUUID = packUUID;
        }

        @Override public boolean uploadPack(File resourcePack) {
            throw new UnsupportedOperationException("Snapshot provider is read-only");
        }
        @Override public String getPackURL() { return packURL.get(); }
        @Override public Supplier<String> getPackURLSource() { return packURL; }
        @Override public byte[] getSHA1() { return sha1; }
        @Override public String getOriginalSHA1() { return originalSHA1; }
        @Override public UUID getPackUUID() { return packUUID; }
    }

    private boolean isSelfHost() {
        String uploadType = Settings.UPLOAD_TYPE.toString().toLowerCase(Locale.ROOT);
        return "self-host".equals(uploadType);
    }

    public MultiVersionPackSender getPackSender() {
        return packSender;
    }

    public PackVersionManager getVersionManager() {
        return versionManager;
    }

    public void unregister() {
        cancelled = true;
        if (packSender != null) {
            packSender.unregister();
            packSender = null;
        }

        versionManager = null;

        if (receiver != null) {
            HandlerList.unregisterAll(receiver);
            receiver = null;
        }
    }
}
