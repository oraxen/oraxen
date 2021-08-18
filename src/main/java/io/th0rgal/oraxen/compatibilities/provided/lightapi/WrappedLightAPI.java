package io.th0rgal.oraxen.compatibilities.provided.lightapi;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class WrappedLightAPI {

    private static boolean loaded;

    public static void init() {
        loaded = Bukkit.getPluginManager().isPluginEnabled("LightAPI");
    }

    public static void createBlockLight(Location location, int value, boolean async) {
        if (loaded) LightApiUtils.createBlockLight(location, value, async);
    }

    public static void removeBlockLight(Location location, boolean async) {
        if (loaded) LightApiUtils.removeBlockLight(location, async);
    }

    public static void refreshBlockLights(int light, Location... locations) {
        if (loaded) LightApiUtils.refreshBlockLights(light, locations);
    }
}
