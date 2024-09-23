package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox.BarrierHitbox;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface IFurniturePacketManager {

    @NotNull NamespacedKey FURNITURE_PACKET_LISTENER = NamespacedKey.fromString("furniture_listener", OraxenPlugin.get());

    BlockData BARRIER_DATA = Material.BARRIER.createBlockData();
    BlockData AIR_DATA = Material.AIR.createBlockData();

    Set<FurnitureBaseEntity> furnitureBaseMap = new HashSet<>();
    Map<Integer, Set<BarrierHitbox>> barrierHitboxPositionMap = new HashMap<>();
    Map<Integer, Set<LightBlock>> lightMechanicPositionMap = new HashMap<>();
    Set<FurnitureSubEntity> interactionHitboxIdMap = new HashSet<>();

    int nextEntityId();
    @Nullable Entity getEntity(int entityId);

    default Optional<FurnitureBaseEntity> furnitureBaseFromBaseEntity(@NotNull Entity baseEntity) {
        for (FurnitureBaseEntity f : furnitureBaseMap) if (f.baseUUID() == baseEntity.getUniqueId()) return Optional.of(f);
        return Optional.empty();
    }

    @Nullable
    default Entity baseEntityFromFurnitureBase(int furnitureBaseId) {
        for (FurnitureBaseEntity f : furnitureBaseMap)
            if (f.baseId() == furnitureBaseId) return f.baseEntity();
        return null;
    }

    @Nullable
    default ItemDisplay baseEntityFromHitbox(int interactionId) {
        for (FurnitureSubEntity h : interactionHitboxIdMap)
            if (h.entityIds().contains(interactionId)) return h.baseEntity();
        return null;
    }

    @Nullable
    default ItemDisplay baseEntityFromHitbox(BlockLocation barrierLocation) {
        for (Map.Entry<Integer, Set<BarrierHitbox>> entry : barrierHitboxPositionMap.entrySet()) {
            for (BarrierHitbox barrierHitbox : entry.getValue())
                if (barrierLocation.equals(barrierHitbox)) return (ItemDisplay) getEntity(entry.getKey());
        }
        return null;
    }

    default void sendFurnitureEntityPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {}
    default void removeFurnitureEntityPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic) {}
    default void removeFurnitureEntityPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {}

    void sendInteractionEntityPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player);
    void removeInteractionHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic);
    void removeInteractionHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player);

    void sendBarrierHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player);
    void removeBarrierHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic);
    void removeBarrierHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player);

    default void sendLightMechanicPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {}
    default void removeLightMechanicPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic) {}
    default void removeLightMechanicPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {}

    default void removeAllFurniturePackets() {
        for (World world : Bukkit.getWorlds()) for (ItemDisplay entity : world.getEntitiesByClass(ItemDisplay.class)) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
            if (mechanic == null) continue;
            removeFurnitureEntityPacket(entity, mechanic);
            removeLightMechanicPacket(entity, mechanic);
            removeInteractionHitboxPacket(entity, mechanic);
            removeBarrierHitboxPacket(entity, mechanic);
        }
    }
}
