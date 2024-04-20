package io.th0rgal.oraxen.nms.v1_19_R1;

import com.ticxo.modelengine.api.ModelEngineAPI;
import io.netty.buffer.Unpooled;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.*;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
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

    private final int ITEM_FRAME_ITEM_ID = 8;
    private final int ITEM_FRAME_ROTATION_ID = 9;
    private final Map<UUID, Set<FurnitureBasePacket>> furnitureBasePacketMap = new HashMap<>();

    @Override
    public void sendFurnitureEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        if (baseEntity.isDead()) return;
        if (mechanic.isModelEngine() && ModelEngineAPI.getBlueprint(mechanic.getModelEngineID()) != null) return;

        Location baseLoc = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        furnitureBasePacketMap.computeIfAbsent(baseEntity.getUniqueId(), key -> {
            FurnitureBaseEntity furnitureBase = furnitureFromBaseEntity(baseEntity).orElseGet(() -> {
                for (FurnitureType type : FurnitureType.values()) {
                    furnitureEntityMap.add(new FurnitureBaseEntity(baseEntity.getUniqueId(), type, net.minecraft.world.entity.Entity.nextEntityId()));
                }
                return furnitureFromBaseEntity(baseEntity).orElse(null);
            });
            if (furnitureBase == null) return new HashSet<>();

            Set<FurnitureBasePacket> packets = new HashSet<>();
            for (FurnitureType type : FurnitureType.values()) {
                int entityId = furnitureBase.entityId(type);

                EntityType<?> entityType = type == FurnitureType.ITEM_FRAME ? EntityType.ITEM_FRAME : EntityType.GLOW_ITEM_FRAME;
                ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
                        entityId, UUID.randomUUID(),
                        baseLoc.getX(), baseLoc.getY(), baseLoc.getZ(), baseLoc.getPitch(), baseLoc.getYaw(),
                        entityType, 0, Vec3.ZERO, 0.0
                );

                FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
                friendlyByteBuf.writeVarInt(entityId);
                SynchedEntityData.pack(Arrays.asList(
                        new SynchedEntityData.DataItem<>(new EntityDataAccessor<>(ITEM_FRAME_ITEM_ID, EntityDataSerializers.ITEM_STACK), CraftItemStack.asNMSCopy(FurnitureHelpers.furnitureItem(baseEntity))),
                        new SynchedEntityData.DataItem<>(new EntityDataAccessor<>(ITEM_FRAME_ROTATION_ID, EntityDataSerializers.INT), FurnitureHelpers.yawToRotation(baseEntity.getLocation().getYaw()).ordinal())
                ), new FriendlyByteBuf(Unpooled.buffer()));

                ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(friendlyByteBuf);

                packets.add(new FurnitureBasePacket(type, entityId, addEntityPacket, metadataPacket));
            }

            return packets;
        }).forEach(packets -> {
            ((CraftPlayer) player).getHandle().connection.send(packets.addEntity);
            ((CraftPlayer) player).getHandle().connection.send(packets.metadata);
        });
    }

    @Override
    public void removeFurnitureEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic) {
        for (Player player : Bukkit.getOnlinePlayers())
            removeFurnitureEntityPacket(baseEntity, mechanic, player);

    }

    @Override
    public void removeFurnitureEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        furnitureEntityMap.stream().filter(f -> f.baseUUID().equals(baseEntity.getUniqueId())).findFirst().ifPresent(base ->
                ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(base.entityIds().toIntArray()))
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
