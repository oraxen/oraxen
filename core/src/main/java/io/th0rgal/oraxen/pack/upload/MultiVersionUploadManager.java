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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages uploading and distributing multiple pack versions to players.
 */
public class MultiVersionUploadManager {

    private final OraxenPlugin plugin;
    private final Map<PackVersion, HostingProvider> hostingProviders = new HashMap<>();
    private MultiVersionPackSender packSender;
    private PackReceiver receiver;

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
        // Check if upload is enabled
        if (!Settings.UPLOAD.toBool()) {
            Logs.logWarning("Pack upload is disabled in settings");
            return;
        }

        // Validate hosting provider compatibility
        // Note: SelfHost compatibility is checked earlier in ResourcePack.generate()
        // This is a safety check in case this method is called directly
        if (isSelfHost()) {
            Logs.logError("SelfHost is incompatible with multi-version packs!");
            Logs.logError("This should have been caught earlier - no packs will be uploaded.");
            return;
        }

        // Initialize player version detection
        PlayerVersionDetector.initialize();

        // Register PackReceiver for pack status events (accepted/denied/loaded etc.)
        if (Settings.RECEIVE_ENABLED.toBool() && receiver == null) {
            receiver = new PackReceiver();
            Bukkit.getPluginManager().registerEvents(receiver, plugin);
        }

        SchedulerUtil.runTaskAsync(() -> {
            try {
                // Upload all pack versions (async — network I/O)
                uploadAllVersions(versionManager);

                // Switch to main thread for Bukkit API calls (registerEvents is not thread-safe)
                SchedulerUtil.runTask(() -> {
                    // Unregister old listener to prevent duplicates
                    if (packSender != null) {
                        packSender.unregister();
                    }

                    // Create and register pack sender
                    packSender = new MultiVersionPackSender(versionManager);

                    if (reload && !Settings.SEND_ON_RELOAD.toBool()) {
                        packSender.unregister();
                    } else if (Settings.SEND_PACK.toBool() || Settings.SEND_JOIN_MESSAGE.toBool()) {
                        packSender.register();

                        // Send to online players if requested
                        if (sendToPlayers && Settings.SEND_PACK.toBool()) {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                packSender.sendPack(player);
                            }
                        }
                    } else {
                        packSender.unregister();
                    }

                    Logs.logSuccess("Multi-version pack upload and distribution complete");
                });
            } catch (Exception e) {
                Logs.logError("Failed to upload and send multi-version packs: " + e.getMessage());
                if (Settings.DEBUG.toBool()) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void uploadAllVersions(PackVersionManager versionManager) {
        Collection<PackVersion> versions = versionManager.getAllVersions();
        Logs.logInfo("Uploading " + versions.size() + " pack versions...");

        for (PackVersion packVersion : versions) {
            try {
                uploadPackVersion(packVersion);
            } catch (Exception e) {
                Logs.logError("Failed to upload pack for " + packVersion.getMinecraftVersion() + ": " + e.getMessage());
                if (Settings.DEBUG.toBool()) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void uploadPackVersion(PackVersion packVersion) throws IOException {
        Logs.logInfo("Uploading pack for Minecraft " + packVersion.getMinecraftVersion() + "...");

        // Fire pre-upload event
        OraxenPackPreUploadEvent event = new OraxenPackPreUploadEvent();
        EventUtils.callEvent(event);

        // Create hosting provider for this version
        // Note: SelfHost is not supported for multi-version (validated earlier)
        HostingProvider provider = createHostingProvider();

        // Upload pack (provider calculates SHA-1 internally)
        boolean success = provider.uploadPack(packVersion.getPackFile());
        if (!success) {
            throw new IOException("Failed to upload pack");
        }

        // Store URL, SHA1, and UUID from provider
        packVersion.setPackURL(provider.getPackURL());
        packVersion.setPackSHA1(provider.getSHA1());
        packVersion.setPackUUID(provider.getPackUUID()); // Use provider's content-based UUID

        // Store provider
        hostingProviders.put(packVersion, provider);

        // Fire upload event on main thread (matches UploadManager behavior)
        OraxenPackUploadEvent uploadEvent = new OraxenPackUploadEvent(provider);
        SchedulerUtil.runTask(() -> Bukkit.getPluginManager().callEvent(uploadEvent));

        Logs.logSuccess("  Uploaded: " + packVersion.getMinecraftVersion() + " -> " + provider.getPackURL());
    }

    private boolean isSelfHost() {
        String uploadType = Settings.UPLOAD_TYPE.toString().toLowerCase(Locale.ROOT);
        return "self-host".equals(uploadType);
    }

    private HostingProvider createHostingProvider() {
        HostingProvider provider = switch (Settings.UPLOAD_TYPE.toString().toLowerCase(Locale.ROOT)) {
            case "polymath" -> new io.th0rgal.oraxen.pack.upload.hosts.Polymath(Settings.POLYMATH_SERVER.toString());
            case "self-host" -> {
                Logs.logError("SelfHost cannot be used with multi-version packs");
                yield null;
            }
            default -> null;
        };

        if (provider == null) {
            String uploadType = Settings.UPLOAD_TYPE.toString();
            if (!"polymath".equalsIgnoreCase(uploadType)) {
                Logs.logError("Upload type '" + uploadType + "' is not supported for multi-version packs");
                Logs.logError("Polymath will be used instead.");
            }
            provider = new io.th0rgal.oraxen.pack.upload.hosts.Polymath(Settings.POLYMATH_SERVER.toString());
        }
        return provider;
    }

    public MultiVersionPackSender getPackSender() {
        return packSender;
    }

    public void unregister() {
        if (packSender != null) {
            packSender.unregister();
        }

        if (receiver != null) {
            HandlerList.unregisterAll(receiver);
            receiver = null;
        }

        // Clear hosting providers
        // Note: SelfHost is not supported in multi-version mode
        hostingProviders.clear();
    }
}
