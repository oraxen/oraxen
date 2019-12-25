package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.settings.Pack;
import net.md_5.bungee.api.chat.BaseComponent;
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
        player.setResourcePack(url, sha1);
    }

    public static void sendWelcomeMessage(Player player) {
        BaseComponent[] components = Pack.WELCOME_MESSAGE.toMiniMessage();
        player.spigot().sendMessage(components);
    }

}
