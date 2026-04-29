package io.th0rgal.oraxen.api;

import com.jeff_media.customblockdata.CustomBlockData;
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
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.*;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic.PERSONAL_STORAGE_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic.STORAGE_KEY;
import static io.th0rgal.oraxen.utils.MusicDiscHelpers.MUSIC_DISC_KEY;

public class OraxenFurniture {

    /**
     * Gett all OraxenItem IDs that have a FurnitureMechanic
     *
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
        BoundingBox blockBox = BoundingBox.of(BlockHelpers.toCenterLocation(block.getLocation()), 0.5, 0.5, 0.5);
        return (block.getType() == Material.BARRIER && getFurnitureMechanic(block) != null) ||
                !block.getWorld().getNearbyEntities(blockBox).stream().filter(OraxenFurniture::isFurniture).toList().isEmpty();
    }

    /**
     * Check if an itemID has a FurnitureMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a FurnitureMechanic, otherwise false
     */
    public static boolean isFurniture(String itemID) {
        return FurnitureFactory.isEnabled() && !FurnitureFactory.getInstance().isNotImplementedIn(itemID);
    }

    public static boolean isFurniture(Entity entity) {
        return entity != null && getFurnitureMechanic(entity) != null;
    }

    public static boolean isOrphanFurnitureEntity(Entity entity) {
        return hasFurnitureEntityMarker(entity) && getFurnitureMechanic(entity) == null;
    }

