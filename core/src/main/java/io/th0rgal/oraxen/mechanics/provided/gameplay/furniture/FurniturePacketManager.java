package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.comphenix.protocol.wrappers.BlockPosition;
import it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class FurniturePacketManager {

    Map<UUID, Set<BlockPosition>> barrierHitboxPositionMap = new HashMap<>();
    Map<UUID, IntList> interactionHitboxIdMap = new HashMap<>();

    @Nullable
    Entity baseEntityFromHitbox(int interactionId) {
        for (Map.Entry<UUID, IntList> entry : interactionHitboxIdMap.entrySet()) {
            if (entry.getValue().contains(interactionId)) return Bukkit.getEntity(entry.getKey());
        }
        return null;
    }

    @Nullable
    Entity baseEntityFromHitbox(BlockPosition barrierPosition) {
        for (Map.Entry<UUID, Set<BlockPosition>> entry : barrierHitboxPositionMap.entrySet()) {
            if (entry.getValue().contains(barrierPosition)) return Bukkit.getEntity(entry.getKey());
        }
        return null;
    }

    abstract void sendInteractionEntityPacket(Entity baseEntity, FurnitureMechanic mechanic, Player player);
    abstract void removeInteractionHitboxPacket(Entity baseEntity, FurnitureMechanic mechanic);
    abstract void removeInteractionHitboxPacket(Entity baseEntity, FurnitureMechanic mechanic, Player player);

    abstract void sendBarrierHitboxPacket(Entity baseEntity, FurnitureMechanic mechanic, Player player);
    abstract void removeBarrierHitboxPacket(Entity baseEntity, FurnitureMechanic mechanic);
    abstract void removeBarrierHitboxPacket(Entity baseEntity, FurnitureMechanic mechanic, Player player);
}
