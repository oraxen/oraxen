package io.th0rgal.oraxen.utils;

import org.bukkit.Bukkit;

public class PluginUtils {

    public static boolean isEnabled(String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }
}
