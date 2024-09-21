package io.th0rgal.oraxen.nms.v1_20_R4.furniture;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.math.Position;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.*;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox.InteractionHitbox;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class FurniturePacketManager implements IFurniturePacketManager {

    public FurniturePacketManager() {
        if (VersionUtil.isPaperServer()) {
            MechanicsManager.registerListeners(OraxenPlugin.get(), "furniture", new FurniturePacketListener());

            if (PluginUtils.isEnabled("AxiomPaper"))
                MechanicsManager.registerListeners(OraxenPlugin.get(), "furniture", new AxiomCompatibility());

        } else {
            Logs.logWarning("Seems that your server is a Spigot-server");
            Logs.logWarning("FurnitureHitboxes will not work due to it relying on Paper-only events");
            Logs.logWarning("It is heavily recommended to make the upgrade to Paper");
        }

        ChannelInitializeListenerHolder.removeListener(FURNITURE_PACKET_LISTENER);
        ChannelInitializeListenerHolder.addListener(FURNITURE_PACKET_LISTENER, channel ->
                channel.pipeline().addBefore("packet_handler", FURNITURE_PACKET_LISTENER.toString(), new ChannelDuplexHandler() {
                    private final Connection connection = (Connection) channel.pipeline().get("packet_handler");

                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {

                        if (msg instanceof Packet<?> packet) handlePacket(packet, connection);
                        ctx.write(msg, promise);
                    }
                })
        );
    }

    private final int INTERACTION_WIDTH_ID = 8;
    private final int INTERACTION_HEIGHT_ID = 9;
    private final Map<UUID, Set<FurnitureInteractionHitboxPacket>> interactionHitboxPacketMap = new HashMap<>();

    private Packet<?> handlePacket(Packet<?> packet, Connection connection) {
        return switch (packet) {
            case ClientboundBundlePacket bundlePacket -> {
                List<Packet<? super ClientGamePacketListener>> newBundle = new ArrayList<>();
                bundlePacket.subPackets().forEach(bundle -> newBundle.add((Packet<? super ClientGamePacketListener>) handlePacket(bundle, connection)));
                yield new ClientboundBundlePacket(newBundle);
            }
            case ClientboundAddEntityPacket entityPacket -> {
                Player player = connection.getPlayer().getBukkitEntity();

                boolean result = furnitureBaseMap.stream().filter(base -> base.baseUUID() == entityPacket.getUUID()).findFirst().map(baseEntity -> {
                    Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
                        ItemDisplay furnitureEntity = baseEntity.baseEntity();
                        FurnitureMechanic mechanic = baseEntity.mechanic();
                        if (furnitureEntity != null && mechanic != null) Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
                            sendFurnitureEntityPacket(furnitureEntity, mechanic, player);
                            sendInteractionEntityPacket(furnitureEntity, mechanic, player);
                            sendBarrierHitboxPacket(furnitureEntity, mechanic, player);
                            sendLightMechanicPacket(furnitureEntity, mechanic, player);
                        });
                    });

                    return true;
                }).orElse(false);

                yield result ? null : packet;
            }
            case ClientboundRemoveEntitiesPacket entitiesPacket -> {
                Player player = connection.getPlayer().getBukkitEntity();
                entitiesPacket.getEntityIds().intStream().filter(i -> furnitureBaseMap.stream().anyMatch(p -> p.baseId() == i)).forEach(id -> {
                    interactionHitboxIdMap.stream()
                            .filter(s -> s.baseId() == id).findFirst()
                            .map(subEntity -> new ClientboundRemoveEntitiesPacket(subEntity.entityIds()))
                            .ifPresent(connection::send);
                    Optional.ofNullable(barrierHitboxPositionMap.get(id))
                            .map(pos -> pos.stream().collect(Collectors.toMap(p -> p.toLocation(player.getWorld()), l -> AIR_DATA)))
                            .ifPresent(player::sendMultiBlockChange);
                    Optional.ofNullable(lightMechanicPositionMap.get(id))
                            .map(pos -> pos.stream().collect(Collectors.toMap(p -> p.toLocation(player.getWorld()), l -> AIR_DATA)))
                            .ifPresent(player::sendMultiBlockChange);
                });

                yield packet;
            }
            case null, default -> packet;
        };
    }

    @Override
    public int nextEntityId() {
        return net.minecraft.world.entity.Entity.nextEntityId();
    }

    @Override
    public Entity getEntity(int entityId) {
        Entity entity = null;
        for (ServerLevel level : Bukkit.getWorlds().stream().map(w -> ((CraftWorld) w).getHandle()).toList()) {
            net.minecraft.world.entity.Entity nmsEntity = level.getEntity(entityId);
            if (nmsEntity == null) continue;
            entity = nmsEntity.getBukkitEntity();
            break;
        }

        return entity;
    }

    @Override
    public void sendFurnitureEntityPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        if (baseEntity.isDead()) return;
        if (mechanic.isModelEngine() && ModelEngineAPI.getBlueprint(mechanic.getModelEngineID()) != null) return;

        FurnitureBaseEntity furnitureBase = furnitureBaseFromBaseEntity(baseEntity).orElseGet(() -> {
            FurnitureBaseEntity base = new FurnitureBaseEntity(baseEntity, mechanic);
            furnitureBaseMap.add(base);
            return base;
        });

        FurnitureBasePacket basePacket = new FurnitureBasePacket(furnitureBase, baseEntity, player);
        ((CraftPlayer) player).getHandle().connection.send(basePacket.bundlePacket());
    }

    @Override
    public void removeFurnitureEntityPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic) {
        for (Player player : Bukkit.getOnlinePlayers())
            removeFurnitureEntityPacket(baseEntity, mechanic, player);
    }

    @Override
    public void removeFurnitureEntityPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        furnitureBaseMap.stream()
                .filter(f -> f.baseUUID() == baseEntity.getUniqueId())
                .map(base -> new ClientboundRemoveEntitiesPacket(base.baseId()))
                .findFirst().ifPresent(connection::send);
    }

    @Override
    public void sendInteractionEntityPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        List<InteractionHitbox> interactionHitboxes = mechanic.hitbox().interactionHitboxes();
        if (interactionHitboxes.isEmpty() || baseEntity.isDead()) return;
        if (mechanic.isModelEngine()) {
            ModelBlueprint blueprint = ModelEngineAPI.getBlueprint(mechanic.getModelEngineID());
            if (blueprint != null && blueprint.getMainHitbox() != null) return;
        }

        Location baseLoc = baseEntity.getLocation();
        interactionHitboxPacketMap.computeIfAbsent(baseEntity.getUniqueId(), key -> {
            List<Integer> entityIds = interactionHitboxIdMap.stream()
                    .filter(subEntity -> subEntity.baseUUID().equals(baseEntity.getUniqueId()))
                    .findFirst()
                    .map(FurnitureSubEntity::entityIds)
                    .orElseGet(() -> {
                        List<Integer> newEntityIds = new ArrayList<>(interactionHitboxes.size());
                        while (newEntityIds.size() < interactionHitboxes.size())
                            newEntityIds.add(net.minecraft.world.entity.Entity.nextEntityId());

                        FurnitureSubEntity subEntity = new FurnitureSubEntity(baseEntity, newEntityIds);
                        interactionHitboxIdMap.add(subEntity);
                        return subEntity.entityIds();
                    });

            Set<FurnitureInteractionHitboxPacket> packets = new HashSet<>();
            for (int i = 0; i < interactionHitboxes.size(); i++) {
                InteractionHitbox hitbox = interactionHitboxes.get(i);
                int entityId = entityIds.get(i);

                // Furniture is spawned at the center of a block, so offset hitbox down half a block
                Location loc = BlockHelpers.toCenterBlockLocation(baseLoc).add(hitbox.offset(baseEntity.getYaw()));
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
        }).forEach(packets -> ((CraftPlayer) player).getHandle().connection.send(packets.bundlePackets()));

    }

    @Override
    public void removeInteractionHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic) {
        Optional<FurnitureSubEntity> subEntity = interactionHitboxIdMap.stream().filter(s -> s.baseUUID().equals(baseEntity.getUniqueId())).findFirst();
        for (Player player : baseEntity.getWorld().getPlayers()) {
            subEntity.ifPresent(furnitureSubEntity -> ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(furnitureSubEntity.entityIds())));
        }
        //subEntity.ifPresent(interactionHitboxIdMap::remove);
        interactionHitboxPacketMap.remove(baseEntity.getUniqueId());
    }

    @Override
    public void removeInteractionHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        interactionHitboxIdMap.stream().filter(s -> s.baseUUID().equals(baseEntity.getUniqueId())).findFirst().ifPresent(subEntity ->
                ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(subEntity.entityIds()))
        );
    }



    @Override
    public void sendBarrierHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        if (baseEntity.isDead()) return;

        Map<Position, BlockData> positions = mechanic.hitbox().barrierHitboxes().stream()
                .map(c -> c.groundRotate(baseEntity.getYaw()).add(baseEntity.getLocation()))
                .distinct().collect(Collectors.toMap(Position::block, l -> BARRIER_DATA));
        player.sendMultiBlockChange(positions);

        for (Position position : positions.keySet().stream().toList()) {
            barrierHitboxPositionMap.compute(baseEntity.getEntityId(), (d, blockPos) -> {
                Set<BlockLocation> newBlockPos = new HashSet<>();
                newBlockPos.add(new BlockLocation(position.blockX(), position.blockY(), position.blockZ()));
                if (blockPos != null) newBlockPos.addAll(blockPos);
                return newBlockPos;
            });
        }
    }

    @Override
    public void removeBarrierHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic) {
        for (Player player : baseEntity.getWorld().getPlayers()) {
            removeBarrierHitboxPacket(baseEntity, mechanic, player);
        }
        barrierHitboxPositionMap.remove(baseEntity.getUniqueId());
    }

    @Override
    public void removeBarrierHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        Map<Position, BlockData> positions = mechanic.hitbox().barrierHitboxes().stream()
                .map(c -> c.groundRotate(baseEntity.getYaw()).add(baseEntity.getLocation())).collect(Collectors.toSet())
                .stream().collect(Collectors.toMap(Position::block, l -> AIR_DATA));
        player.sendMultiBlockChange(positions);
    }



    private record LightPosition(Light lightData, Location lightLocation) {}

    @Override
    public void sendLightMechanicPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        if (baseEntity.isDead()) return;

        Map<Position, BlockData> positions = mechanic.light().lightBlocks().stream()
                .map(l -> new LightPosition(l.lightData(), l.groundRotate(baseEntity.getYaw()).add(baseEntity.getLocation())))
                .distinct().collect(Collectors.toMap(l -> Position.block(l.lightLocation), l -> l.lightData));

        for (Position position : positions.keySet().stream().toList()) {
            lightMechanicPositionMap.compute(baseEntity.getEntityId(), (d, blockPos) -> {
                Set<BlockLocation> newBlockPos = new HashSet<>();
                newBlockPos.add(new BlockLocation(position.blockX(), position.blockY(), position.blockZ()));
                if (blockPos != null) newBlockPos.addAll(blockPos);
                return newBlockPos;
            });
        }

        positions.entrySet().removeIf(e -> !e.getKey().toBlock().toLocation(baseEntity.getWorld()).getBlock().isEmpty());
        player.sendMultiBlockChange(positions);
    }

    @Override
    public void removeLightMechanicPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic) {
        for (Player player : baseEntity.getWorld().getPlayers()) {
            removeLightMechanicPacket(baseEntity, mechanic, player);
        }
        lightMechanicPositionMap.remove(baseEntity.getUniqueId());
    }

    @Override
    public void removeLightMechanicPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        Map<Position, BlockData> positions = mechanic.light().lightBlocks().stream()
                .map(c -> c.groundRotate(baseEntity.getYaw()).add(baseEntity.getLocation())).collect(Collectors.toSet())
                .stream().collect(Collectors.toMap(Position::block, l -> AIR_DATA));
        player.sendMultiBlockChange(positions);
    }
}
