package io.th0rgal.oraxen.mechanics.provided.gameplay.light;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

public class LightMechanic {
    private static final BlockFace[] BLOCK_FACES = new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.SELF};
    private final Light lightData;

    private final int lightLevel;

    public LightMechanic(ConfigurationSection section) {
        lightLevel = section.contains("light") ? clampLightLevel(section.getInt("light")) : -1;
        lightData = lightLevel > 0 ? (Light) Material.LIGHT.createBlockData() : null;
        if (lightData != null) lightData.setLevel(lightLevel);
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public boolean hasLightLevel() {
        return lightLevel > 0 && lightData != null;
    }

    public void createBlockLight(Block block) {
        if (!hasLightLevel()) return;
        createBlockLight(block, lightLevel);
    }

    public static void createBlockLight(Block block, int lightLevel) {
        int clampedLightLevel = clampLightLevel(lightLevel);
        if (clampedLightLevel <= 0) return;

        Light lightData = (Light) Material.LIGHT.createBlockData();
        lightData.setLevel(clampedLightLevel);
        // If the block attempted placed at is empty, we only place here
        // This is mainly for furniture with no barrier hitbox
        if (block.getType().isAir()) block.setBlockData(lightData);
        else for (BlockFace face : BLOCK_FACES) {
            Block relative = block.getRelative(face);
            if (!relative.getType().isAir() && relative.getType() != Material.LIGHT) continue;
            if (relative.getBlockData() instanceof Light relativeLight && relativeLight.getLevel() > clampedLightLevel) continue;

            relative.setBlockData(lightData);
        }
    }

    public void removeBlockLight(Block block) {
        if (hasLightLevel()) removeBlockLightAt(block);
    }

    public static void removeBlockLightAt(Block block) {
        for (BlockFace face : BLOCK_FACES) {
            Block relative = block.getRelative(face);
            // If relative is Light we want to remove it
            if (relative.getType() == Material.LIGHT)
                relative.setType(Material.AIR);
            // But also update the surrounding blocks
            for (BlockFace relativeFace : BLOCK_FACES) {
                if (relativeFace == face.getOppositeFace()) continue;
                //refreshBlockLight(relative.getRelative(relativeFace));
                //refreshBlockLight(relative.getRelative(relativeFace));
            }
        }
    }

    public static void refreshBlockLight(Block block) {
        if (block.getType() == Material.LIGHT || block.getType().isAir()) return;

        Block relative = block;
        for (BlockFace face : BLOCK_FACES) {
            relative = relative.getRelative(face);
            if (refreshNoteBlockLight(relative)) continue;
            if (refreshStringBlockLight(relative)) continue;
            if (refreshChorusBlockLight(relative)) continue;
            if (refreshShapedBlockLight(relative)) continue;
            refreshFurnitureLight(relative);
        }
    }

    private static boolean refreshNoteBlockLight(Block block) {
        NoteBlockMechanic noteBlockMechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (noteBlockMechanic == null) return false;
        if (!noteBlockMechanic.hasLight()) return true;
        noteBlockMechanic.getLight().createBlockLight(block);
        return false;
    }

    private static boolean refreshStringBlockLight(Block block) {
        StringBlockMechanic stringBlockMechanic = OraxenBlocks.getStringMechanic(block);
        if (stringBlockMechanic == null) return false;
        if (!stringBlockMechanic.hasLight()) return true;
        stringBlockMechanic.getLight().createBlockLight(block);
        return false;
    }

    private static boolean refreshChorusBlockLight(Block block) {
        ChorusBlockMechanic chorusBlockMechanic = OraxenBlocks.getChorusMechanic(block);
        if (chorusBlockMechanic == null) return false;
        if (!chorusBlockMechanic.hasLight()) return true;
        chorusBlockMechanic.getLight().createBlockLight(block);
        return false;
    }

    private static boolean refreshShapedBlockLight(Block block) {
        ShapedBlockMechanic shapedBlockMechanic = OraxenBlocks.getShapedMechanic(block);
        if (shapedBlockMechanic == null) return false;
        if (!shapedBlockMechanic.hasLight()) return true;
        shapedBlockMechanic.getLight().createBlockLight(block);
        return false;
    }

    private static void refreshFurnitureLight(Block block) {
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (furnitureMechanic == null || !furnitureMechanic.hasLight()) return;

        Entity baseEntity = furnitureMechanic.getBaseEntity(block);
        if (baseEntity == null) return;
        furnitureMechanic.setEntityData(baseEntity, FurnitureMechanic.getFurnitureYaw(baseEntity), baseEntity.getFacing());
    }

    private static int clampLightLevel(int lightLevel) {
        return Math.min(15, Math.max(0, lightLevel));
    }

}
