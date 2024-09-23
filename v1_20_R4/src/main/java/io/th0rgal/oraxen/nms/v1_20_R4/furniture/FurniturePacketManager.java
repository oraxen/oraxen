package io.th0rgal.oraxen.nms.v1_20_R4.furniture;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.math.Position;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.*;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox.BarrierHitbox;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox.InteractionHitbox;
import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightBlock;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
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
                if (entityPacket.getData() != 0) yield packet; // Resent addEntity packet to correctly apply metadata

                Player player = connection.getPlayer().getBukkitEntity();
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
                    Entity entity = Bukkit.getEntity(entityPacket.getUUID());
                    FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
                    if (entity instanceof ItemDisplay baseEntity && entity.isValid() && mechanic != null) {
                        sendFurnitureEntityPacket(baseEntity, mechanic, player);
                        sendInteractionEntityPacket(baseEntity, mechanic, player);
                        sendBarrierHitboxPacket(baseEntity, mechanic, player);
                        sendLightMechanicPacket(baseEntity, mechanic, player);
                    }
                }, 2L);

                yield packet;
            }
            case ClientboundRemoveEntitiesPacket entitiesPacket -> {
                Player player = connection.getPlayer().getBukkitEntity();
                World world = player.getWorld();

                Set<Integer> furnitureBaseIds = furnitureBaseMap.stream().map(FurnitureBaseEntity::baseId).collect(Collectors.toSet());
                IntList hitboxEntities = new IntArrayList();
                Set<Location> hitboxBlockLocations = new HashSet<>();

                for (int id : entitiesPacket.getEntityIds()) {
                    if (!furnitureBaseIds.contains(id)) continue;

                    new HashSet<>(interactionHitboxIdMap).stream()
                            .filter(s -> s.baseId() == id).findFirst()
                            .ifPresent(sub -> hitboxEntities.addAll(sub.entityIds()));

                    Optional.ofNullable(barrierHitboxPositionMap.get(id))
                            .map(set -> set.stream().map(p -> p.toLocation(world)).collect(Collectors.toSet()))
                            .ifPresent(hitboxBlockLocations::addAll);

                    Optional.ofNullable(lightMechanicPositionMap.get(id))
                            .map(set -> set.stream().map(p -> p.toLocation(world)).collect(Collectors.toSet()))
                            .ifPresent(hitboxBlockLocations::addAll);
                }

                if (!hitboxEntities.isEmpty()) connection.send(new ClientboundRemoveEntitiesPacket(hitboxEntities));
                if (!hitboxBlockLocations.isEmpty()) player.sendMultiBlockChange(hitboxBlockLocations.stream().collect(Collectors.toMap(l -> l, l -> AIR_DATA)));

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
        if (!baseEntity.isValid()) return;
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
        furnitureBaseFromBaseEntity(baseEntity).ifPresent(base -> connection.send(new ClientboundRemoveEntitiesPacket(base.baseId())));
    }

    @Override
    public void sendInteractionEntityPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        List<InteractionHitbox> interactionHitboxes = mechanic.hitbox().interactionHitboxes();
        if (interactionHitboxes.isEmpty() || !baseEntity.isValid()) return;
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
            while (entityIds.size() < interactionHitboxes.size()) entityIds.add(nextEntityId());

            Set<FurnitureInteractionHitboxPacket> packets = new HashSet<>();
            for (int i = 0; i < interactionHitboxes.size(); i++) {
                InteractionHitbox hitbox = interactionHitboxes.get(i);
                int entityId = entityIds.get(i);

                // Furniture is spawned at the center of a block, so offset hitbox down half a block
                Location loc = BlockHelpers.toCenterBlockLocation(baseLoc).add(hitbox.offset(baseEntity.getYaw()));
                ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
                        entityId, UUID.randomUUID(),
                        loc.x(), loc.y(), loc.z(), loc.getPitch(), loc.getYaw(),
                        EntityType.INTERACTION, 1, Vec3.ZERO, 0.0
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
        if (!baseEntity.isValid()) return;

        int entityId = baseEntity.getEntityId();
        float yaw = baseEntity.getYaw();
        Location loc = baseEntity.getLocation();
        Map<Position, BlockData> barrierPositions = new HashMap<>();

        if (barrierHitboxPositionMap.containsKey(entityId))  {
            for (BlockLocation barrier : barrierHitboxPositionMap.get(entityId))
                barrierPositions.put(Position.block(barrier.getX(), barrier.getY(), barrier.getZ()), BARRIER_DATA);
        } else {
            Set<BarrierHitbox> newPositions = new HashSet<>();
            for (BarrierHitbox barrierHitbox : mechanic.hitbox().barrierHitboxes()) {
                Location location = barrierHitbox.groundRotate(yaw).add(loc);
                barrierPositions.put(Position.block(location.getBlockX(), location.getBlockY(), location.getBlockZ()), BARRIER_DATA);
                newPositions.add(new BarrierHitbox(location));
            }

            barrierHitboxPositionMap.put(entityId, newPositions);
        }

        player.sendMultiBlockChange(barrierPositions);
    }

    @Override
    public void removeBarrierHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic) {
        int entityId = baseEntity.getEntityId();
        Map<Position, BlockData> barrierPositions = new HashMap<>();

        if (barrierHitboxPositionMap.containsKey(entityId))  {
            for (BlockLocation barrier : barrierHitboxPositionMap.get(entityId))
                barrierPositions.put(Position.block(barrier.getX(), barrier.getY(), barrier.getZ()), AIR_DATA);
        } else for (Location location : mechanic.hitbox().barrierHitboxLocations(baseEntity.getLocation(), baseEntity.getYaw())) {
            barrierPositions.put(Position.block(location.getBlockX(), location.getBlockY(), location.getBlockZ()), AIR_DATA);
        }

        for (Player player : baseEntity.getWorld().getPlayers()) player.sendMultiBlockChange(barrierPositions);
        barrierHitboxPositionMap.remove(baseEntity.getEntityId());
    }

    @Override
    public void removeBarrierHitboxPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        int entityId = baseEntity.getEntityId();
        Map<Position, BlockData> barrierPositions = new HashMap<>();

        if (barrierHitboxPositionMap.containsKey(entityId))  {
            for (BlockLocation barrier : barrierHitboxPositionMap.get(entityId))
                barrierPositions.put(Position.block(barrier.getX(), barrier.getY(), barrier.getZ()), AIR_DATA);
        } else for (Location location : mechanic.hitbox().barrierHitboxLocations(baseEntity.getLocation(), baseEntity.getYaw())) {
            barrierPositions.put(Position.block(location.getBlockX(), location.getBlockY(), location.getBlockZ()), AIR_DATA);
        }

        player.sendMultiBlockChange(barrierPositions);
    }



    @Override
    public void sendLightMechanicPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        if (!baseEntity.isValid()) return;

        int entityId = baseEntity.getEntityId();
        Map<Position, BlockData> lightPositions = new HashMap<>();

        if (lightMechanicPositionMap.containsKey(entityId))  {
            for (LightBlock light : lightMechanicPositionMap.get(entityId))
                lightPositions.put(Position.block(light.getX(), light.getY(), light.getZ()), light.lightData());
        } else {
            Set<LightBlock> newPositions = new HashSet<>();
            for (LightBlock lightBlock : mechanic.light().lightBlocks()) {
                Location newLoc = lightBlock.groundRotate(baseEntity.getYaw()).add(baseEntity.getLocation());
                lightPositions.put(Position.block(lightBlock.getX(), lightBlock.getY(), lightBlock.getZ()), lightBlock.lightData());
                newPositions.add(new LightBlock(newLoc, lightBlock.lightData()));
            }

            lightMechanicPositionMap.put(entityId, newPositions);
        }

        player.sendMultiBlockChange(lightPositions);
    }

    @Override
    public void removeLightMechanicPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic) {
        int entityId = baseEntity.getEntityId();
        Map<Position, BlockData> lightPositions = new HashMap<>();

        if (lightMechanicPositionMap.containsKey(entityId))  {
            for (BlockLocation light : lightMechanicPositionMap.get(entityId))
                lightPositions.put(Position.block(light.getX(), light.getY(), light.getZ()), AIR_DATA);
        } else for (Location location : mechanic.light().lightBlockLocations(baseEntity.getLocation(), baseEntity.getYaw())) {
            lightPositions.put(Position.block(location.getBlockX(), location.getBlockY(), location.getBlockZ()), AIR_DATA);
        }

        for (Player player : baseEntity.getWorld().getPlayers()) player.sendMultiBlockChange(lightPositions);
        lightMechanicPositionMap.remove(baseEntity.getEntityId());
    }

    @Override
    public void removeLightMechanicPacket(@NotNull ItemDisplay baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        int entityId = baseEntity.getEntityId();
        Map<Position, BlockData> barrierPositions = new HashMap<>();

        if (lightMechanicPositionMap.containsKey(entityId))  {
            for (BlockLocation barrier : lightMechanicPositionMap.get(entityId))
                barrierPositions.put(Position.block(barrier.getX(), barrier.getY(), barrier.getZ()), AIR_DATA);
        } else for (Location location : mechanic.light().lightBlockLocations(baseEntity.getLocation(), baseEntity.getYaw())) {
            barrierPositions.put(Position.block(location.getBlockX(), location.getBlockY(), location.getBlockZ()), AIR_DATA);
        }

        player.sendMultiBlockChange(barrierPositions);
    }
}
