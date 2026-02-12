package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.generation.PackVersion;
import io.th0rgal.oraxen.pack.generation.PackVersionManager;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;

/**
 * Sends version-appropriate resource packs to players based on their client version.
 */
public class MultiVersionPackSender implements Listener {

    private final PackVersionManager versionManager;
    private final Map<PackVersion, HostingProvider> hostingProviders;
    private static final String prompt = Settings.SEND_PACK_PROMPT.toString();
    private static final boolean mandatory = Settings.SEND_PACK_MANDATORY.toBool();

    public MultiVersionPackSender(PackVersionManager versionManager, Map<PackVersion, HostingProvider> hostingProviders) {
        this.versionManager = versionManager;
        this.hostingProviders = hostingProviders;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    /**
     * Sends the appropriate pack version to a player based on their client version.
     *
     * @param player Player to send pack to
     */
    public void sendPack(Player player) {
        // Detect player's client version
        Integer protocolVersion = PlayerVersionDetector.getPlayerProtocolVersion(player);
        PackVersion packVersion;

        if (protocolVersion != null) {
            // Find best pack version for player's protocol
            packVersion = versionManager.findBestVersionForProtocol(protocolVersion);

            if (packVersion != null) {
                String clientVersion = PlayerVersionDetector.getPlayerVersionString(player);
                Logs.logInfo("Sending " + packVersion.getMinecraftVersion() + " pack to " + player.getName() + " (client: " + clientVersion + ")");
            } else {
                Logs.logWarning("No compatible pack found for " + player.getName() + " (protocol: " + protocolVersion + "), using server version");
                packVersion = versionManager.getServerPackVersion();
            }
        } else {
            // Cannot detect version, use server pack version
            packVersion = versionManager.getServerPackVersion();
            if (Settings.DEBUG.toBool()) {
                Logs.logInfo("Sending server pack to " + player.getName() + " (version detection unavailable)");
            }
        }

        // Send the pack
        sendPackVersion(player, packVersion);
    }

    private void sendPackVersion(Player player, PackVersion packVersion) {
        HostingProvider provider = hostingProviders.get(packVersion);
        if (provider == null) {
            Logs.logError("No hosting provider for pack version: " + packVersion);
            return;
        }

        String url = packVersion.getPackURL();
        byte[] sha1 = packVersion.getPackSHA1();

        if (url == null || sha1 == null) {
            Logs.logError("Pack version not uploaded: " + packVersion);
            return;
        }

        String layer = Settings.SEND_PACK_LAYER.toString();
        boolean useBungeeLayer = layer != null && !layer.isEmpty();

        // Pre-compute prompt conversions
        net.kyori.adventure.text.Component componentPrompt = AdventureUtils.MINI_MESSAGE.deserialize(prompt);
        String legacyPrompt = AdventureUtils.LEGACY_SERIALIZER.serialize(componentPrompt);

        if (VersionUtil.atOrAbove("1.20.3")) {
            if (useBungeeLayer) {
                // BungeeCord mode: Remove old Oraxen packs, then add without clearing proxy packs
                player.removeResourcePacks(packVersion.getPackUUID());
                player.addResourcePack(packVersion.getPackUUID(), url, sha1, legacyPrompt, mandatory);
            } else if (VersionUtil.isPaperServer()) {
                // Standalone Paper: setResourcePack clears all packs, supports Component prompt
                player.setResourcePack(packVersion.getPackUUID(), url, sha1, componentPrompt, mandatory);
            } else {
                // Standalone Spigot: setResourcePack clears all packs, requires String prompt
                player.setResourcePack(packVersion.getPackUUID(), url, sha1, legacyPrompt, mandatory);
            }
        } else {
            // Pre-1.20.3 versions
            if (VersionUtil.isPaperServer()) {
                player.setResourcePack(url, sha1, componentPrompt, mandatory);
            } else {
                player.setResourcePack(url, sha1, legacyPrompt, mandatory);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!Settings.SEND_PACK.toBool()) {
            return;
        }

        int delay = (int) Settings.SEND_PACK_DELAY.getValue();
        if (delay <= 0) {
            sendPack(player);
        } else {
            SchedulerUtil.runTaskLaterAsync(delay * 20L, () -> sendPack(player));
        }
    }
}
