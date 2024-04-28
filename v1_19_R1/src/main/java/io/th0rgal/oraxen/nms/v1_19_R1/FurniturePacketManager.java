package io.th0rgal.oraxen.nms.v1_19_R1;

import com.ticxo.modelengine.api.ModelEngineAPI;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureBaseEntity;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureType;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
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

    @Override
    public void sendFurnitureEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        if (baseEntity.isDead()) return;
        if (mechanic.isModelEngine() && ModelEngineAPI.getBlueprint(mechanic.getModelEngineID()) != null) return;

        FurnitureBaseEntity furnitureBase = furnitureBaseFromBaseEntity(baseEntity).orElseGet(() -> {
            FurnitureBaseEntity base = new FurnitureBaseEntity(baseEntity, mechanic);
            furnitureBaseMap.add(base);
            return base;
        });

        FurnitureType type = mechanic.furnitureType(player);
        FurnitureBasePacket basePacket = new FurnitureBasePacket(furnitureBase, baseEntity, type, player);
        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        connection.send(basePacket.entityPacket());
        connection.send(basePacket.metadataPacket());
    }

    @Override
    public void removeFurnitureEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic) {
        for (Player player : Bukkit.getOnlinePlayers())
            removeFurnitureEntityPacket(baseEntity, mechanic, player);

    }

    @Override
    public void removeFurnitureEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        furnitureBaseMap.stream().filter(f -> f.baseUUID().equals(baseEntity.getUniqueId())).findFirst().ifPresent(base ->
                ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(base.entityId()))
        );
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
