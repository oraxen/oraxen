package io.th0rgal.oraxen.pack.upload;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenPackPreUploadEvent;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.dispatch.MultiVersionPackSender;
import io.th0rgal.oraxen.pack.dispatch.PlayerVersionDetector;
import io.th0rgal.oraxen.pack.generation.PackVersion;
import io.th0rgal.oraxen.pack.generation.PackVersionManager;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages uploading and distributing multiple pack versions to players.
 */
public class MultiVersionUploadManager {

    private final OraxenPlugin plugin;
    private final Map<PackVersion, HostingProvider> hostingProviders = new HashMap<>();
    private MultiVersionPackSender packSender;

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
        // Initialize player version detection
        PlayerVersionDetector.initialize();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Upload all pack versions
                uploadAllVersions(versionManager);

                // Create pack sender
                packSender = new MultiVersionPackSender(versionManager, hostingProviders);
                packSender.register();

                // Send to online players if requested
                if (sendToPlayers && Settings.SEND_PACK.toBool()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            packSender.sendPack(player);
                        }
                    });
                }

                Logs.logSuccess("Multi-version pack upload and distribution complete");
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
        HostingProvider provider = createHostingProvider();

        // Upload pack (provider calculates SHA-1 internally)
        boolean success = provider.uploadPack(packVersion.getPackFile());
        if (!success) {
            throw new IOException("Failed to upload pack");
        }

        // Store URL and SHA1 from provider
        packVersion.setPackURL(provider.getPackURL());
        packVersion.setPackSHA1(provider.getSHA1());

        // Store provider
        hostingProviders.put(packVersion, provider);

        Logs.logSuccess("  Uploaded: " + packVersion.getMinecraftVersion() + " -> " + provider.getPackURL());
    }

    private HostingProvider createHostingProvider() {
        java.util.Locale locale = java.util.Locale.ROOT;
        HostingProvider provider = switch (Settings.UPLOAD_TYPE.toString().toLowerCase(locale)) {
            case "polymath" -> new io.th0rgal.oraxen.pack.upload.hosts.Polymath(Settings.POLYMATH_SERVER.toString());
            case "self-host" -> {
                org.bukkit.configuration.ConfigurationSection selfHostConfig = plugin.getConfigsManager().getSettings()
                        .getConfigurationSection("Pack.upload.self-host");
                yield new io.th0rgal.oraxen.pack.upload.hosts.SelfHost(selfHostConfig);
            }
            default -> null;
        };

        if (provider == null) {
            Logs.logError("Unknown Hosting-Provider type: " + Settings.UPLOAD_TYPE);
            Logs.logError("Polymath will be used instead.");
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

        // Note: SelfHost providers don't have a shutdown() method in the interface
        // They are automatically stopped when uploadPack is called again
        hostingProviders.clear();
    }
}
