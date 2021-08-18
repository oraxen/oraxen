package io.th0rgal.oraxen.compatibilities.provided.lightapi;

import org.bukkit.Location;
import ru.beykerykt.lightapi.LightAPI;
import ru.beykerykt.lightapi.LightType;
import ru.beykerykt.lightapi.chunks.ChunkInfo;

public class LightApiUtils {

    protected static void createBlockLight(Location location, int value, boolean async) {
        LightAPI.createLight(location, LightType.BLOCK, value, async);
    }

    protected static void removeBlockLight(Location location, boolean async) {
        LightAPI.deleteLight(location, LightType.BLOCK, async);
    }

    public static void refreshBlockLights(int light, Location... locations) {
        for (Location location : locations)
            for (ChunkInfo info : LightAPI.collectChunks(location, LightType.BLOCK, light))
                LightAPI.updateChunk(info, LightType.BLOCK);
    }

}
