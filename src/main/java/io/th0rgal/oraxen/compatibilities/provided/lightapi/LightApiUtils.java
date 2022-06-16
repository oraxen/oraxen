package io.th0rgal.oraxen.compatibilities.provided.lightapi;

import org.bukkit.Location;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;


public class LightApiUtils {

    private LightApiUtils() {}

    protected static void createBlockLight(Location location, int value) {
        LightAPI.get().setLightLevel(location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(), value);
    }

    protected static void removeBlockLight(Location location) {
        LightAPI.get().setLightLevel(location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(), 0);
    }

}
