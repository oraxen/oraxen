package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SHA1Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.entity.Player;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

public abstract class PackSender {

    protected final HostingProvider hostingProvider;
    private static final Object dispatchNormalizationLock = new Object();
    private static volatile boolean dispatchModeNormalized = false;

    protected PackSender(HostingProvider hostingProvider) {
        this.hostingProvider = hostingProvider;
    }

    public abstract void register();

    public abstract void unregister();

    public abstract void sendPack(Player player);

    /**
     * Sends a resource pack to a player using the appropriate Bukkit API for the server version.
     * Shared between BukkitPackSender and MultiVersionPackSender to avoid duplicating
     * the BungeeCord/Paper/Spigot/version-specific branching logic.
     *
     * @param player Player to send the pack to
     * @param uuid Pack UUID
     * @param url Pack download URL
     * @param sha1 Pack SHA-1 hash
     * @param prompt Prompt text (MiniMessage format)
     * @param mandatory Whether the pack is mandatory
     */
    static void sendResourcePack(Player player, UUID uuid, String url, byte[] sha1,
                                  String prompt, boolean mandatory) {
        String layer = Settings.SEND_PACK_LAYER.toString();
        boolean useBungeeLayer = layer != null && !layer.isEmpty();

        net.kyori.adventure.text.Component componentPrompt = AdventureUtils.MINI_MESSAGE.deserialize(prompt);
        String legacyPrompt = AdventureUtils.LEGACY_SERIALIZER.serialize(componentPrompt);

        if (VersionUtil.atOrAbove("1.20.3")) {
            if (useBungeeLayer) {
                // Do not remove the layer before adding it: proxies that suppress duplicate pack pushes
                // can otherwise let the remove through and leave the client without the pack.
                player.addResourcePack(uuid, url, sha1, legacyPrompt, mandatory);
            } else if (VersionUtil.isPaperServer()) {
                sendPaperResourcePack(player, uuid, url, sha1, componentPrompt, mandatory);
            } else {
                player.setResourcePack(uuid, url, sha1, legacyPrompt, mandatory);
            }
        } else {
            if (VersionUtil.isPaperServer()) {
                player.setResourcePack(url, sha1, componentPrompt, mandatory);
            } else {
                player.setResourcePack(url, sha1, legacyPrompt, mandatory);
            }
        }
    }

    static void sendResourcePack(Audience audience, UUID uuid, String url, byte[] sha1,
                                 String prompt, boolean mandatory) {
        net.kyori.adventure.text.Component componentPrompt = AdventureUtils.MINI_MESSAGE.deserialize(prompt);
        audience.sendResourcePacks(createResourcePackRequest(uuid, url, sha1, componentPrompt, mandatory));
    }

    private static void sendPaperResourcePack(Player player, UUID uuid, String url, byte[] sha1,
                                              net.kyori.adventure.text.Component prompt, boolean mandatory) {
        if (SHA1Utils.bytesToHex(sha1) == null) {
            player.setResourcePack(uuid, url, sha1, prompt, mandatory);
            return;
        }

        player.sendResourcePacks(createResourcePackRequest(uuid, url, sha1, prompt, mandatory));
    }

    private static ResourcePackRequest createResourcePackRequest(UUID uuid, String url, byte[] sha1,
                                                                 net.kyori.adventure.text.Component prompt,
                                                                 boolean mandatory) {
        String hash = SHA1Utils.bytesToHex(sha1);
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(uuid, URI.create(url), hash);

        return ResourcePackRequest.resourcePackRequest()
                .required(mandatory)
                .replace(false)
                .prompt(prompt)
                .packs(info)
                .build();
    }

    public static boolean isSendPreJoinConfigured() {
        normalizeDispatchModeForServerSupport();
        return isDispatchSendEnabled() && getDispatchMode() == DispatchMode.PRE_JOIN;
    }

    public static boolean isSendOnJoinConfigured() {
        normalizeDispatchModeForServerSupport();
        return isDispatchSendEnabled() && getDispatchMode() == DispatchMode.JOIN;
    }

    public static boolean isPreJoinDispatchActive() {
        return isSendPreJoinConfigured() && isPreJoinSupported();
    }

    public static boolean isAnyDispatchEnabled() {
        normalizeDispatchModeForServerSupport();
        return isDispatchSendEnabled();
    }

    private static boolean isDispatchSendEnabled() {
        Object send = Settings.SEND_PACK.getValue();
        return send instanceof Boolean bool && bool;
    }

    private static DispatchMode getDispatchMode() {
        Object value = Settings.SEND_PACK_MODE.getValue();
        if (!(value instanceof String mode)) return DispatchMode.JOIN;

        return switch (mode.trim().toUpperCase(Locale.ROOT).replace('_', '-')) {
            case "PRE-JOIN", "PREJOIN" -> DispatchMode.PRE_JOIN;
            case "JOIN" -> DispatchMode.JOIN;
            default -> {
                Logs.logWarning("Invalid Pack.dispatch.mode '" + mode + "'. Falling back to JOIN.");
                yield DispatchMode.JOIN;
            }
        };
    }

    private static boolean isPreJoinSupported() {
        return VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.21.7");
    }

    private static void normalizeDispatchModeForServerSupport() {
        if (dispatchModeNormalized) return;

        synchronized (dispatchNormalizationLock) {
            if (dispatchModeNormalized) return;

            if (isDispatchSendEnabled() && getDispatchMode() == DispatchMode.PRE_JOIN && !isPreJoinSupported()) {
                Logs.logInfo("Pack.dispatch.mode is set to PRE-JOIN, but pre-join dispatch is not available on this server. Falling back to JOIN.");
            }

            dispatchModeNormalized = true;
        }
    }

    /**
     * Resets the dispatch mode normalization flag so it is re-evaluated on next access.
     * Should be called on plugin reload to pick up changed settings.
     */
    public static void resetDispatchNormalization() {
        dispatchModeNormalized = false;
    }

    private enum DispatchMode {
        JOIN,
        PRE_JOIN
    }

}
