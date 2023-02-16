package io.th0rgal.oraxen.api;

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
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;

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

    /**
     * Places Furniture from a given ID at a given location, optionally by a player
     *
     * @param location The location to place the Furniture
     * @param itemID   The ID of the Furniture to place
     * @param rotation The rotation of the Furniture
     * @param blockFace The blockFace to place the Furniture against
     * @param player   The player who places the Furniture, can be null
     * @return true if the Furniture was placed, false otherwise
     */
    public static boolean place(Location location, String itemID, Rotation rotation, BlockFace blockFace, @Nullable Player player) {
        FurnitureMechanic mechanic = (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(itemID);
        if (mechanic == null) return false;
        return mechanic.place(rotation, mechanic.getYaw(rotation), blockFace, location) != null;
    }

    /**
     * Places Furniture from a given ID at a given location, optionally by a player
     *
     * @param location The location to place the Furniture
     * @param itemID   The ID of the Furniture to place
     * @param player   The player who places the Furniture, can be null
     * @return true if the Furniture was placed, false otherwise
     */
    public static boolean place(Location location, String itemID, @Nullable Player player) {
        FurnitureMechanic mechanic = (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(itemID);
        if (mechanic == null) return false;
        return mechanic.place(location) != null;
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

        ItemFrame itemFrame = mechanic.getItemFrame(block);
        if (itemFrame == null) return false;

        if (mechanic.removeSolid(block) && (!mechanic.isStorage() || !mechanic.getStorage().isShulker())) {
            if (player != null && player.getGameMode() != GameMode.CREATIVE)
                mechanic.getDrop().furnitureSpawns(itemFrame, itemStack);
        }
        if (mechanic.hasBarriers())
            mechanic.removeSolid(itemFrame.getWorld(), new BlockLocation(itemFrame.getLocation()), mechanic.getYaw(itemFrame.getRotation()));
        else mechanic.removeAirFurniture(itemFrame);
        return true;
    }

    /**
     * Removes Furniture at a given Entity, optionally by a player
     *
     * @param entity The entity at which the Furniture should be removed
     * @param player The player who removed the Furniture, can be null
     * @return true if the Furniture was removed, false otherwise
     */
    public static boolean remove(Entity entity, @Nullable Player player) {
        FurnitureMechanic mechanic = getFurnitureMechanic(entity);
        if (mechanic == null || !(entity instanceof ItemFrame itemFrame)) return false;
        ItemStack itemStack = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);

        if (player != null && player.getGameMode() != GameMode.CREATIVE)
            mechanic.getDrop().furnitureSpawns(itemFrame, itemStack);

        if (mechanic.hasBarriers())
            mechanic.removeSolid(itemFrame.getWorld(), new BlockLocation(itemFrame.getLocation()), mechanic.getYaw(itemFrame.getRotation()));
        else mechanic.removeAirFurniture(itemFrame);
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
        if (!(entity instanceof ItemFrame)) return null;
        final String itemID = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (!OraxenItems.exists(itemID)) return null;
        return (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(itemID);
    }
}
