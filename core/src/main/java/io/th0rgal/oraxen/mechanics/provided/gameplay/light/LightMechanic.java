package io.th0rgal.oraxen.mechanics.provided.gameplay.light;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class LightMechanic {
    private static final LightMechanic EMPTY = new LightMechanic(List.of());
    private final List<LightBlock> lightBlocks;

    public LightMechanic(ConfigurationSection section) {
        List<LightBlock> lightBlocks = new ArrayList<>();
        for (String lightString : section.getStringList("lights"))
            lightBlocks.add(new LightBlock(lightString));


        this.lightBlocks = lightBlocks;
    }

    public LightMechanic(List<LightBlock> lightBlocks) {
        this.lightBlocks = lightBlocks;
    }

    public boolean isEmpty() {
        return lightBlocks.stream().allMatch(l -> l.lightLevel() == 0);
    }

    public List<LightBlock> lightBlocks() {
        return lightBlocks;
    }

    public List<Location> lightBlockLocations(Location center, float rotation) {
        return lightBlocks.stream().map(b -> b.groundRotate(rotation).add(center)).toList();
    }

}
