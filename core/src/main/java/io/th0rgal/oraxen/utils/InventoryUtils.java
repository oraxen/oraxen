package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.config.Settings;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class InventoryUtils {

    private static final Map<String, Method> methodCache = new HashMap<>();

    public static Component titleFromView(InventoryEvent event) {
        if (VersionUtil.atOrAbove("1.21")) return event.getView().title();
        Object view = event.getView();
        try {
            return (Component) methodCache.computeIfAbsent("title", (title) -> {
                try {
                    Method method = view.getClass().getMethod("title");
                    method.setAccessible(true);
                    return method;
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
        if (VersionUtil.atOrAbove("1.21")) return (Player) event.getView().getPlayer();
        Object view = event.getView();
        try {
            return (Player) methodCache.computeIfAbsent("getPlayer", (player) -> {
                try {
                    Method method = view.getClass().getMethod("getPlayer");
                    method.setAccessible(true);
                    return method;
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
        if (VersionUtil.atOrAbove("1.21")) return event.getView().getTitle();
        @NotNull Object view = event.getView();
        try {
            return (String) methodCache.computeIfAbsent("getTitle", (title) -> {
                try {
                    Method method = view.getClass().getMethod("getTitle");
                    method.setAccessible(true);
                    return method;
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
        if (VersionUtil.atOrAbove("1.21")) return player.getOpenInventory().getTopInventory();
        Object view = player.getOpenInventory();
        try {
            return (Inventory) methodCache.computeIfAbsent("getTopInventory", (topInv) -> {
                try {
                    Method method = view.getClass().getMethod("getTopInventory");
                    method.setAccessible(true);
                    return method;
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
