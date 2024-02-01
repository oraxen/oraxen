package io.th0rgal.oraxen.mechanics.provided.gameplay.light;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.configuration.ConfigurationSection;

public class LightMechanic {
    private static final BlockFace[] BLOCK_FACES = new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.SELF};
    private final BlockData lightData;

    private final int lightLevel;

    public LightMechanic(ConfigurationSection section) {
        lightLevel = Math.min(15, section.getInt("light", -1));
        lightData = lightLevel == -1 ? null : (Light) Material.LIGHT.createBlockData();
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public boolean hasLightLevel() {
        return lightLevel != -1;
    }

    public void createBlockLight(Block block) {
        if (!hasLightLevel()) return;

        // If the block attempted placed at is empty, we only place here
        // This is mainly for furniture with no barrier hitbox
        if (block.getType().isAir()) block.setBlockData(lightData);
        else for (BlockFace face : BLOCK_FACES) {
            Block relative = block.getRelative(face);
            if (!relative.getType().isAir() && relative.getType() != Material.LIGHT) continue;
            if (relative.getBlockData() instanceof Light relativeLight && relativeLight.getLevel() > lightLevel) continue;

            relative.setBlockData(lightData);
        }
    }

    public void removeBlockLight(Block block) {
        if (!hasLightLevel()) return;
        for (BlockFace face : BLOCK_FACES) {
            Block relative = block.getRelative(face);
            if (relative.getType() != Material.LIGHT) continue;
            if (relative.getBlockData() instanceof Light relativeLight && relativeLight.getLevel() != lightLevel) continue;

            relative.setType(Material.AIR);
        }
    }

}
