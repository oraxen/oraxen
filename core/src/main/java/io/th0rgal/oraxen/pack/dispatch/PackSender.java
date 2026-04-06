package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.receive.PackReceiver;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.OraxenPlugin;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    protected void sendWelcomeMessage(Player player, boolean delayed) {
        sendWelcomeMessage(player, delayed, hostingProvider.getPackURL());
    }

    /**
     * Sends a welcome/join message to a player with the given pack URL.
     * This static utility method is shared between BukkitPackSender and MultiVersionPackSender
     * to avoid duplicating the welcome message logic.
     *
     * @param player Player to send the message to
     * @param delayed Whether to apply the configured join message delay
     * @param packUrl The pack URL to use in the message's pack_url placeholder
     */
    static void sendWelcomeMessage(Player player, boolean delayed, String packUrl) {
        long delay = (int) Settings.JOIN_MESSAGE_DELAY.getValue();
        if (delay == -1 || !delayed)
            Message.COMMAND_JOIN_MESSAGE.send(player,
                    AdventureUtils.tagResolver("pack_url", packUrl),
                    AdventureUtils.tagResolver("player", player.getName()));
        else
            SchedulerUtil.runTaskLaterAsync(delay * 20L,
                    () -> Message.COMMAND_JOIN_MESSAGE.send(player,
                            AdventureUtils.tagResolver("pack_url", packUrl),
                            AdventureUtils.tagResolver("player", player.getName())));
    }

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
                player.removeResourcePacks(uuid);
                player.addResourcePack(uuid, url, sha1, legacyPrompt, mandatory);
            } else if (VersionUtil.isPaperServer()) {
                player.setResourcePack(uuid, url, sha1, componentPrompt, mandatory);
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

    public static boolean isSendPreJoinConfigured() {
        normalizeDispatchModeForServerSupport();
        return readSendPreJoinConfigured();
    }

    public static boolean isSendOnJoinConfigured() {
        normalizeDispatchModeForServerSupport();
        return readSendOnJoinConfigured();
    }

    public static boolean isPreJoinDispatchActive() {
        return isSendPreJoinConfigured() && VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.21.7");
    }

    public static boolean isAnyDispatchEnabled() {
        return isSendOnJoinConfigured() || isSendPreJoinConfigured();
    }

    @Nullable
    public static CompletableFuture<Void> sendResourcePack(PlayerConfigurationConnection connection, boolean reconfigure) {
        String packUrl = OraxenPlugin.get().getPackURL();
        String hash = OraxenPlugin.get().getPackSHA1();
        if (packUrl == null || hash == null) return null;
        UUID playerId = connection.getProfile().getId();

        byte[] hashBytes = hashArray(hash);
        UUID packUUID = UUID.nameUUIDFromBytes(hashBytes);
        CompletableFuture<Void> future = reconfigure ? null : new CompletableFuture<>();

        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo()
                .id(packUUID)
                .uri(java.net.URI.create(packUrl))
                .hash(hash)
                .build();

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .required(Settings.SEND_PACK_MANDATORY.toBool())
                .replace(true)
                .prompt(AdventureUtils.MINI_MESSAGE.deserialize(Settings.SEND_PACK_PROMPT.toString()))
                .packs(info)
                .callback((requestId, status, audience) -> {
                    PackReceiver.handleAdventureStatus(playerId, status);
                    if (!status.intermediate()) {
                        if (future != null) future.complete(null);
                        else connection.completeReconfiguration();
                    }
                })
                .build();

        connection.getAudience().sendResourcePacks(request);
        return future;
    }

    private static byte[] hashArray(String hash) {
        int length = hash.length();
        if (length % 2 != 0) throw new IllegalArgumentException("Hash length must be even");

        byte[] result = new byte[length / 2];
        for (int i = 0; i < result.length; i++) {
            int from = i * 2;
            result[i] = (byte) Integer.parseInt(hash.substring(from, from + 2), 16);
        }
        return result;
    }

    private static boolean getBooleanSetting(String path, @Nullable String legacyPath, boolean fallback) {
        YamlConfiguration settings = OraxenPlugin.get().getConfigsManager().getSettings();
        Object value = settings.get(path);
        if (value instanceof Boolean bool) return bool;

        if (legacyPath != null) {
            Object legacyValue = settings.get(legacyPath);
            if (legacyValue instanceof Boolean bool) return bool;
        }

        return fallback;
    }

    private static boolean readSendPreJoinConfigured() {
        return getBooleanSetting("Pack.dispatch.send_pre_join", null, true);
    }

    private static boolean readSendOnJoinConfigured() {
        return getBooleanSetting("Pack.dispatch.send_on_join", "Pack.dispatch.send_pack", false);
    }

    private static void normalizeDispatchModeForServerSupport() {
        if (dispatchModeNormalized) return;

        synchronized (dispatchNormalizationLock) {
            if (dispatchModeNormalized) return;

            boolean sendPreJoin = readSendPreJoinConfigured();
            boolean sendOnJoin = readSendOnJoinConfigured();
            boolean preJoinSupported = VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.21.7");

            if (sendPreJoin && !preJoinSupported && !sendOnJoin) {
                Settings.SEND_ON_JOIN.setValue(true);
                if (Settings.DEBUG.toBool()) {
                    Logs.logInfo("Enabled Pack.dispatch.send_on_join because Pack.dispatch.send_pre_join is unsupported on this server.");
                }
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

}
