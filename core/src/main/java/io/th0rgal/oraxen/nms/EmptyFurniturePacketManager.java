package io.th0rgal.oraxen.nms;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EmptyFurniturePacketManager implements IFurniturePacketManager {
    @Override
    public int nextEntityId() {
        return -1;
    }

    @Override
    public void sendInteractionEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {

    }

    @Override
    public void removeInteractionHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic) {

    }

    @Override
    public void removeInteractionHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {

    }

    @Override
    public void sendBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {

    }

    @Override
    public void removeBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic) {

    }

    @Override
    public void removeBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {

    }
}
