package io.th0rgal.oraxen.api;

import com.jeff_media.persistentdataserializer.PersistentDataSerializer;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemUpdater;
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
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.BASE_ENTITY_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.FURNITURE_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.INTERACTION_KEY;

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

    public static boolean isBaseEntity(Entity entity) {
        FurnitureMechanic mechanic = getFurnitureMechanic(entity);
        return mechanic != null && mechanic.getFurnitureEntityType() == entity.getType();
    }

    public static boolean isInteractionEntity(Entity entity) {
        FurnitureMechanic mechanic = getFurnitureMechanic(entity);
        return mechanic != null && OraxenPlugin.supportsDisplayEntities && entity.getType() == EntityType.INTERACTION;
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
    public static boolean remove(@NotNull Location location, @Nullable Player player) {
        if (!location.isWorldLoaded()) return false;
        assert location.getWorld() != null;

        Entity entity = location.getWorld().getNearbyEntities(location, 0.5,0.5,0.5).stream().filter(OraxenFurniture::isFurniture).findFirst().orElse(null);
        FurnitureMechanic mechanic = getFurnitureMechanic(location.getBlock());
        mechanic = mechanic != null ? mechanic : getFurnitureMechanic(entity);
        ItemStack itemStack = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        if (mechanic == null) return false;

        Entity baseEntity = mechanic.getBaseEntity(location.getBlock());
        baseEntity = baseEntity != null ? baseEntity : mechanic.getBaseEntity(entity);
        if (baseEntity == null) return false;

        if (player != null && player.getGameMode() != GameMode.CREATIVE)
            mechanic.getDrop().furnitureSpawns(baseEntity, itemStack);
        if (mechanic.hasBarriers())
            mechanic.removeSolid(baseEntity.getLocation(), FurnitureMechanic.getFurnitureYaw(baseEntity));
        else mechanic.removeNonSolidFurniture(baseEntity);
        return true;
    }

    /**
     * Removes Furniture at a given Entity, optionally by a player
     *
     * @param baseEntity The entity at which the Furniture should be removed
     * @param player     The player who removed the Furniture, can be null
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
            mechanic.removeSolid(baseEntity.getLocation(), FurnitureMechanic.getFurnitureYaw(baseEntity));
        else mechanic.removeNonSolidFurniture(baseEntity);
        return true;
    }

    /**
     * Get the FurnitureMechanic from a given block.
     * This will only return non-null for furniture with a barrier-hitbox
     *
     * @param block The block to get the FurnitureMechanic from
     * @return Instance of this block's FurnitureMechanic, or null if the block is not tied to a Furniture
     */
    @Nullable
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
        if (entity == null) return null;
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
     *
     * @param entity
     */
    public static void updateFurniture(Entity entity) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return;
        entity = mechanic.getBaseEntity(entity);
        if (entity == null) return;

        ItemStack oldItem = FurnitureMechanic.getFurnitureItem(entity);
        ItemStack newItem = ItemUpdater.updateItem(oldItem);

        if (Settings.EXPERIMENTAL_FURNITURE_TYPE_UPDATE.toBool() && mechanic.getFurnitureEntityType() != entity.getType()) {
            final PersistentDataContainer oldPdc = entity.getPersistentDataContainer();
            final BlockFace oldFacing = entity instanceof ItemFrame itemFrame ? itemFrame.getAttachedFace() : BlockFace.UP;
            if (!OraxenFurniture.remove(entity, null)) return;
            entity = mechanic.place(entity.getLocation(), newItem, FurnitureMechanic.getFurnitureYaw(entity), oldFacing);
            PersistentDataContainer newPdc = entity.getPersistentDataContainer();

            // Copy old PDC to new PDC
            List<Map<?, ?>> serializedPdc = PersistentDataSerializer.toMapList(oldPdc);
            serializedPdc.removeIf(map -> map.containsKey(INTERACTION_KEY) || map.containsKey(FURNITURE_KEY) || map.containsKey(BASE_ENTITY_KEY));
            PersistentDataSerializer.fromMapList(serializedPdc, newPdc);
        }

        FurnitureMechanic.setFurnitureItem(entity, newItem);
    }
}
