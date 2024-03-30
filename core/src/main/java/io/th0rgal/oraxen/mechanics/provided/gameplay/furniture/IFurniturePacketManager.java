package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.comphenix.protocol.wrappers.BlockPosition;
import io.th0rgal.oraxen.api.OraxenFurniture;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface IFurniturePacketManager {

    BlockData BARRIER_DATA = Material.BARRIER.createBlockData();
    BlockData AIR_DATA = Material.AIR.createBlockData();

    Map<UUID, Set<BlockPosition>> barrierHitboxPositionMap = new HashMap<>();
    Set<FurnitureSubEntity> interactionHitboxIdMap = new HashSet<>();

    @Nullable
    default Entity baseEntityFromHitbox(int interactionId) {
        for (FurnitureSubEntity hitbox : interactionHitboxIdMap) {
            if (hitbox.entityIds().contains(interactionId)) return hitbox.baseEntity();
        }
        return null;
    }

    @Nullable
    default Entity baseEntityFromHitbox(BlockPosition barrierPosition) {
        for (Map.Entry<UUID, Set<BlockPosition>> entry : barrierHitboxPositionMap.entrySet()) {
            if (entry.getValue().contains(barrierPosition)) return Bukkit.getEntity(entry.getKey());
        }
        return null;
    }

    void sendInteractionEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player);
    void removeInteractionHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic);
    void removeInteractionHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player);

    void sendBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player);
    void removeBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic);
    void removeBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player);

    default void removeAllHitboxes() {
        for (World world : Bukkit.getWorlds()) for (Entity entity : world.getEntities()) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
            if (mechanic == null) continue;
            removeInteractionHitboxPacket(entity, mechanic);
            removeBarrierHitboxPacket(entity, mechanic);
        }
    }
}
