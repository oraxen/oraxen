package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.config.Settings;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class InventoryUtils {

    private static final Map<String, Method> methodCache = new HashMap<>();

    public static Component titleFromView(InventoryEvent event) {
        Object view = event.getView();
        try {
            return (Component) methodCache.computeIfAbsent("title", (title) -> {
                try {
                    return view.getClass().getMethod(title);
                } catch (NoSuchMethodException e) {
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                    return null;
                }
            }).invoke(view);
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return Component.empty();
        }
    }

    public static Player playerFromView(InventoryEvent event) {
        Object view = event.getView();
        try {
            return (Player) methodCache.computeIfAbsent("getPlayer", (player) -> {
                try {
                    return view.getClass().getMethod(player);
                } catch (NoSuchMethodException e) {
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                    return null;
                }
            }).invoke(view);
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return null;
        }
    }

    public static String getTitleFromView(InventoryEvent event) {
        Object view = event.getView();
        try {
            return (String) methodCache.computeIfAbsent("getTitle", (title) -> {
                try {
                    return view.getClass().getMethod(title);
                } catch (NoSuchMethodException e) {
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                    return null;
                }
            }).invoke(view);
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return "";
        }
    }

    public static Inventory topInventoryForPlayer(Player player) {
        Object view = player.getOpenInventory();
        try {
            return (Inventory) methodCache.computeIfAbsent("getTopInventory", (topInv) -> {
                try {
                    return view.getClass().getMethod(topInv);
                } catch (NoSuchMethodException e) {
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                    return null;
                }
            }).invoke(view);
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return player.getInventory();
        }
    }
}
