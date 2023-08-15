package io.th0rgal.oraxen.api;

import com.jeff_media.morepersistentdatatypes.DataType;
import com.jeff_media.persistentdataserializer.PersistentDataSerializer;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.BARRIER_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.EVOLUTION_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.FURNITURE_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.SEAT_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic.PERSONAL_STORAGE_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic.STORAGE_KEY;
import static io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscListener.MUSIC_DISC_KEY;

public class OraxenFurniture {

    /**
     * Gett all OraxenItem IDs that have a FurnitureMechanic
     * @return a Set of all OraxenItem IDs that have a FurnitureMechanic
     */
    public static Set<String> getFurnitureIDs() {
        return Arrays.stream(OraxenItems.getItemNames()).filter(OraxenFurniture::isFurniture).collect(Collectors.toSet());
    }

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
        return entity != null && getFurnitureMechanic(entity) != null;
    }

    public static boolean isBaseEntity(Entity entity) {
        if (entity == null) return false;
        FurnitureMechanic mechanic = getFurnitureMechanic(entity);
        // Commented out as this breaks FurnitureUpdating when type is different
        //return mechanic != null && mechanic.getFurnitureEntityType() == entity.getType();
        return mechanic != null && (!OraxenPlugin.supportsDisplayEntities || entity.getType() != EntityType.INTERACTION);
    }

    public static boolean isInteractionEntity(@NotNull Entity entity) {
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

        Entity entity = location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5).stream().filter(OraxenFurniture::isFurniture).findFirst().orElse(null);
        FurnitureMechanic mechanic = getFurnitureMechanic(location.getBlock());
        mechanic = mechanic != null ? mechanic : entity != null ? getFurnitureMechanic(entity) : null;
        ItemStack itemStack = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        if (mechanic == null) return false;
        assert entity != null;

        Entity baseEntity = mechanic.getBaseEntity(location.getBlock());
        baseEntity = baseEntity != null ? baseEntity : mechanic.getBaseEntity(entity);
        if (baseEntity == null) return false;

        if (player != null) {
            if (player.getGameMode() != GameMode.CREATIVE)
                mechanic.getDrop().furnitureSpawns(baseEntity, itemStack);
            StorageMechanic storage = mechanic.getStorage();
            if (storage != null && (storage.isStorage() || storage.isShulker()))
                storage.dropStorageContent(mechanic, baseEntity);
        }

        if (mechanic.hasBarriers())
            mechanic.removeSolid(baseEntity, baseEntity.getLocation(), FurnitureMechanic.getFurnitureYaw(baseEntity));
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
        // Ensure the baseEntity is baseEntity and not interactionEntity
        if (OraxenFurniture.isInteractionEntity(baseEntity)) baseEntity = mechanic.getBaseEntity(baseEntity);
        if (baseEntity == null) return false;
        // Allows for changing the FurnitureType in config and still remove old entities
        ItemStack itemStack = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);

        if (player != null) {
            if (player.getGameMode() != GameMode.CREATIVE)
                mechanic.getDrop().furnitureSpawns(baseEntity, itemStack);
            StorageMechanic storage = mechanic.getStorage();
            if (storage != null && (storage.isStorage() || storage.isShulker()))
                storage.dropStorageContent(mechanic, baseEntity);
        }

        // Check if the mechanic or the baseEntity has barriers tied to it
        if (mechanic.hasBarriers(baseEntity))
            mechanic.removeSolid(baseEntity, baseEntity.getLocation(), FurnitureMechanic.getFurnitureYaw(baseEntity));
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
     * Ensures that the given entity is a Furniture, and updates it if it is
     *
     * @param entity The furniture baseEntity to update
     */
    public static void updateFurniture(@NotNull Entity entity) {
        if (!BlockHelpers.isLoaded(entity.getLocation())) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return;
        entity = mechanic.getBaseEntity(entity);
        if (entity == null) return;

        ItemStack oldItem = FurnitureMechanic.getFurnitureItem(entity);
        ItemStack newItem = ItemUpdater.updateItem(oldItem);
        FurnitureMechanic.setFurnitureItem(entity, newItem);

        if (Settings.EXPERIMENTAL_FURNITURE_TYPE_UPDATE.toBool()) {
            final PersistentDataContainer oldPdc = entity.getPersistentDataContainer();
            final BlockFace oldFacing = entity instanceof ItemFrame itemFrame ? itemFrame.getAttachedFace() : BlockFace.UP;

            // Check if furnitureType changed, if so remove and place new
            if (entity.getType() == mechanic.getFurnitureEntityType()) {
                // Check if barriers changed, if so remove and place new
                if (mechanic.getBarriers().equals(oldPdc.getOrDefault(BARRIER_KEY, DataType.asList(BlockLocation.dataType), new ArrayList<>()))) {
                    if (OraxenPlugin.supportsDisplayEntities) {
                        Interaction interaction = mechanic.getInteractionEntity(entity);
                        // Check if interaction-hitbox changed, if so remove and place new
                        if (interaction != null && mechanic.hasHitbox())
                            if (interaction.getInteractionWidth() == mechanic.getHitbox().width())
                                if (interaction.getInteractionHeight() == mechanic.getHitbox().height())
                                    // Check if seat changed, if so remove and place new
                                    if (oldPdc.has(SEAT_KEY, DataType.UUID) && mechanic.hasSeat())
                                        // Check if any displayEntity properties changed, if so remove and place new
                                        if (mechanic.hasDisplayEntityProperties() && mechanic.getDisplayEntityProperties().ensureSameDisplayProperties(entity))
                                            return;
                    } else return;
                }
            }

            if (!OraxenFurniture.remove(entity, null)) return;
            Entity newEntity = mechanic.place(entity.getLocation(), newItem, FurnitureMechanic.getFurnitureYaw(entity), oldFacing);
            if (newEntity == null) return;

            // Copy old PDC to new PDC, skip keys that should not persist
            List<Map<?, ?>> serializedPdc = PersistentDataSerializer.toMapList(oldPdc);
            serializedPdc.removeIf(map -> Stream.of(MUSIC_DISC_KEY, EVOLUTION_KEY, STORAGE_KEY, PERSONAL_STORAGE_KEY).map(NamespacedKey::toString).noneMatch(map::containsValue));
            PersistentDataSerializer.fromMapList(serializedPdc, newEntity.getPersistentDataContainer());
        }
    }
}
