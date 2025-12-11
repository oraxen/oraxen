package io.th0rgal.oraxen.mechanics.provided.gameplay.togglelight;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
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

import java.util.*;

public class ToggleLightMechanic extends Mechanic {

    public static final NamespacedKey TOGGLE_LIGHT_STATE_KEY = new NamespacedKey(OraxenPlugin.get(), "toggle_light_state");
    private static final BlockFace[] BLOCK_FACES = new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.SELF};

    private final int toggleLightLevel;
    private final int baseLightLevel;

    public ToggleLightMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        toggleLightLevel = Math.min(15, Math.max(0, section.getInt("toggle_light", 0)));
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

        if (block.getType().isAir()) {
            // Create a new Light BlockData for each block to avoid mutating a shared instance
            Light lightData = (Light) Material.LIGHT.createBlockData();
            lightData.setLevel(lightLevel);
            block.setBlockData(lightData);
        } else {
            for (BlockFace face : BLOCK_FACES) {
                Block relative = block.getRelative(face);
                if (!relative.getType().isAir() && relative.getType() != Material.LIGHT) continue;
                if (relative.getBlockData() instanceof Light relativeLight && relativeLight.getLevel() > lightLevel) continue;
                // Create a new Light BlockData for each block to avoid mutating a shared instance
                Light lightData = (Light) Material.LIGHT.createBlockData();
                lightData.setLevel(lightLevel);
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
        
        int lightLevel = hasToggleLight() && isToggledOn(baseEntity) ? toggleLightLevel : baseLightLevel;
        
        // Collect all blocks that need light updates
        List<Block> blocksToUpdate = new ArrayList<>();
        Block baseBlock = baseEntity.getLocation().getBlock();
        blocksToUpdate.add(baseBlock);
        
        for (Location barrierLocation : barrierLocations) {
            Block barrierBlock = barrierLocation.getBlock();
            if (barrierBlock.getType() == Material.BARRIER) {
                blocksToUpdate.add(barrierBlock);
            }
        }
        
        // Collect all light positions that should have lights (with their desired levels)
        // Use normalized Location (block coordinates only) as key to fix equality issues
        Map<Location, Integer> desiredLights = new HashMap<>();
        
        for (Block block : blocksToUpdate) {
            if (lightLevel <= 0) continue;
            
            if (block.getType().isAir()) {
                // If block is air, place light directly in it
                Location blockLoc = normalizeLocation(block.getLocation());
                desiredLights.put(blockLoc, lightLevel);
            } else {
                // Otherwise, place lights in adjacent air/light blocks
                for (BlockFace face : BLOCK_FACES) {
                    Block relative = block.getRelative(face);
                    if (relative.getType().isAir() || relative.getType() == Material.LIGHT) {
                        // Normalize Location to only compare block coordinates (x, y, z, world)
                        Location relativeLoc = normalizeLocation(relative.getLocation());
                        // Use maximum light level if multiple blocks want to light the same position
                        desiredLights.merge(relativeLoc, lightLevel, Math::max);
                    }
                }
            }
        }
        
        // Remove all existing lights around blocks that need updates
        // Remove lights that: (1) aren't in desired set, OR (2) are at a higher level than desired
        // BUT only if they're not adjacent to other furniture's barriers/base entities
        Set<Block> thisFurnitureBlocks = new HashSet<>(blocksToUpdate);
        for (Block block : blocksToUpdate) {
            for (BlockFace face : BLOCK_FACES) {
                Block relative = block.getRelative(face);
                if (relative.getType() == Material.LIGHT) {
                    Location relativeLoc = normalizeLocation(relative.getLocation());
                    Integer desiredLevel = desiredLights.get(relativeLoc);
                    
                    // Remove if: not in desired set, OR existing light level is higher than desired
                    boolean shouldRemove = false;
                    if (desiredLevel == null) {
                        // Location not in desired set - check if it's safe to remove
                        // Only remove if the light is adjacent to THIS furniture's blocks
                        // and not adjacent to any OTHER furniture's blocks
                        if (isLightAdjacentToThisFurnitureOnly(relative, thisFurnitureBlocks, baseEntity)) {
                            shouldRemove = true;
                        }
                    } else if (relative.getBlockData() instanceof Light existingLight) {
                        // Location is desired, but check if existing level is too high
                        // Only remove if it's safe (not adjacent to other furniture)
                        if (existingLight.getLevel() > desiredLevel) {
                            if (isLightAdjacentToThisFurnitureOnly(relative, thisFurnitureBlocks, baseEntity)) {
                                shouldRemove = true;
                            }
                        }
                    }
                    
                    if (shouldRemove) {
                        relative.setType(Material.AIR);
                    }
                }
            }
        }
        
        // Place all desired lights
        for (Map.Entry<Location, Integer> entry : desiredLights.entrySet()) {
            Location lightLoc = entry.getKey();
            int level = entry.getValue();
            Block lightBlock = lightLoc.getBlock();
            
            // Skip if there's already a light with higher level
            if (lightBlock.getBlockData() instanceof Light existingLight && existingLight.getLevel() > level) {
                continue;
            }
            
            // Create a new Light BlockData for each block to avoid mutating a shared instance
            Light lightData = (Light) Material.LIGHT.createBlockData();
            lightData.setLevel(level);
            lightBlock.setBlockData(lightData);
        }
    }
    
    /**
     * Checks if a light block is adjacent to THIS furniture's blocks only,
     * and not adjacent to any other furniture's barriers or base entities.
     * This prevents removing lights that belong to adjacent furniture pieces.
     *
     * @param lightBlock The light block to check
     * @param thisFurnitureBlocks Set of blocks that belong to this furniture
     * @param thisBaseEntity The base entity of this furniture
     * @return true if the light is only adjacent to this furniture's blocks, false otherwise
     */
    private boolean isLightAdjacentToThisFurnitureOnly(Block lightBlock, Set<Block> thisFurnitureBlocks, Entity thisBaseEntity) {
        // Check all adjacent blocks to the light
        for (BlockFace face : BLOCK_FACES) {
            Block adjacent = lightBlock.getRelative(face);
            
            // Skip if this adjacent block belongs to this furniture
            if (thisFurnitureBlocks.contains(adjacent)) {
                continue;
            }
            
            // Check if this adjacent block belongs to another furniture
            if (adjacent.getType() == Material.BARRIER) {
                FurnitureMechanic otherFurniture = OraxenFurniture.getFurnitureMechanic(adjacent);
                if (otherFurniture != null) {
                    // Check if it's a different furniture (not this one)
                    Entity otherBaseEntity = otherFurniture.getBaseEntity(adjacent);
                    if (otherBaseEntity != null && !otherBaseEntity.getUniqueId().equals(thisBaseEntity.getUniqueId())) {
                        // This light is adjacent to another furniture's barrier - don't remove it
                        return false;
                    }
                }
            }
            
            // Check if this adjacent block is at the location of another furniture's base entity
            // Base entities are typically at block center, so check if there's a furniture entity at this block
            if (adjacent.getWorld() != null) {
                Location blockCenter = BlockHelpers.toCenterLocation(adjacent.getLocation());
                for (Entity entity : adjacent.getWorld().getNearbyEntities(blockCenter, 0.1, 0.1, 0.1)) {
                    if (OraxenFurniture.isBaseEntity(entity)) {
                        // Check if the entity's block location matches the adjacent block
                        Block entityBlock = entity.getLocation().getBlock();
                        if (entityBlock.equals(adjacent) && !entity.getUniqueId().equals(thisBaseEntity.getUniqueId())) {
                            // This light is adjacent to another furniture's base entity location - don't remove it
                            return false;
                        }
                    }
                }
            }
        }
        
        // Light is only adjacent to this furniture's blocks (or non-furniture blocks)
        return true;
    }
    
    /**
     * Normalizes a Location to only include block coordinates (x, y, z, world).
     * This ensures Location.equals() works correctly for block-based comparisons
     * by ignoring yaw, pitch, and other non-coordinate properties.
     */
    private Location normalizeLocation(Location loc) {
        Location normalized = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return normalized;
    }
}

