package io.th0rgal.oraxen.nms.handler.java21;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.th0rgal.oraxen.mechanics.provided.combat.trident.TridentMechanic;
import io.th0rgal.oraxen.mechanics.provided.combat.trident.TridentMechanicFactory;
import io.th0rgal.oraxen.nms.TridentPacketRotation;
import io.th0rgal.oraxen.nms.TridentTrackingInterval;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Display;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Changes the tracked projectile's client type, leaving server physics and pickup untouched. */
final class TridentPacketHandler implements Listener {
    private final Map<Integer, RenderData> tridents = new ConcurrentHashMap<>();

    TridentPacketHandler() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdd(EntityAddToWorldEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (tridents.containsKey(trident.getEntityId())) return;
        register(trident);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrack(PlayerTrackEntityEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        SchedulerUtil.runOnOwningThread(trident, () -> {
            if (!tridents.containsKey(trident.getEntityId())) register(trident);
            RenderData data = tridents.get(trident.getEntityId());
            if (data != null) updateTracking(trident, data);
        });
    }

    private void register(Trident trident) {
        TridentMechanicFactory factory = TridentMechanicFactory.get();
        TridentMechanic mechanic = factory == null ? null : factory.getMechanic(trident.getItemStack());
        if (mechanic == null) return;
        var item = trident.getItemStack().clone();
        var meta = item.getItemMeta();
        meta.setItemModel(mechanic.getThrownItemModel());
        item.setItemMeta(meta);
        // This temporary object is never added to a world or ticked. Its metadata is cached once.
        Display.ItemDisplay nativeDisplay = new Display.ItemDisplay(TridentNativeAccess.itemDisplay,
                ((CraftWorld) trident.getWorld()).getHandle());
        nativeDisplay.setItemStack(CraftItemStack.asNMSCopy(item));
        nativeDisplay.setId(trident.getEntityId());
        ItemDisplay display = (ItemDisplay) nativeDisplay.getBukkitEntity();
        display.setItemDisplayTransform(mechanic.getTransform());
        display.setTeleportDuration(1);
        display.setTransformation(new Transformation(new Vector3f(),
                new Quaternionf().rotateX((float) (Math.PI / 2.0)), new Vector3f(1.0F), new Quaternionf()));
        Location location = trident.getLocation();
        RenderData data = new RenderData(trident.getUniqueId(),
                List.copyOf(nativeDisplay.getEntityData().getNonDefaultValues()), mechanic, nativeDisplay,
                new SoundPosition(location.getX(), location.getY(), location.getZ(), System.nanoTime(), "item.trident.throw"));
        tridents.put(trident.getEntityId(), data);
        // EntityAddToWorldEvent can precede tracker creation. Retry on the entity's region
        // until it exists, then let the native tracker send movement every tick.
        if (!updateTracking(trident, data)) {
            data.trackingTask = SchedulerUtil.runForEntityTimer(trident, 1, 1,
                    () -> updateTracking(trident, data), () -> tridents.remove(trident.getEntityId(), data));
        }
    }

    private boolean updateTracking(Trident trident, RenderData data) {
        try {
            var trackedEntity = ((CraftEntity) trident).getHandle().moonrise$getTrackedEntity();
            if (trackedEntity == null) return false;
            TridentTrackingInterval.everyTick(trackedEntity.serverEntity);
        } catch (ReflectiveOperationException | LinkageError exception) {
            Logs.logWarning("Failed to update custom trident movement interval: " + exception.getMessage());
        }
        data.cancelTrackingTask();
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        RenderData data = tridents.get(event.getEntity().getEntityId());
        if (data == null) return;
        Location location = event.getEntity().getLocation();
        String sound = "item.trident.hit";
        if (event.getHitBlock() != null) {
            sound = "item.trident.hit_ground";
            var velocity = event.getEntity().getVelocity();
            if (velocity.lengthSquared() > 0) {
                var hit = event.getHitBlock().rayTrace(location, velocity, velocity.length(), FluidCollisionMode.NEVER);
                if (hit != null) location = hit.getHitPosition().toLocation(location.getWorld());
            }
        }
        data.soundPosition = new SoundPosition(location.getX(), location.getY(), location.getZ(), System.nanoTime(), sound);
    }

    @EventHandler
    public void onRemove(EntityRemoveFromWorldEvent event) {
        RenderData data = tridents.remove(event.getEntity().getEntityId());
        if (data != null) data.cancelTrackingTask();
    }

    void shutdown() {
        HandlerList.unregisterAll(this);
        tridents.values().forEach(RenderData::cancelTrackingTask);
        tridents.clear();
    }

    Session session() { return new Session(); }

    final class Session {
        // Accessed only by this connection's event loop. Sound matching stays within its tracked entities.
        private final Set<Integer> tracked = new HashSet<>();

        Object transform(Object packet) throws ReflectiveOperationException {
            if (tridents.isEmpty() && tracked.isEmpty()) return packet;
            if (packet instanceof ClientboundBundlePacket bundle) return transformBundle(bundle);
            if (packet instanceof ClientboundAddEntityPacket spawn) return transformSpawn(spawn);
            if (packet instanceof ClientboundSetEntityDataPacket metadata) return transformMetadata(metadata);
            if (packet instanceof ClientboundMoveEntityPacket move) return transformMove(move);
            if (packet instanceof ClientboundEntityPositionSyncPacket position) return transformPositionSync(position);
            if (packet instanceof ClientboundSoundEntityPacket sound) return transformEntitySound(sound);
            if (packet instanceof ClientboundSoundPacket sound) return transformSound(sound);
            if (packet instanceof ClientboundRemoveEntitiesPacket remove) return transformRemoval(remove);
            return packet;
        }

        @SuppressWarnings("unchecked")
        private Object transformBundle(ClientboundBundlePacket bundle) throws ReflectiveOperationException {
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            boolean changed = false;
            Set<Integer> previousTracked = new HashSet<>(tracked);
            try {
                for (var nested : bundle.subPackets()) {
                    Object converted = transform(nested);
                    changed |= converted != nested;
                    if (converted instanceof ClientboundBundlePacket expanded) {
                        expanded.subPackets().forEach(packets::add);
                    } else packets.add((Packet<? super ClientGamePacketListener>) converted);
                }
                return changed ? new ClientboundBundlePacket(packets) : bundle;
            } catch (ReflectiveOperationException | LinkageError exception) {
                tracked.clear();
                tracked.addAll(previousTracked);
                throw exception;
            }
        }

        private Object transformSpawn(ClientboundAddEntityPacket spawn) throws ReflectiveOperationException {
            RenderData data = tridents.get(spawn.getId());
            if (data == null || !data.uuid.equals(spawn.getUUID())) return spawn;
            var converted = new ClientboundAddEntityPacket(spawn.getId(), spawn.getUUID(),
                    spawn.getX(), spawn.getY(), spawn.getZ(), Math.clamp(-spawn.getXRot(), -90.0F, 90.0F),
                    -spawn.getYRot(), TridentNativeAccess.itemDisplay, 0, TridentNativeAccess.movement(spawn), -spawn.getYHeadRot());
            tracked.add(spawn.getId());
            return new ClientboundBundlePacket(List.of(converted,
                    new ClientboundSetEntityDataPacket(spawn.getId(), data.metadata)));
        }

        private Object transformMetadata(ClientboundSetEntityDataPacket metadata) {
            if (!tracked.contains(metadata.id())) return metadata;
            RenderData data = tridents.get(metadata.id());
            return data != null ? new ClientboundSetEntityDataPacket(metadata.id(), data.metadata) : metadata;
        }

        private Object transformMove(ClientboundMoveEntityPacket move) throws ReflectiveOperationException {
            if (!move.hasRotation() || tracked.isEmpty()) return move;
            int id = TridentPacketRotation.entityId(move);
            return tracked.contains(id)
                    ? TridentPacketRotation.move(move, id, TridentNativeAccess.yaw(move), TridentNativeAccess.pitch(move), move.isOnGround())
                    : move;
        }

        private Object transformPositionSync(ClientboundEntityPositionSyncPacket position) throws ReflectiveOperationException {
            return tracked.contains(position.id()) ? TridentPacketRotation.positionSync(position) : position;
        }

        private Object transformEntitySound(ClientboundSoundEntityPacket sound) throws ReflectiveOperationException {
            RenderData data = tridents.get(sound.getId());
            if (data == null) return sound;
            String replacement = data.mechanic.soundFor(TridentNativeAccess.soundPath(sound.getSound().value()));
            return replacement != null
                    ? new ClientboundSoundEntityPacket(sound(replacement), sound.getSource(), data.soundEntity,
                    sound.getVolume(), sound.getPitch(), sound.getSeed())
                    : sound;
        }

        private Object transformSound(ClientboundSoundPacket sound) throws ReflectiveOperationException {
            if (tracked.isEmpty()) return sound;
            String key = TridentNativeAccess.soundPath(sound.getSound().value());
            if (!key.startsWith("item.trident.")) return sound;
            for (int id : tracked) {
                RenderData data = tridents.get(id);
                if (data == null || !data.soundPosition.matches(key, sound.getX(), sound.getY(), sound.getZ())) continue;
                String replacement = data.mechanic.soundFor(key);
                if (replacement != null) {
                    return new ClientboundSoundPacket(sound(replacement), sound.getSource(),
                            sound.getX(), sound.getY(), sound.getZ(), sound.getVolume(), sound.getPitch(), sound.getSeed());
                }
            }
            return sound;
        }

        private Object transformRemoval(ClientboundRemoveEntitiesPacket remove) throws ReflectiveOperationException {
            if (!tracked.isEmpty()) {
                for (int id : TridentPacketRotation.removedEntities(remove)) tracked.remove(id);
            }
            return remove;
        }
    }

    private static Holder<SoundEvent> sound(String value) throws ReflectiveOperationException {
        return Holder.direct(TridentNativeAccess.sound(value));
    }

    private static final class RenderData {
        private final UUID uuid;
        private final List<SynchedEntityData.DataValue<?>> metadata;
        private final TridentMechanic mechanic;
        private final Display.ItemDisplay soundEntity;
        private volatile SoundPosition soundPosition;
        private volatile SchedulerUtil.ScheduledTask trackingTask;

        private void cancelTrackingTask() {
            SchedulerUtil.ScheduledTask task = trackingTask;
            if (task != null) task.cancel();
        }

        private RenderData(UUID uuid, List<SynchedEntityData.DataValue<?>> metadata,
                           TridentMechanic mechanic, Display.ItemDisplay soundEntity, SoundPosition soundPosition) {
            this.uuid = uuid;
            this.metadata = metadata;
            this.mechanic = mechanic;
            this.soundEntity = soundEntity;
            this.soundPosition = soundPosition;
        }
    }

    private record SoundPosition(double x, double y, double z, long created, String sound) {
        boolean matches(String key, double soundX, double soundY, double soundZ) {
            boolean expected = key.equals(sound) || (key.equals("item.trident.return") && !sound.equals("item.trident.throw"));
            return expected && System.nanoTime() - created < 2_000_000_000L
                    && Math.abs(x - soundX) < 0.25 && Math.abs(y - soundY) < 0.25 && Math.abs(z - soundZ) < 0.25;
        }
    }
}
