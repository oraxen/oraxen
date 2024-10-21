package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.config.Settings;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.lang.reflect.Method;

public class InventoryUtils {

    private static Method getTitleMethod;
    private static Method titleMethod;
    private static Method topInventoryMethod;
    private static Method playerFromViewMethod;

    static {
        try {
            getTitleMethod = InventoryView.class.getDeclaredMethod("getTitle");
        } catch (Exception e) {

        }
        try {
            titleMethod = InventoryView.class.getDeclaredMethod("title");
        } catch (Exception e) {

        }
        try {
            topInventoryMethod = InventoryView.class.getDeclaredMethod("getTopInventory");
        } catch (Exception e) {

        }
        try {
            playerFromViewMethod = InventoryView.class.getDeclaredMethod("getPlayer");
        } catch (Exception e) {

        }
    }

    public static Component titleFromView(InventoryEvent event) {
        if (VersionUtil.atOrAbove("1.21")) return event.getView().title();
        try {
            return (Component) titleMethod.invoke(event.getView());
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return Component.empty();
        }
    }

    public static Player playerFromView(InventoryEvent event) {
        if (VersionUtil.atOrAbove("1.21")) return (Player) event.getView().getPlayer();
        try {
            return (Player) playerFromViewMethod.invoke(event.getView());
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return null;
        }
    }

    public static String getTitleFromView(InventoryEvent event) {
        if (VersionUtil.atOrAbove("1.21")) return event.getView().getTitle();
        try {
            return (String) getTitleMethod.invoke(event.getView());
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return "";
        }
    }

    public static Inventory topInventoryForPlayer(Player player) {
        if (VersionUtil.atOrAbove("1.21")) return player.getOpenInventory().getTopInventory();
        try {
            return (Inventory) topInventoryMethod.invoke(player.getOpenInventory());
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return player.getInventory();
        }
    }
}
