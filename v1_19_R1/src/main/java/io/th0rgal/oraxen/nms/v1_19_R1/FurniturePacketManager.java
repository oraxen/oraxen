package io.th0rgal.oraxen.nms.v1_19_R1;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FurniturePacketManager implements IFurniturePacketManager {

    public FurniturePacketManager() {
        if (VersionUtil.isPaperServer()) MechanicsManager.registerListeners(OraxenPlugin.get(), "furniture", new FurniturePacketListener());
        else {
            Logs.logWarning("Seems that your server is a Spigot-server");
            Logs.logWarning("FurnitureHitboxes will not work due to it relying on Paper-only events");
            Logs.logWarning("It is heavily recommended to make the upgrade to Paper");
        }
    }

    @Override public void sendInteractionEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {}
    @Override public void removeInteractionHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic) {}
    @Override public void removeInteractionHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {}

    @Override
    public void sendBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        Map<Location, BlockData> positions = mechanic.hitbox().barrierHitboxes().stream()
                .map(c -> c.groundRotate(baseEntity.getLocation().getYaw()).add(baseEntity.getLocation()))
                .collect(Collectors.toMap(l -> l, l -> BARRIER_DATA));
        player.sendMultiBlockChange(positions);

        for (Location location : positions.keySet()) {
            barrierHitboxPositionMap.compute(baseEntity.getUniqueId(), (d, blockPos) -> {
                Set<com.comphenix.protocol.wrappers.BlockPosition> newBlockPos = new HashSet<>();
                com.comphenix.protocol.wrappers.BlockPosition newPos = new com.comphenix.protocol.wrappers.BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                newBlockPos.add(newPos);
                if (blockPos != null) newBlockPos.addAll(blockPos);
                return newBlockPos;
            });
        }
    }

    @Override
    public void removeBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic) {
        for (Player player : baseEntity.getWorld().getPlayers()) {
            removeBarrierHitboxPacket(baseEntity, mechanic, player);
        }
        barrierHitboxPositionMap.remove(baseEntity.getUniqueId());
    }

    @Override
    public void removeBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        Map<Location, BlockData> positions = mechanic.hitbox().barrierHitboxes().stream()
                .map(c -> c.groundRotate(baseEntity.getLocation().getYaw()).add(baseEntity.getLocation()))
                .collect(Collectors.toMap(l -> l, l -> AIR_DATA));
        player.sendMultiBlockChange(positions);
    }
}
