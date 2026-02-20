package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class PackSender {

    protected final HostingProvider hostingProvider;


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

}
