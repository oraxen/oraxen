package io.th0rgal.oraxen.mechanics.provided.gameplay.togglelight;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ToggleLightMechanic extends Mechanic {

    public static final NamespacedKey TOGGLE_LIGHT_STATE_KEY = new NamespacedKey(OraxenPlugin.get(), "toggle_light_state");
    private static final BlockFace[] BLOCK_FACES = new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.SELF};

    private final int toggleLightLevel;
    private final int baseLightLevel;

    public ToggleLightMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        toggleLightLevel = Math.min(15, section.getInt("toggle_light", 0));
        baseLightLevel = Math.min(15, Math.max(0, section.getInt("light", 0)));
    }

    public int getToggleLightLevel() {
        return toggleLightLevel;
    }

    public int getBaseLightLevel() {
        return baseLightLevel;
    }

    public boolean hasToggleLight() {
        return toggleLightLevel > 0;
    }

    public boolean isToggledOn(Block block) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        return pdc.getOrDefault(TOGGLE_LIGHT_STATE_KEY, PersistentDataType.BOOLEAN, false);
    }

    public boolean isToggledOn(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.getOrDefault(TOGGLE_LIGHT_STATE_KEY, PersistentDataType.BOOLEAN, false);
    }

    public void setToggledOn(Block block, boolean toggled) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        pdc.set(TOGGLE_LIGHT_STATE_KEY, PersistentDataType.BOOLEAN, toggled);
    }

    public void setToggledOn(Entity entity, boolean toggled) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(TOGGLE_LIGHT_STATE_KEY, PersistentDataType.BOOLEAN, toggled);
    }

    public int getCurrentLightLevel(Block block) {
        if (!hasToggleLight()) return baseLightLevel;
        return isToggledOn(block) ? toggleLightLevel : baseLightLevel;
    }

    public int getCurrentLightLevel(Entity entity) {
        if (!hasToggleLight()) return baseLightLevel;
        return isToggledOn(entity) ? toggleLightLevel : baseLightLevel;
    }

    public void toggle(Block block) {
        boolean newState = !isToggledOn(block);
        setToggledOn(block, newState);
        updateLight(block, newState ? toggleLightLevel : baseLightLevel);
    }

    public void toggle(Entity entity) {
        boolean newState = !isToggledOn(entity);
        setToggledOn(entity, newState);
        Block block = entity.getLocation().getBlock();
        updateLight(block, newState ? toggleLightLevel : baseLightLevel);
    }

    public void updateLight(Block block, int lightLevel) {
        // First remove any existing light blocks around this block
        removeBlockLight(block);
        
        if (lightLevel <= 0) {
            return;
        }

        Light lightData = (Light) Material.LIGHT.createBlockData();
        lightData.setLevel(lightLevel);

        if (block.getType().isAir()) {
            block.setBlockData(lightData);
        } else {
            for (BlockFace face : BLOCK_FACES) {
                Block relative = block.getRelative(face);
                if (!relative.getType().isAir() && relative.getType() != Material.LIGHT) continue;
                if (relative.getBlockData() instanceof Light relativeLight && relativeLight.getLevel() > lightLevel) continue;
                relative.setBlockData(lightData);
            }
        }
    }

    private void removeBlockLight(Block block) {
        for (BlockFace face : BLOCK_FACES) {
            Block relative = block.getRelative(face);
            if (relative.getType() == Material.LIGHT) {
                relative.setType(Material.AIR);
            }
        }
    }

    public void toggleFurnitureBarriers(FurnitureMechanic furnitureMechanic, Entity baseEntity) {
        boolean newState = !isToggledOn(baseEntity);
        setToggledOn(baseEntity, newState);
        updateAllBarrierBlocks(furnitureMechanic, baseEntity);
    }

    public void updateAllBarrierBlocks(FurnitureMechanic furnitureMechanic, Entity baseEntity) {
        if (baseEntity == null) return;
        
        float yaw = FurnitureMechanic.getFurnitureYaw(baseEntity);
        Location center = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        List<Location> barrierLocations = furnitureMechanic.getLocations(yaw, center, furnitureMechanic.getBarriers(baseEntity));
        
        int lightLevel = isToggledOn(baseEntity) ? toggleLightLevel : baseLightLevel;
        for (Location barrierLocation : barrierLocations) {
            Block barrierBlock = barrierLocation.getBlock();
            if (barrierBlock.getType() == Material.BARRIER) {
                updateLight(barrierBlock, lightLevel);
            }
        }
    }
}

