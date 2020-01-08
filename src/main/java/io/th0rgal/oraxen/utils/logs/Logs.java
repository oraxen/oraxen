package io.th0rgal.oraxen.utils.logs;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.settings.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.logging.Level;

public class Logs {

    private static final CustomLogger LOGGER = new CustomLogger(OraxenPlugin.get());

    public static void enableFilter() throws NoSuchFieldException, IllegalAccessException {
        Field field = JavaPlugin.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(OraxenPlugin.get(), LOGGER);
    }

    public static void log(String message) {
        log(ChatColor.GRAY, message);
    }

    public static void log(ChatColor chatColor, String message) {
        logInfo(Plugin.PREFIX + chatColor.toString() + message);
    }

    public static void logInfo(String message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public static void logError(String message) {
        LOGGER.newLog(Level.SEVERE, message);
    }

    public static void logWarning(String message) {
        LOGGER.newLog(Level.WARNING, message);
    }


}
