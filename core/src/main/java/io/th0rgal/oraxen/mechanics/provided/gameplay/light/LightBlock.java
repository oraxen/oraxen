package io.th0rgal.oraxen.mechanics.provided.gameplay.light;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import io.th0rgal.oraxen.utils.ParseUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.Light;

public class LightBlock extends BlockLocation {

    private final int lightLevel;
    private final Light lightData;

    public LightBlock from(Object hitboxObject) {
        if (hitboxObject instanceof String string) {
            return new LightBlock(string);
        } else return new LightBlock("0,0,0 15");
    }

    public LightBlock(String hitboxString) {
        super(hitboxString);
        this.lightLevel = ParseUtils.parseInt(StringUtils.substringAfter(hitboxString, " "), 15);
        this.lightData = (Light) Material.LIGHT.createBlockData(data -> ((Light) data).setLevel(lightLevel));
    }

    public LightBlock(Location location, Light lightData) {
        super(location);
        this.lightLevel = lightData.getLevel();
        this.lightData = lightData;
    }

    public int lightLevel() {
        return lightLevel;
    }

    public Light lightData() {
        return lightData;
    }

}
