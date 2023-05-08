package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.stream.Collectors;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.FURNITURE_KEY;

public class OraxenFurniture {

    /**
     * Check if a block is an instance of a Furniture
     *
     * @param block The block to check
     * @return true if the block is an instance of a Furniture, otherwise false
     */
    public static boolean isFurniture(Block block) {
        return block.getType() == Material.BARRIER && getFurnitureMechanic(block) != null;
    }

    /**
     * Check if an itemID has a FurnitureMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a FurnitureMechanic, otherwise false
     */
    public static boolean isFurniture(String itemID) {
        return !FurnitureFactory.getInstance().isNotImplementedIn(itemID);
    }

    public static boolean isFurniture(Entity entity) {
        return getFurnitureMechanic(entity) != null;
    }

    public static boolean place(Location location, String itemID, Rotation rotation, BlockFace blockFace) {
        FurnitureMechanic mechanic = (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(itemID);
        if (mechanic == null) return false;
        return mechanic.place(location, FurnitureMechanic.rotationToYaw(rotation), blockFace) != null;
    }

    /**
     * Removes Furniture at a given location, optionally by a player
     *
     * @param location The location to remove the Furniture
     * @param player   The player who removed the Furniture, can be null
     * @return true if the Furniture was removed, false otherwise
     */
    public static boolean remove(Location location, @Nullable Player player) {
        Block block = location.getBlock();
        FurnitureMechanic mechanic = getFurnitureMechanic(block);
        ItemStack itemStack = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        if (mechanic == null) return false;

        Entity baseEntity = mechanic.getBaseEntity(block);
        if (baseEntity == null) return false;

        if (mechanic.removeSolid(block) && (!mechanic.isStorage() || !mechanic.getStorage().isShulker())) {
            if (player != null && player.getGameMode() != GameMode.CREATIVE)
                mechanic.getDrop().furnitureSpawns(baseEntity, itemStack);
        }
        if (mechanic.hasBarriers())
            for (Block barrier : mechanic.getBarriers().stream().map(blockLoc -> blockLoc.toLocation(baseEntity.getWorld()).getBlock()).collect(Collectors.toSet()))
                if (block.getType() == Material.BARRIER) mechanic.removeSolid(barrier);
                else mechanic.removeAirFurniture(baseEntity);
        return true;
    }

    /**
     * Removes Furniture at a given Entity, optionally by a player
     *
     * @param baseEntity The entity at which the Furniture should be removed
     * @param player The player who removed the Furniture, can be null
     * @return true if the Furniture was removed, false otherwise
     */
    public static boolean remove(Entity baseEntity, @Nullable Player player) {
        FurnitureMechanic mechanic = getFurnitureMechanic(baseEntity);
        if (mechanic == null) return false;
        // Return if entity is interaction. Otherwise, remove it based on mechanic
        // Allows for changing the FurnitureType in config and still remove old entities
        if (OraxenPlugin.supportsDisplayEntities && baseEntity.getType() == EntityType.INTERACTION) return false;
        ItemStack itemStack = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);

        if (player != null && player.getGameMode() != GameMode.CREATIVE)
            mechanic.getDrop().furnitureSpawns(baseEntity, itemStack);
        if (mechanic.hasBarriers())
            mechanic.removeSolid(baseEntity.getWorld(), new BlockLocation(baseEntity.getLocation()), FurnitureMechanic.getFurnitureYaw(baseEntity));
        else mechanic.removeAirFurniture(baseEntity);
        return true;
    }

    /**
     * Get the FurnitureMechanic from a given block.
     * This will only return non-null for furniture with a barrier-hitbox
     *
     * @param block The block to get the FurnitureMechanic from
     * @return Instance of this block's FurnitureMechanic, or null if the block is not tied to a Furniture
     */
    public static FurnitureMechanic getFurnitureMechanic(Block block) {
        if (block.getType() != Material.BARRIER) return null;
        final String mechanicID = BlockHelpers.getPDC(block).get(FURNITURE_KEY, PersistentDataType.STRING);
        return (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(mechanicID);
    }

    /**
     * Get the FurnitureMechanic from a given block.
     * This will only return non-null for furniture with a barrier-hitbox
     *
     * @param entity The entity to get the FurnitureMechanic from
     * @return Returns this entity's FurnitureMechanic, or null if the entity is not tied to a Furniture
     */
    public static FurnitureMechanic getFurnitureMechanic(Entity entity) {
        final String itemID = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (!OraxenItems.exists(itemID)) return null;
        return (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(itemID);
    }

    /**
     * Get the FurnitureMechanic from a given block.
     * This will only return non-null for furniture with a barrier-hitbox
     *
     * @param itemID The itemID tied to this FurnitureMechanic
     * @return Returns the FurnitureMechanic tied to this itemID, or null if the itemID is not tied to a Furniture
     */
    public static FurnitureMechanic getFurnitureMechanic(String itemID) {
        if (!OraxenItems.exists(itemID)) return null;
        return (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(itemID);
    }

    /**
     * Ensures that the given entity is a Furniture, and updates its item if it is
     * @param entity
     */
    public static void updateFurniture(Entity entity) {
        if (!OraxenFurniture.isFurniture(entity)) return;
        entity = OraxenFurniture.getFurnitureMechanic(entity).getBaseEntity(entity);
        if (entity == null) return;
        ItemStack oldItem = FurnitureMechanic.getFurnitureItem(entity);
        ItemStack newItem = ItemUpdater.updateItem(oldItem);
        if (oldItem == null || oldItem.equals(newItem)) return;
        FurnitureMechanic.setFurnitureItem(entity, newItem);
    }
}
