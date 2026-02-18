package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackPreUploadEvent;
import io.th0rgal.oraxen.api.events.OraxenPackUploadEvent;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.dispatch.MultiVersionPackSender;
import io.th0rgal.oraxen.pack.dispatch.PlayerVersionDetector;
import io.th0rgal.oraxen.pack.generation.PackVersion;
import io.th0rgal.oraxen.pack.generation.PackVersionManager;
import io.th0rgal.oraxen.pack.receive.PackReceiver;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.EventUtils;
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
                boolean contentChanged = uploadAllVersions(versionManager);
                if (cancelled) return;
                SchedulerUtil.runTask(() -> distributePacksToPlayers(reload, sendToPlayers, contentChanged));
            } catch (Exception e) {
                Logs.logError("Failed to upload and send multi-version packs: " + e.getMessage());
                if (Settings.DEBUG.toBool()) e.printStackTrace();
            }
        });
    }

    private void registerReceiverIfNeeded() {
        if (Settings.RECEIVE_ENABLED.toBool() && receiver == null) {
            receiver = new PackReceiver();
            Bukkit.getPluginManager().registerEvents(receiver, plugin);
        }
    }

    private void distributePacksToPlayers(boolean reload, boolean sendToPlayers, boolean contentChanged) {
        if (cancelled) return;

        if (packSender != null) {
            packSender.unregister();
        }
        packSender = new MultiVersionPackSender(versionManager);

        boolean shouldRegister = (Settings.SEND_PACK.toBool() || Settings.SEND_JOIN_MESSAGE.toBool())
                && !(reload && !Settings.SEND_ON_RELOAD.toBool());

        if (shouldRegister) {
            packSender.register();
            if (sendToPlayers && Settings.SEND_PACK.toBool() && contentChanged) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    packSender.sendPack(player);
                }
            }
        }

        Logs.logSuccess("Multi-version pack upload and distribution complete");
    }

    /**
     * Uploads all pack versions and checks if any content changed since the last upload.
     *
     * @return true if at least one pack version had a different SHA1 than the previous upload
     */
    private boolean uploadAllVersions(PackVersionManager versionManager) {
        Collection<PackVersion> versions = versionManager.getAllVersions();
        Logs.logInfo("Uploading " + versions.size() + " pack versions...");

        // Create hosting provider once and reuse for all versions (avoids repeated
        // reflection/initialization for external providers)
        HostingProvider provider = HostingProviderFactory.createHostingProvider(false);

        Map<String, String> currentSHA1s = new HashMap<>();
        for (PackVersion packVersion : versions) {
            try {
                uploadPackVersion(packVersion, provider);
                byte[] sha1Bytes = packVersion.getPackSHA1();
                if (sha1Bytes != null) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : sha1Bytes) sb.append(String.format("%02x", b));
                    currentSHA1s.put(packVersion.getMinecraftVersion(), sb.toString());
                }
            } catch (Exception e) {
                Logs.logError("Failed to upload pack for " + packVersion.getMinecraftVersion() + ": " + e.getMessage());
                if (Settings.DEBUG.toBool()) {
                    e.printStackTrace();
                }
            }
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

        return anyChanged;
    }

    private void uploadPackVersion(PackVersion packVersion, HostingProvider provider) throws IOException {
        Logs.logInfo("Uploading pack for Minecraft " + packVersion.getMinecraftVersion() + "...");

        // Fire pre-upload event
        OraxenPackPreUploadEvent event = new OraxenPackPreUploadEvent();
        EventUtils.callEvent(event);

        // Upload pack (provider calculates SHA-1 internally)
        boolean success = provider.uploadPack(packVersion.getPackFile());
        if (!success) {
            throw new IOException("Failed to upload pack");
        }

        // Capture provider state NOW, before the next upload overwrites it.
        String url = provider.getPackURL();
        byte[] sha1 = provider.getSHA1();
        UUID uuid = provider.getPackUUID();

        packVersion.setPackURL(url);
        packVersion.setPackSHA1(sha1);
        packVersion.setPackUUID(uuid);

        // Snapshot the provider so the event exposes this upload's data,
        // not whatever the shared provider holds when the main thread processes it.
        HostingProvider snapshot = new HostingProviderSnapshot(url, sha1, provider.getOriginalSHA1(), uuid);
        OraxenPackUploadEvent uploadEvent = new OraxenPackUploadEvent(snapshot);
        SchedulerUtil.runTask(() -> Bukkit.getPluginManager().callEvent(uploadEvent));

        Logs.logSuccess("  Uploaded: " + packVersion.getMinecraftVersion() + " -> " + url);
    }

    /** Immutable snapshot of a {@link HostingProvider}'s state at a point in time. */
    private static final class HostingProviderSnapshot implements HostingProvider {
        private final String packURL;
        private final byte[] sha1;
        private final String originalSHA1;
        private final UUID packUUID;

        HostingProviderSnapshot(String packURL, byte[] sha1, String originalSHA1, UUID packUUID) {
            this.packURL = packURL;
            this.sha1 = sha1;
            this.originalSHA1 = originalSHA1;
            this.packUUID = packUUID;
        }

        @Override public boolean uploadPack(File resourcePack) {
            throw new UnsupportedOperationException("Snapshot provider is read-only");
        }
        @Override public String getPackURL() { return packURL; }
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
        }

        if (receiver != null) {
            HandlerList.unregisterAll(receiver);
            receiver = null;
        }
    }
}
