package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PackDispatcher {

    private static String url;
    private static byte[] sha1;

    public static void setPackURL(String packURL) {
        url = packURL;
    }

    public static void setSha1(byte[] sha1) {
        PackDispatcher.sha1 = sha1;
    }

    public static void sendPack(Player player) {
        player.setResourcePack(
                // minecraft has known bugs with ssl protocol
                url.replace("https://", "http://"), sha1);
    }

    public static void sendWelcomeMessage(Player player, boolean delayed) {
        BaseComponent[] components = null; // Pack.JOIN_MESSAGE_CONTENT.toMiniMessage("pack_url", url)
        long delay = (int) Settings.JOIN_MESSAGE_DELAY.getValue();
        if (delay == -1 || !delayed)
            Message.COMMAND_JOIN_MESSAGE.send(player, "pack_url", url);
        else
            Bukkit
                    .getScheduler()
                    .runTaskLaterAsynchronously(OraxenPlugin.get(),
                            () -> Message.COMMAND_JOIN_MESSAGE.send(player, "pack_url", url),
                            delay * 20L);
    }

}
