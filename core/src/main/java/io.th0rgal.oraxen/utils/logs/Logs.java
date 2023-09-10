package io.th0rgal.oraxen.utils.logs;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public class Logs {

    private Logs() {}

    private static final CustomLogger LOGGER = new CustomLogger(OraxenPlugin.get());

    public static void enableFilter() throws NoSuchFieldException, IllegalAccessException {
        Field field = JavaPlugin.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(OraxenPlugin.get(), LOGGER);
    }

    public static void logInfo(String message) {
        Component info = AdventureUtils.MINI_MESSAGE_EMPTY.deserialize(Message.PREFIX + "<blue>" + message + "</blue>");
        OraxenPlugin.get().getAudience().console().sendMessage(info);
    }

    public static void logSuccess(String message) {
        Component success = AdventureUtils.MINI_MESSAGE_EMPTY.deserialize(Message.PREFIX + "<#55ffa4>" + message + "</#55ffa4>");
        OraxenPlugin.get().getAudience().console().sendMessage(success);
    }

    public static void logError(String message) {
        Component error = AdventureUtils.MINI_MESSAGE_EMPTY.deserialize(Message.PREFIX + "<#e73f34>" + message + "</#e73f34>");
        OraxenPlugin.get().getAudience().console().sendMessage(error);
    }

    public static void logWarning(String message) {
        Component warning = AdventureUtils.MINI_MESSAGE_EMPTY.deserialize(Message.PREFIX + "<#f9f178>" + message + "</#f9f178>");
        OraxenPlugin.get().getAudience().console().sendMessage(warning);
    }

    public static void newline() {
        OraxenPlugin.get().getAudience().console().sendMessage(Component.empty());
    }

    public static void debug(Object object) { if ( Settings.DEBUG.toBool()) Bukkit.broadcastMessage(String.valueOf(object)); }
    public static <T> T debugVal(T object) { if ( Settings.DEBUG.toBool()) Bukkit.broadcastMessage(String.valueOf(object)); return object; }
    public static void debug(Component component) { if ( Settings.DEBUG.toBool()) OraxenPlugin.get().getAudience().console().sendMessage(component); }

}
