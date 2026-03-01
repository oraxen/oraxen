package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.generation.PackVersion;
import io.th0rgal.oraxen.pack.generation.PackVersionManager;
import io.th0rgal.oraxen.pack.generation.ProtocolVersion;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Comparator;
import java.util.UUID;

/**
 * Sends version-appropriate resource packs to players based on their client version.
 */
public class MultiVersionPackSender implements Listener {

    private final PackVersionManager versionManager;
    private final String prompt = Settings.SEND_PACK_PROMPT.toString();
    private final boolean mandatory = Settings.SEND_PACK_MANDATORY.toBool();

    public MultiVersionPackSender(PackVersionManager versionManager) {
        this.versionManager = versionManager;
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
                if (Settings.DEBUG.toBool()) {
                    String clientVersion = PlayerVersionDetector.getPlayerVersionString(player);
                    Logs.logInfo("Sending " + packVersion.getMinecraftVersion() + " pack to " + player.getName() + " (client: " + clientVersion + ")");
                }
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
        sendPackVersion(player, packVersion, protocolVersion);
    }

    private void sendPackVersion(Player player, PackVersion packVersion, Integer protocolVersion) {
        PackVersion resolvedVersion = resolveSendableVersion(packVersion, protocolVersion);
        if (resolvedVersion == null) {
            Logs.logError("Cannot send pack: no pack version available for player: " + player.getName());
            return;
        }
        String url = resolvedVersion.getPackURL();
        byte[] sha1 = resolvedVersion.getPackSHA1();
        UUID uuid = resolvedVersion.getPackUUID();

        if (url == null || sha1 == null || uuid == null) {
            Logs.logError("Pack version not fully uploaded (missing url/sha1/uuid): " + resolvedVersion);
            return;
        }

        PackSender.sendResourcePack(player, uuid, url, sha1, prompt, mandatory);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerConnect(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Send welcome message if enabled
        if (Settings.SEND_JOIN_MESSAGE.toBool()) {
            sendWelcomeMessage(player, true);
        }

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

    private void sendWelcomeMessage(Player player, boolean delayed) {
        // Resolve the player's pack URL for the welcome message placeholder
        String packUrl = resolvePackUrlForPlayer(player);
        PackSender.sendWelcomeMessage(player, delayed, packUrl);
    }

    public String resolvePackUrlForPlayer(Player player) {
        Integer protocolVersion = PlayerVersionDetector.getPlayerProtocolVersion(player);
        PackVersion packVersion = (protocolVersion != null)
                ? versionManager.findBestVersionForProtocol(protocolVersion)
                : null;
        packVersion = resolveSendableVersion(packVersion, protocolVersion);
        if (packVersion == null) {
            Logs.logWarning("No pack version available for player: " + player.getName());
            return "";
        }
        String url = packVersion.getPackURL();
        return url != null ? url : "";
    }

    private PackVersion resolveSendableVersion(PackVersion preferred, Integer protocolVersion) {
        int packFormat = protocolVersion != null ? ProtocolVersion.getPackFormatForProtocol(protocolVersion) : -1;

        if (hasUploadData(preferred)) {
            return preferred;
        }

        PackVersion serverVersion = versionManager.getServerPackVersion();
        if (hasUploadData(serverVersion) && (protocolVersion == null || serverVersion.supportsFormat(packFormat))) {
            return serverVersion;
        }

        if (protocolVersion != null) {
            return versionManager.getAllVersions().stream()
                    .filter(this::hasUploadData)
                    .filter(candidate -> candidate.supportsFormat(packFormat))
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }

        return versionManager.getAllVersions().stream()
                .filter(this::hasUploadData)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private boolean hasUploadData(PackVersion packVersion) {
        return packVersion != null
                && packVersion.getPackURL() != null
                && packVersion.getPackSHA1() != null
                && packVersion.getPackUUID() != null;
    }
}