    public static boolean hasFurnitureEntityMarker(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING);
    }

    public static boolean hasFurnitureBlockMarker(Block block) {
        return block != null
                && block.getType() == Material.BARRIER
                && BlockHelpers.getPDC(block).has(FURNITURE_KEY, PersistentDataType.STRING);
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

    /**
     * Places Furniture at a given location
     * @param location The location to place the Furniture
     * @param itemID The itemID of the Furniture to place
     * @param rotation The rotation of the Furniture
     * @param blockFace The blockFace of the Furniture
     * @return The Furniture entity that was placed, or null if the Furniture could not be placed
     */
    @Nullable
    public static Entity place(String itemID, Location location, Rotation rotation, BlockFace blockFace) {
        return place(itemID, location, FurnitureMechanic.rotationToYaw(rotation), blockFace);
    }

    /**
     * Places Furniture at a given location
     * @param location The location to place the Furniture
     * @param itemID The itemID of the Furniture to place
     * @param yaw The yaw of the Furniture
     * @param blockFace The blockFace of the Furniture
     * @return The Furniture entity that was placed, or null if the Furniture could not be placed
     */
    @Nullable
    public static Entity place(String itemID, Location location, float yaw, BlockFace blockFace) {
        FurnitureMechanic mechanic = getFurnitureMechanic(itemID);
        if (mechanic == null) return null;
        return mechanic.place(location, yaw, blockFace);
    }

    /**
     * Places Furniture at a given location
     * @param location The location to place the Furniture
     * @param itemID The itemID of the Furniture to place
     * @param rotation The rotation of the Furniture
     * @param blockFace The blockFace of the Furniture
     * @return true if the Furniture was placed, false otherwise
     * @deprecated Use {@link #place(String, Location, Rotation, BlockFace)} instead
     */
    @Deprecated(since = "1.162.0", forRemoval = true)
    public static boolean place(Location location, String itemID, Rotation rotation, BlockFace blockFace) {
        FurnitureMechanic mechanic = getFurnitureMechanic(itemID);
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
        return remove(location, player, null);
    }

    /**
     * Removes Furniture at a given location, optionally by a player
     *
     * @param location The location to remove the Furniture
     * @param player   The player who removed the Furniture, can be null
     * @param drop     The drop of the furniture, if null the default drop will be used
     * @return true if the Furniture was removed, false otherwise
     */
    public static boolean remove(@NotNull Location location, @Nullable Player player, @Nullable Drop drop) {
        if (!FurnitureFactory.isEnabled()) return false;
        if (!location.isWorldLoaded()) return false;
        assert location.getWorld() != null;

        Entity entity = location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5).stream().filter(OraxenFurniture::isFurniture).findFirst().orElse(null);
        FurnitureMechanic mechanic = getFurnitureMechanic(location.getBlock());
        mechanic = mechanic != null ? mechanic : entity != null ? getFurnitureMechanic(entity) : null;
        if (mechanic == null) return removeOrphanFurniture(location);

        Entity baseEntity = mechanic.getBaseEntity(location.getBlock());
        baseEntity = baseEntity != null ? baseEntity : mechanic.getBaseEntity(entity);
        if (baseEntity == null) return false;

        return removeFurniture(baseEntity, mechanic, player, drop, mechanic.hasBarriers());
    }

    /**
     * Removes Furniture at a given Entity, optionally by a player
     *
     * @param baseEntity The entity at which the Furniture should be removed
     * @param player     The player who removed the Furniture, can be null
     * @return true if the Furniture was removed, false otherwise
     */
    public static boolean remove(Entity baseEntity, @Nullable Player player) {
        return remove(baseEntity, player, null);
    }

    /**
     * Removes Furniture at a given Entity, optionally by a player and with an altered Drop
     *
     * @param baseEntity The entity at which the Furniture should be removed
     * @param player     The player who removed the Furniture, can be null
     * @param drop       The drop of the furniture, if null the default drop will be used
     * @return true if the Furniture was removed, false otherwise
     */
    public static boolean remove(Entity baseEntity, @Nullable Player player, @Nullable Drop drop) {
        if (!FurnitureFactory.isEnabled() || baseEntity == null) return false;
        FurnitureMechanic mechanic = getFurnitureMechanic(baseEntity);
        if (mechanic == null) return removeOrphanFurniture(baseEntity);
        // Ensure the baseEntity is baseEntity and not interactionEntity
        if (OraxenFurniture.isInteractionEntity(baseEntity)) baseEntity = mechanic.getBaseEntity(baseEntity);
        if (baseEntity == null) return false;

        return removeFurniture(baseEntity, mechanic, player, drop, mechanic.hasBarriers(baseEntity));
    }

    /**
     * Shared removal logic for furniture: handles drops, storage cleanup, game events, and entity removal.
     */
    private static boolean removeFurniture(Entity baseEntity, FurnitureMechanic mechanic,
                                            @Nullable Player player, @Nullable Drop drop, boolean hasBarriers) {
        if (player != null) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();
            if (player.getGameMode() != GameMode.CREATIVE)
                (drop != null ? drop : mechanic.getDrop()).furnitureSpawns(baseEntity, itemStack);
            StorageMechanic storage = mechanic.getStorage();
            if (storage != null && (storage.isStorage() || storage.isShulker()))
                storage.dropStorageContent(mechanic, baseEntity);
            if (VersionUtil.isPaperServer())
                baseEntity.getWorld().sendGameEvent(player, GameEvent.BLOCK_DESTROY, baseEntity.getLocation().toVector());
        }

        if (hasBarriers)
            mechanic.removeSolid(baseEntity, baseEntity.getLocation(), FurnitureMechanic.getFurnitureYaw(baseEntity));
        else mechanic.removeNonSolidFurniture(baseEntity);
        return true;
    }

    private static boolean removeOrphanFurniture(@NotNull Location location) {
        if (!location.isWorldLoaded()) return false;
        assert location.getWorld() != null;

        Entity orphanEntity = location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5).stream()
                .filter(OraxenFurniture::hasFurnitureEntityMarker)
                .findFirst()
                .orElse(null);
        if (orphanEntity != null) return removeOrphanFurniture(orphanEntity);

        Block block = location.getBlock();
        if (!hasFurnitureBlockMarker(block)) return false;

        UUID baseEntityUUID = BlockHelpers.getPDC(block).get(BASE_ENTITY_KEY, DataType.UUID);
        if (baseEntityUUID != null) {
            Entity baseEntity = Bukkit.getEntity(baseEntityUUID);
            if (baseEntity != null && hasFurnitureEntityMarker(baseEntity))
                return removeOrphanFurniture(baseEntity);
        }

        removeOrphanBarrierBlock(block, null);
        return true;
    }

    private static boolean removeOrphanFurniture(@NotNull Entity entity) {
        if (!hasFurnitureEntityMarker(entity)) return false;

        Entity baseEntity = resolveBaseEntity(entity);
        if (baseEntity == null) {
            removeOrphanSeat(entity.getPersistentDataContainer());
            if (!entity.isDead()) entity.remove();
            return true;
        }

        removeOrphanBarriers(baseEntity);
        removeOrphanSeat(baseEntity.getPersistentDataContainer());

        List<UUID> interactionUUIDs = baseEntity.getPersistentDataContainer()
                .getOrDefault(INTERACTIONS_KEY, DataType.asList(DataType.UUID), new ArrayList<>());
        UUID interactionUUID = baseEntity.getPersistentDataContainer().get(INTERACTION_KEY, DataType.UUID);
        if (interactionUUID != null && !interactionUUIDs.contains(interactionUUID)) {
            interactionUUIDs = new ArrayList<>(interactionUUIDs);
            interactionUUIDs.add(interactionUUID);
        }
        for (UUID uuid : interactionUUIDs) {
            Entity interactionEntity = Bukkit.getEntity(uuid);
            if (interactionEntity != null && !interactionEntity.isDead()) interactionEntity.remove();
        }

        if (!baseEntity.isDead()) baseEntity.remove();
        if (entity != baseEntity && !entity.isDead()) entity.remove();
        return true;
    }

    @Nullable
    private static Entity resolveBaseEntity(@NotNull Entity entity) {
        UUID baseEntityUUID = entity.getPersistentDataContainer().get(BASE_ENTITY_KEY, DataType.UUID);
        if (baseEntityUUID != null) {
            Entity baseEntity = Bukkit.getEntity(baseEntityUUID);
            if (baseEntity != null) return baseEntity;
        }

        return entity.getType() == EntityType.INTERACTION ? null : entity;
    }

    private static void removeOrphanBarriers(@NotNull Entity baseEntity) {
        List<BlockLocation> barrierLocations = baseEntity.getPersistentDataContainer().getOrDefault(BARRIER_KEY,
                DataType.asList(BlockLocation.dataType), new ArrayList<>());
        float yaw = FurnitureMechanic.getFurnitureYaw(baseEntity);
        Location rootLocation = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        UUID baseEntityUUID = baseEntity.getUniqueId();

        for (BlockLocation barrierLocation : barrierLocations) {
            Block block = barrierLocation.groundRotate(yaw).add(rootLocation).getBlock();
            removeOrphanBarrierBlock(block, baseEntityUUID);
        }

        int maxOffset = barrierLocations.stream()
                .mapToInt(loc -> Math.max(Math.abs(loc.getX()), Math.max(Math.abs(loc.getY()), Math.abs(loc.getZ()))))
                .max()
                .orElse(0);
        int scanRadius = Math.max(4, maxOffset + 2);
        Location center = baseEntity.getLocation();

        for (int x = center.getBlockX() - scanRadius; x <= center.getBlockX() + scanRadius; x++) {
            for (int y = center.getBlockY() - scanRadius; y <= center.getBlockY() + scanRadius; y++) {
                for (int z = center.getBlockZ() - scanRadius; z <= center.getBlockZ() + scanRadius; z++) {
                    Block block = center.getWorld().getBlockAt(x, y, z);
                    removeOrphanBarrierBlock(block, baseEntityUUID);
                }
            }
        }
    }

    private static void removeOrphanBarrierBlock(@NotNull Block block, @Nullable UUID baseEntityUUID) {
        if (!hasFurnitureBlockMarker(block)) return;

        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        UUID markerBaseEntityUUID = pdc.get(BASE_ENTITY_KEY, DataType.UUID);
        if (baseEntityUUID != null && (markerBaseEntityUUID == null || !baseEntityUUID.equals(markerBaseEntityUUID)))
            return;

        removeOrphanSeat(pdc);
        block.setType(Material.AIR);
        new CustomBlockData(block, OraxenPlugin.get()).clear();
    }

    private static void removeOrphanSeat(@NotNull PersistentDataContainer pdc) {
        UUID seatUUID = pdc.get(SEAT_KEY, DataType.UUID);
        if (seatUUID == null) return;
        Entity seatEntity = Bukkit.getEntity(seatUUID);
        if (seatEntity instanceof ArmorStand seat) {
            seat.getPassengers().forEach(seat::removePassenger);
            if (!seat.isDead()) seat.remove();
        }
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
        if (!FurnitureFactory.isEnabled() || block == null) return null;
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
        if (!FurnitureFactory.isEnabled() || entity == null) return null;
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
        if (!FurnitureFactory.isEnabled() || !OraxenItems.exists(itemID)) return null;
        return FurnitureFactory.getInstance().getMechanic(itemID);
    }

    /**
     * Ensures that the given entity is a Furniture, and updates it if it is
     *
     * @param entity The furniture baseEntity to update
     */
    public static void updateFurniture(@NotNull Entity entity) {
        if (!FurnitureFactory.isEnabled()) return;
        if (!BlockHelpers.isLoaded(entity.getLocation())) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return;
        entity = mechanic.getBaseEntity(entity);
        if (entity == null) return;

        ItemStack oldItem = FurnitureMechanic.getFurnitureItem(entity);
        ItemStack newItem = ItemUpdater.updateItem(oldItem);
        FurnitureMechanic.setFurnitureItem(entity, newItem);

        // Refresh light based on current toggle state
        mechanic.refreshLight(entity);

        if (Settings.EXPERIMENTAL_FURNITURE_TYPE_UPDATE.toBool()) {
            final PersistentDataContainer oldPdc = entity.getPersistentDataContainer();
            final BlockFace oldFacing = entity instanceof ItemFrame itemFrame ? itemFrame.getAttachedFace() : BlockFace.UP;

            // Check if furnitureType changed, if so remove and place new
            if (entity.getType() == mechanic.getFurnitureEntityType()) {
                // Check if barriers changed, if so remove and place new
                if (mechanic.getBarriers().equals(oldPdc.getOrDefault(BARRIER_KEY, DataType.asList(BlockLocation.dataType), new ArrayList<>()))) {
                    if (OraxenPlugin.supportsDisplayEntities) {
                        List<Interaction> interactions = mechanic.getInteractionEntities(entity);
                        // Check if interaction-hitbox changed, if so remove and place new
                        if (!interactions.isEmpty() && mechanic.hasHitbox() && hasSameHitboxes(interactions, mechanic.getHitboxes()))
                            // Check if seat changed, if so remove and place new
                            if (oldPdc.has(SEAT_KEY, DataType.UUID) && mechanic.hasSeat())
                                // Check if any displayEntity properties changed, if so remove and place new
                                if (mechanic.hasDisplayEntityProperties() && mechanic.getDisplayEntityProperties().ensureSameDisplayProperties(entity))
                                    return;
                    } else return;
                }
            }

            if (!OraxenFurniture.remove(entity, null)) return;
            Entity newEntity = mechanic.place(entity.getLocation(), newItem, FurnitureMechanic.getFurnitureYaw(entity), oldFacing, true);
            if (newEntity == null) return;

            // Copy old PDC to new PDC, skip keys that should not persist
            List<Map<?, ?>> serializedPdc = PersistentDataSerializer.toMapList(oldPdc);
            serializedPdc.removeIf(map -> Stream.of(MUSIC_DISC_KEY, EVOLUTION_KEY, STORAGE_KEY, PERSONAL_STORAGE_KEY).map(NamespacedKey::toString).noneMatch(map::containsValue));
            PersistentDataSerializer.fromMapList(serializedPdc, newEntity.getPersistentDataContainer());
        }
    }

    private static boolean hasSameHitboxes(List<Interaction> interactions, List<FurnitureMechanic.FurnitureHitbox> hitboxes) {
        if (interactions.size() != hitboxes.size()) return false;
        for (int i = 0; i < hitboxes.size(); i++) {
            Interaction interaction = interactions.get(i);
            FurnitureMechanic.FurnitureHitbox hitbox = hitboxes.get(i);
            if (interaction.getInteractionWidth() != hitbox.width()) return false;
            if (interaction.getInteractionHeight() != hitbox.height()) return false;
        }
        return true;
    }
}
