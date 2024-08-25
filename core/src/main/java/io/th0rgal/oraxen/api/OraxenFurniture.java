package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.*;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.seats.FurnitureSeat;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.FURNITURE_KEY;

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
     * Check if a location contains a Furniture
     *
     * @param location The location to check
     * @return true if the location contains a Furniture, otherwise false
     */
    public static boolean isFurniture(Location location) {
        BoundingBox blockBox = BoundingBox.of(BlockHelpers.toCenterLocation(location), 0.5, 0.5, 0.5);
        return (getFurnitureMechanic(location) != null) ||
                !location.getWorld().getNearbyEntities(blockBox).stream().filter(OraxenFurniture::isFurniture).toList().isEmpty();
    }

    /**
     * Check if an itemID has a FurnitureMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a FurnitureMechanic, otherwise false
     */
    public static boolean isFurniture(String itemID) {
        return FurnitureFactory.isEnabled() && !FurnitureFactory.get().isNotImplementedIn(itemID);
    }

    public static boolean isFurniture(Entity entity) {
        return entity != null && entity.getType() == EntityType.ITEM_DISPLAY && getFurnitureMechanic(entity) != null;
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
    public static ItemDisplay place(String itemID, Location location, Rotation rotation, BlockFace blockFace) {
        return place(itemID, location, FurnitureHelpers.rotationToYaw(rotation), blockFace);
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
    public static ItemDisplay place(String itemID, Location location, float yaw, BlockFace blockFace) {
        FurnitureMechanic mechanic = getFurnitureMechanic(itemID);
        if (mechanic == null) return null;
        return mechanic.place(location, yaw, blockFace);
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
        FurnitureMechanic mechanic = getFurnitureMechanic(location);
        mechanic = mechanic != null ? mechanic : entity != null ? getFurnitureMechanic(entity) : null;
        ItemStack itemStack = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        if (mechanic == null) return false;
        assert entity != null;

        ItemDisplay baseEntity = mechanic.baseEntity(location);
        if (baseEntity == null) return false;

        if (player != null) {
            if (player.getGameMode() != GameMode.CREATIVE)
                (drop != null ? drop : mechanic.breakable().drop()).furnitureSpawns(baseEntity, itemStack);
            StorageMechanic storage = mechanic.storage();
            if (storage != null && (storage.isStorage() || storage.isShulker()))
                storage.dropStorageContent(mechanic, baseEntity);

            if (VersionUtil.isPaperServer()) baseEntity.getWorld().sendGameEvent(player, GameEvent.BLOCK_DESTROY, baseEntity.getLocation().toVector());
        }

        mechanic.removeBaseEntity(baseEntity);
        return true;
    }

    /**
     * Removes Furniture at a given Entity, optionally by a player
     *
     * @param baseEntity The entity at which the Furniture should be removed
     * @param player     The player who removed the Furniture, can be null
     * @return true if the Furniture was removed, false otherwise
     */
    public static boolean remove(@NotNull Entity baseEntity, @Nullable Player player) {
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
    public static boolean remove(@NotNull Entity baseEntity, @Nullable Player player, @Nullable Drop drop) {
        if (!FurnitureFactory.isEnabled()) return false;
        if (!(baseEntity instanceof ItemDisplay itemDisplay)) return false;
        FurnitureMechanic mechanic = getFurnitureMechanic(baseEntity);
        if (mechanic == null) return false;
        // Allows for changing the FurnitureType in config and still remove old entities

        if (player != null) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();
            if (player.getGameMode() != GameMode.CREATIVE)
                (drop != null ? drop : mechanic.breakable().drop()).furnitureSpawns(itemDisplay, itemStack);
            StorageMechanic storage = mechanic.storage();
            if (storage != null && (storage.isStorage() || storage.isShulker()))
                storage.dropStorageContent(mechanic, itemDisplay);
            if (VersionUtil.isPaperServer()) itemDisplay.getWorld().sendGameEvent(player, GameEvent.BLOCK_DESTROY, itemDisplay.getLocation().toVector());
        }

        // Check if the mechanic or the baseEntity has barriers tied to it
        mechanic.removeBaseEntity(itemDisplay);
        return true;
    }

    @Nullable
    public static FurnitureMechanic getFurnitureMechanic(Block block) {
        if (!FurnitureFactory.isEnabled() || block == null) return null;
        return getFurnitureMechanic(block.getLocation());
    }

    /**
     * Get the FurnitureMechanic from a given location.
     * This will only return non-null for furniture with a barrier-hitbox
     *
     * @param location The location to get the FurnitureMechanic from
     * @return Instance of this block's FurnitureMechanic, or null if the location is not tied to a Furniture
     */
    @Nullable
    public static FurnitureMechanic getFurnitureMechanic(Location location) {
        if (!FurnitureFactory.isEnabled() || location == null) return null;
        ItemDisplay baseEntity = FurnitureFactory.instance.packetManager().baseEntityFromHitbox(new BlockLocation(location));
        if (baseEntity == null) return null;
        FurnitureMechanic mechanic = getFurnitureMechanic(baseEntity);

        Location centerLoc = BlockHelpers.toCenterBlockLocation(location);
        BoundingBox boundingBox = BoundingBox.of(centerLoc,0.5,1.0,0.5);
        if (mechanic == null) {
            Optional<Entity> entity = centerLoc.getNearbyEntities(2.0,2.0,2.0).stream()
                    .sorted(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(centerLoc)))
                    .filter(e -> e.getBoundingBox().overlaps(boundingBox))
                    .findFirst();
            if (entity.isPresent()) mechanic = getFurnitureMechanic(entity.get());
        }

        return mechanic;
    }

    /**
     * Get the FurnitureMechanic from a given entity.
     *
     * @param baseEntity The entity to get the FurnitureMechanic from
     * @return Returns this entity's FurnitureMechanic, or null if the entity is not tied to a Furniture
     */
    public static FurnitureMechanic getFurnitureMechanic(Entity baseEntity) {
        if (!FurnitureFactory.isEnabled() || baseEntity == null || baseEntity.getType() != EntityType.ITEM_DISPLAY) return null;
        final String itemID = baseEntity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (!OraxenItems.exists(itemID) || FurnitureSeat.isSeat(baseEntity)) return null;
        // Ignore legacy hitbox entities as they should be removed in FurnitureConverter
        if (baseEntity instanceof Interaction) return null;
        return FurnitureFactory.get().getMechanic(itemID);
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
        return FurnitureFactory.get().getMechanic(itemID);
    }

    /**
     * Ensures that the given entity is a Furniture, and updates it if it is
     *
     * @param baseEntity The furniture baseEntity to update
     */
    public static void updateFurniture(@NotNull ItemDisplay baseEntity) {
        if (!FurnitureFactory.isEnabled() || !BlockHelpers.isLoaded(baseEntity.getLocation())) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null || FurnitureSeat.isSeat(baseEntity)) return;

        ItemStack newItem = ItemUpdater.updateItem(FurnitureHelpers.furnitureItem(baseEntity));
        FurnitureHelpers.furnitureItem(baseEntity, newItem);
        FurnitureHelpers.furnitureDye(baseEntity, ItemUtils.dyeColor(newItem).orElse(null));

        IFurniturePacketManager packetManager = FurnitureFactory.instance.packetManager();
        packetManager.removeFurnitureEntityPacket(baseEntity, mechanic);
        packetManager.removeLightMechanicPacket(baseEntity, mechanic);
        packetManager.removeInteractionHitboxPacket(baseEntity, mechanic);
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic);

        for (Player player : baseEntity.getLocation().getNearbyPlayers(FurnitureFactory.get().simulationRadius)) {
            packetManager.sendFurnitureEntityPacket(baseEntity, mechanic, player);
            packetManager.sendLightMechanicPacket(baseEntity, mechanic, player);
            packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player);
            packetManager.sendBarrierHitboxPacket(baseEntity, mechanic, player);
        }
    }
}
