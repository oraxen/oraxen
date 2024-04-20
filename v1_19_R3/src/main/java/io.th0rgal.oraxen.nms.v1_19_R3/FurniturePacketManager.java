package io.th0rgal.oraxen.nms.v1_19_R3;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.*;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox.InteractionHitbox;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
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

    private final int INTERACTION_WIDTH_ID = 8;
    private final int INTERACTION_HEIGHT_ID = 9;
    private final int ITEM_FRAME_ITEM_ID = 8;
    private final int ITEM_FRAME_ROTATION_ID = 9;
    private final int ITEM_DISPLAY_ITEM_ID = 23;
    private final int ITEM_DISPLAY_TRANSFORM_ID = 24;
    private final Map<UUID, Set<FurnitureInteractionHitboxPacket>> interactionHitboxPacketMap = new HashMap<>();
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

                EntityType<?> entityType = type == FurnitureType.DISPLAY_ENTITY ? EntityType.ITEM_DISPLAY : type == FurnitureType.ITEM_FRAME ? EntityType.ITEM_FRAME : EntityType.GLOW_ITEM_FRAME;
                ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
                        entityId, UUID.randomUUID(),
                        baseLoc.x(), baseLoc.y(), baseLoc.z(), baseLoc.getPitch(), baseLoc.getYaw(),
                        entityType, 0, Vec3.ZERO, 0.0
                );

                ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(
                        entityId, Arrays.asList(
                        new SynchedEntityData.DataValue<>(type == FurnitureType.DISPLAY_ENTITY ? ITEM_DISPLAY_ITEM_ID : ITEM_FRAME_ITEM_ID, EntityDataSerializers.ITEM_STACK, CraftItemStack.asNMSCopy(FurnitureHelpers.furnitureItem(baseEntity))),
                        new SynchedEntityData.DataValue<>(type == FurnitureType.DISPLAY_ENTITY ? ITEM_DISPLAY_TRANSFORM_ID : ITEM_FRAME_ROTATION_ID, EntityDataSerializers.INT, (type == FurnitureType.DISPLAY_ENTITY ? mechanic.displayEntityProperties().displayTransform() : FurnitureHelpers.yawToRotation(baseEntity.getLocation().getYaw())).ordinal())
                ));

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


    @Override
    public void sendInteractionEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        List<InteractionHitbox> interactionHitboxes = mechanic.hitbox().interactionHitboxes();
        if (interactionHitboxes.isEmpty()) return;
        if (mechanic.isModelEngine()) {
            ModelBlueprint blueprint = ModelEngineAPI.getBlueprint(mechanic.getModelEngineID());
            if (blueprint != null && blueprint.getMainHitbox() != null) return;
        }

        Location baseLoc = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        interactionHitboxPacketMap.computeIfAbsent(baseEntity.getUniqueId(), key -> {
            List<Integer> entityIds = interactionHitboxIdMap.stream()
                    .filter(ids -> ids.baseUUID().equals(baseEntity.getUniqueId()))
                    .findFirst()
                    .map(FurnitureSubEntity::entityIds)
                    .orElseGet(() -> {
                        List<Integer> newEntityIds = new ArrayList<>(interactionHitboxes.size());
                        while (newEntityIds.size() < interactionHitboxes.size())
                            newEntityIds.add(net.minecraft.world.entity.Entity.nextEntityId());

                        FurnitureSubEntity subEntity = new FurnitureSubEntity(baseEntity.getUniqueId(), newEntityIds);
                        interactionHitboxIdMap.add(subEntity);
                        return subEntity.entityIds();
                    });

            Set<FurnitureInteractionHitboxPacket> packets = new HashSet<>();
            for (int i = 0; i < interactionHitboxes.size(); i++) {
                InteractionHitbox hitbox = interactionHitboxes.get(i);
                int entityId = entityIds.get(i);

                Location loc = baseLoc.clone().add(hitbox.offset(baseEntity.getLocation().getYaw()));
                ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
                        entityId, UUID.randomUUID(),
                        loc.x(), loc.y(), loc.z(), loc.getPitch(), loc.getYaw(),
                        EntityType.INTERACTION, 0, Vec3.ZERO, 0.0
                );

                ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(
                        entityId, Arrays.asList(
                        new SynchedEntityData.DataValue<>(INTERACTION_WIDTH_ID, EntityDataSerializers.FLOAT, hitbox.width()),
                        new SynchedEntityData.DataValue<>(INTERACTION_HEIGHT_ID, EntityDataSerializers.FLOAT, hitbox.height())
                ));

                packets.add(new FurnitureInteractionHitboxPacket(entityId, addEntityPacket, metadataPacket));
            }
            return packets;
        }).forEach(packets -> {
            ((CraftPlayer) player).getHandle().connection.send(packets.addEntity);
            ((CraftPlayer) player).getHandle().connection.send(packets.metadata);
        });

    }

    @Override
    public void removeInteractionHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic) {
        for (Player player : baseEntity.getWorld().getPlayers()) {
            removeInteractionHitboxPacket(baseEntity, mechanic, player);
        }
        interactionHitboxIdMap.removeIf(id -> id.baseUUID().equals(baseEntity.getUniqueId()));
        interactionHitboxPacketMap.remove(baseEntity.getUniqueId());
    }

    @Override
    public void removeInteractionHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        IntList entityIds = interactionHitboxIdMap.stream().filter(id -> id.baseUUID().equals(baseEntity.getUniqueId())).findFirst().map(FurnitureSubEntity::entityIds).orElse(IntList.of());
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(entityIds.toIntArray()));
    }

    @Override
    public void sendBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        Map<Position, BlockData> positions = mechanic.hitbox().barrierHitboxes().stream()
                .map(c -> c.groundRotate(baseEntity.getLocation().getYaw()).add(baseEntity.getLocation()))
                .collect(Collectors.toMap(Position::block, l -> BARRIER_DATA));
        player.sendMultiBlockChange(positions);

        for (BlockPosition position : positions.keySet().stream().map(Position::toBlock).toList()) {
            barrierHitboxPositionMap.compute(baseEntity.getUniqueId(), (d, blockPos) -> {
                Set<com.comphenix.protocol.wrappers.BlockPosition> newBlockPos = new HashSet<>();
                com.comphenix.protocol.wrappers.BlockPosition newPos = new com.comphenix.protocol.wrappers.BlockPosition(position.blockX(), position.blockY(), position.blockZ());
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
        Map<Position, BlockData> positions = mechanic.hitbox().barrierHitboxes().stream()
                .map(c -> c.groundRotate(baseEntity.getLocation().getYaw()).add(baseEntity.getLocation()))
                .collect(Collectors.toMap(Position::block, l -> AIR_DATA));
        player.sendMultiBlockChange(positions);
    }
}
