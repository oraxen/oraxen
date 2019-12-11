package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.settings.Pack;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.entity.Player;

public class PackDispatcher {

    private static String url;

    public static void setPackURL(String packURL) {
        url = packURL;
    }

    public static void sendPack(Player player) {
        player.setResourcePack(url);
    }

    public static void sendMenu(Player player) {
        player.spigot().sendMessage(ComponentSerializer.parse(Pack.MENU_JSON.toString()));
    }

}
