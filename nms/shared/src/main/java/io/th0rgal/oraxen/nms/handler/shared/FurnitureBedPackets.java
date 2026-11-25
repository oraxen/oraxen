package io.th0rgal.oraxen.nms.handler.shared;

import io.th0rgal.oraxen.utils.SchedulerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class FurnitureBedPackets {

    private static final EntityDataAccessor<Pose> pose = accessor(Entity.class, "DATA_POSE", 6, EntityDataSerializers.POSE);
    private static final EntityDataAccessor<Boolean> noGravity = accessor(Entity.class, "DATA_NO_GRAVITY", 5, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> sleepingPos =
            accessor(LivingEntity.class, "SLEEPING_POS_ID", 14, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final int teleportId = Integer.MIN_VALUE;
    private static final Field moveEntityId = field(ClientboundMoveEntityPacket.class);
    private static final Field rotateHeadEntityId = field(ClientboundRotateHeadPacket.class);

    private record Bed(ServerPlayer sleeper, Vec3 head, float bodyYaw, BlockPos bedPos, BlockState bedState, BlockPos headPos) {

        private BlockState clientState(BlockPos pos, BlockState state) {
            if (pos.equals(bedPos)) return bedState;
            return pos.equals(headPos) && state.is(Blocks.BARRIER) ? Blocks.AIR.defaultBlockState() : state;
        }
    }

    private final Map<Integer, Bed> beds = new ConcurrentHashMap<>();

    boolean isEmpty() {
        return beds.isEmpty();
    }

    void clear() {
        beds.clear();
    }

    void forget(int playerEntityId) {
        beds.remove(playerEntityId);
    }

    boolean show(Player player, Location head, float feetYaw) {
        ServerPlayer sleeper = ((CraftPlayer) player).getHandle();
        World world = player.getWorld();
        int top = world.getMaxHeight() - 1;
        int bedY = head.getBlockY() - world.getMinHeight() > top - head.getBlockY() ? world.getMinHeight() : top;
        BlockPos bedPos = new BlockPos(head.getBlockX(), bedY, head.getBlockZ());
        org.bukkit.block.data.type.Bed bedData = (org.bukkit.block.data.type.Bed) Material.RED_BED.createBlockData();
        bedData.setFacing(BlockFace.valueOf(Direction.fromYRot(feetYaw).getOpposite().name()));
        bedData.setPart(org.bukkit.block.data.type.Bed.Part.HEAD);
        BlockState bedState = ((CraftBlockData) bedData).getState();
        Vec3 headPosition = new Vec3(head.getX(), head.getY(), head.getZ());
        BlockPos headPos = BlockPos.containing(headPosition);
        Bed bed = new Bed(sleeper, headPosition, Mth.wrapDegrees(-90f - feetYaw), bedPos, bedState, headPos);
        beds.put(sleeper.getId(), bed);
        sleeper.connection.send(bundle(new ClientboundBlockUpdatePacket(bedPos, bedState),
                new ClientboundBlockUpdatePacket(headPos, bed.clientState(headPos, sleeper.level().getBlockState(headPos))),
                new ClientboundSetEntityDataPacket(sleeper.getId(), List.of(
                        SynchedEntityData.DataValue.create(pose, Pose.SLEEPING),
                        SynchedEntityData.DataValue.create(noGravity, true),
                        SynchedEntityData.DataValue.create(sleepingPos, Optional.of(bedPos)))),
                new ClientboundPlayerPositionPacket(teleportId,
                        new PositionMoveRotation(headPosition, Vec3.ZERO, 0f, 0f), Relative.ROTATION)));
        respawnForViewers(sleeper);
        return true;
    }

    void hide(Player player) {
        ServerPlayer sleeper = ((CraftPlayer) player).getHandle();
        Bed bed = beds.remove(sleeper.getId());
        if (bed == null) return;
        sleeper.connection.send(bundle(new ClientboundBlockUpdatePacket(sleeper.level(), bed.bedPos()),
                new ClientboundBlockUpdatePacket(sleeper.level(), bed.headPos()),
                new ClientboundSetEntityDataPacket(sleeper.getId(), List.of(
                        SynchedEntityData.DataValue.create(pose, sleeper.getPose()),
                        SynchedEntityData.DataValue.create(noGravity, sleeper.isNoGravity()),
                        SynchedEntityData.DataValue.create(sleepingPos, sleeper.getSleepingPos())))));
        respawnForViewers(sleeper);
    }

    private static void respawnForViewers(ServerPlayer sleeper) {
        ChunkMap.TrackedEntity tracked = sleeper.moonrise$getTrackedEntity();
        if (tracked == null) return;
        for (ServerPlayerConnection connection : List.copyOf(tracked.seenBy)) {
            ServerPlayer viewer = connection.getPlayer();
            if (viewer == sleeper) continue;
            tracked.removePlayer(viewer);
            tracked.updatePlayer(viewer);
        }
    }

    void onIncoming(Object packet, ServerPlayer sender) {
        if (!(packet instanceof ServerboundPlayerCommandPacket command) || sender == null
                || command.getAction() != ServerboundPlayerCommandPacket.Action.STOP_SLEEPING
                || !beds.containsKey(sender.getId())) return;
        org.bukkit.entity.Player player = sender.getBukkitEntity();
        SchedulerUtil.runForEntity(player, player::leaveVehicle, null);
    }

    Object transform(Object packet, ServerPlayer receiver) throws IllegalAccessException {
        if (packet instanceof ClientboundSetEntityDataPacket data) return transformData(data, receiver);
        if (packet instanceof ClientboundBlockUpdatePacket update) {
            Bed bed = beds.get(receiver.getId());
            if (bed == null) return packet;
            BlockState state = bed.clientState(update.getPos(), update.getBlockState());
            return state == update.getBlockState() ? packet : new ClientboundBlockUpdatePacket(update.getPos(), state);
        }
        if (packet instanceof ClientboundAddEntityPacket spawn) return transformSpawn(spawn, receiver);
        if (packet instanceof ClientboundMoveEntityPacket move && moveEntityId != null) {
            Bed bed = viewedBed(moveEntityId.getInt(move), receiver);
            if (bed != null) return new ClientboundMoveEntityPacket.Rot(moveEntityId.getInt(move),
                    Mth.packDegrees(bed.bodyYaw()), (byte) 0, move.isOnGround());
        }
        if (packet instanceof ClientboundRotateHeadPacket rotateHead && rotateHeadEntityId != null) {
            Bed bed = viewedBed(rotateHeadEntityId.getInt(rotateHead), receiver);
            if (bed != null) return new ClientboundRotateHeadPacket(bed.sleeper(), Mth.packDegrees(bed.bodyYaw()));
        }
        if (packet instanceof ClientboundEntityPositionSyncPacket sync) {
            Bed bed = viewedBed(sync.id(), receiver);
            if (bed != null) return NmsAccessors.positionSync(sync.id(), position(bed), sync.onGround());
        }
        if (packet instanceof ClientboundTeleportEntityPacket teleport) {
            Bed bed = viewedBed(teleport.id(), receiver);
            if (bed != null) return new ClientboundTeleportEntityPacket(teleport.id(), position(bed), Set.of(), teleport.onGround());
        }
        return packet;
    }

    private Bed viewedBed(int entityId, ServerPlayer receiver) {
        Bed bed = beds.get(entityId);
        return bed == null || bed.sleeper() == receiver ? null : bed;
    }

    private static PositionMoveRotation position(Bed bed) {
        return new PositionMoveRotation(bed.head(), Vec3.ZERO, bed.bodyYaw(), 0f);
    }

    private Object transformData(ClientboundSetEntityDataPacket packet, ServerPlayer receiver) {
        Bed bed = beds.get(packet.id());
        if (bed == null) return packet;
        boolean self = bed.sleeper() == receiver;
        List<SynchedEntityData.DataValue<?>> values = new ArrayList<>(packet.packedItems().size());
        boolean changed = false;
        for (SynchedEntityData.DataValue<?> value : packet.packedItems()) {
            if (value.id() == pose.id()) {
                values.add(SynchedEntityData.DataValue.create(pose, Pose.SLEEPING));
                changed = true;
            } else if (self && value.id() == noGravity.id()) {
                values.add(SynchedEntityData.DataValue.create(noGravity, true));
                changed = true;
            } else if (value.id() == sleepingPos.id()) {
                values.add(SynchedEntityData.DataValue.create(sleepingPos, self ? Optional.of(bed.bedPos()) : Optional.empty()));
                changed = true;
            } else values.add(value);
        }
        return changed ? new ClientboundSetEntityDataPacket(packet.id(), values) : packet;
    }

    private Object transformSpawn(ClientboundAddEntityPacket spawn, ServerPlayer receiver) {
        Bed bed = viewedBed(spawn.getId(), receiver);
        if (bed == null) return spawn;
        Vec3 head = bed.head();
        return bundle(new ClientboundAddEntityPacket(spawn.getId(), spawn.getUUID(), head.x, head.y, head.z, 0f,
                        bed.bodyYaw(), spawn.getType(), spawn.getData(), Vec3.ZERO, bed.bodyYaw()),
                new ClientboundSetEntityDataPacket(spawn.getId(), List.of(SynchedEntityData.DataValue.create(pose, Pose.SLEEPING))));
    }

    @SafeVarargs
    private static ClientboundBundlePacket bundle(Packet<? super ClientGamePacketListener>... packets) {
        return new ClientboundBundlePacket(List.of(packets));
    }

    @SuppressWarnings("unchecked")
    private static <T> EntityDataAccessor<T> accessor(Class<?> owner, String name, int fallbackId, EntityDataSerializer<T> serializer) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return (EntityDataAccessor<T>) field.get(null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return new EntityDataAccessor<>(fallbackId, serializer);
        }
    }

    private static Field field(Class<?> owner) {
        try {
            Field field = owner.getDeclaredField("entityId");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
