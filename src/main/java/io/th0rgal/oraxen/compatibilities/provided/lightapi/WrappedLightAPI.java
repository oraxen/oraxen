package io.th0rgal.oraxen.compatibilities.provided.lightapi;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class WrappedLightAPI {

    private WrappedLightAPI() {}

    private static boolean loaded;

    public static void init() {
        loaded = Bukkit.getPluginManager().isPluginEnabled("LightAPI");
    }

    public static void createBlockLight(Location location, int value) {
        if (loaded)
            LightApiUtils.createBlockLight(location, value);
    }

    public static void removeBlockLight(Location location) {
        if (loaded) LightApiUtils.removeBlockLight(location);
    }

}
