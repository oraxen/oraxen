package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.settings.Pack;
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

    public static void sendWelcomeMessage(Player player) {
        BaseComponent[] components = Pack.JOIN_MESSAGE_CONTENT.toMiniMessage("pack_url", url);
        long delay = (int) Pack.JOIN_MESSAGE_DELAY.getValue();
        if (delay == -1)
            player.spigot().sendMessage(components);
        else
            Bukkit
                    .getScheduler()
                    .runTaskLaterAsynchronously(OraxenPlugin.get(), () -> player.spigot().sendMessage(components),
                            delay * 20L);
    }

}
