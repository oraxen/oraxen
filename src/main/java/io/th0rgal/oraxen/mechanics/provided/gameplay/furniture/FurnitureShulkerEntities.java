package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.papermc.paper.entity.TeleportFlag;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EntityUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.wrappers.AttributeWrapper;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.*;

final class FurnitureShulkerEntities implements Listener {

    private static final NamespacedKey SHULKER_OFFSET_KEY = new NamespacedKey(OraxenPlugin.get(), "shulker_offset");
    private static final NamespacedKey SHULKER_PEEK_KEY = new NamespacedKey(OraxenPlugin.get(), "shulker_peek");
    private static final NamespacedKey SHULKER_DIRECTION_KEY = new NamespacedKey(OraxenPlugin.get(), "shulker_direction");
    private static final NamespacedKey SHULKER_ROOT_KEY = new NamespacedKey(OraxenPlugin.get(), "shulker_root");
    private static final NamespacedKey SHULKER_YAW_KEY = new NamespacedKey(OraxenPlugin.get(), "shulker_yaw");
    private static final NamespacedKey SHULKER_VEHICLE_KEY = new NamespacedKey(OraxenPlugin.get(), "shulker_vehicle");

    static boolean isHitbox(Entity entity) {
        return entity.getPersistentDataContainer().has(SHULKER_OFFSET_KEY);
    }

    private static boolean isVehicle(Entity entity) {
        return entity.getPersistentDataContainer().has(SHULKER_VEHICLE_KEY);
    }

    static void spawn(Entity base, Location root, float yaw, String itemId, List<FurnitureShulkerHitbox> hitboxes) {
        PersistentDataContainer data = base.getPersistentDataContainer();
        if (data.has(SHULKERS_KEY)) return;
        data.set(SHULKERS_KEY, DataType.asList(DataType.UUID), List.of());
        data.set(SHULKER_YAW_KEY, PersistentDataType.FLOAT, yaw);

        for (FurnitureShulkerHitbox hitbox : hitboxes) {
            Location location = hitboxLocation(root, hitbox.offset(), yaw);
            if (Bukkit.isOwnedByCurrentRegion(location)) {
                Shulker shulker = spawnHitbox(base.getUniqueId(), root, location, yaw, itemId, hitbox);
                if (shulker != null) addLink(base, shulker);
            } else {
                UUID baseId = base.getUniqueId();
                SchedulerUtil.runAtLocation(location, () -> {
                    Shulker shulker = spawnHitbox(baseId, root, location, yaw, itemId, hitbox);
                    if (shulker == null) return;
                    Runnable discard = () -> SchedulerUtil.runForEntity(shulker, () -> discard(shulker));
                    if (SchedulerUtil.runForEntity(base, () -> {
                        if (!base.isValid() || !base.getPersistentDataContainer().has(SHULKERS_KEY)) {
                            discard.run();
                            return;
                        }
                        addLink(base, shulker);
                        reposition(shulker, base.getLocation(), shulkerYaw(base));
                    }, discard) == null) discard.run();
                });
            }
        }
    }

    private static Shulker spawnHitbox(UUID baseId, Location root, Location location, float yaw, String itemId,
                                       FurnitureShulkerHitbox hitbox) {
        Shulker shulker = EntityUtils.spawnEntity(location, Shulker.class, entity -> {
            entity.setInvisible(!Settings.DEBUG.toBool());
            entity.setSilent(true);
            entity.setAI(false);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
            AttributeInstance scale = entity.getAttribute(AttributeWrapper.SCALE);
            if (scale != null) scale.setBaseValue(hitbox.scale());
            PersistentDataContainer data = entity.getPersistentDataContainer();
            data.set(FURNITURE_KEY, PersistentDataType.STRING, itemId);
            data.set(BASE_ENTITY_KEY, DataType.UUID, baseId);
            data.set(SHULKER_OFFSET_KEY, DataType.asList(PersistentDataType.DOUBLE), List.of(hitbox.x(), hitbox.y(), hitbox.z()));
            data.set(SHULKER_PEEK_KEY, PersistentDataType.INTEGER, hitbox.peek());
            data.set(SHULKER_DIRECTION_KEY, PersistentDataType.STRING, hitbox.direction().name());
            data.set(SHULKER_ROOT_KEY, DataType.LOCATION, root);
            NMSHandlers.getHandler().trackFurnitureHitbox(entity);
        });
        if (shulker == null || !shulker.isValid()) return null;
        if (!activate(shulker, location, yaw)) {
            discard(shulker);
            return null;
        }
        return shulker;
    }

    // A shulker that doesn't ride anything snaps to the block grid and teleports when it can't attach to a block.
    private static boolean activate(Shulker shulker, Location location, float yaw) {
        NMSHandlers.getHandler().trackFurnitureHitbox(shulker);
        boolean invisible = !Settings.DEBUG.toBool();
        if (shulker.isInvisible() != invisible) shulker.setInvisible(invisible);
        keepInPeaceful(shulker);
        if (!(shulker.getVehicle() instanceof ItemDisplay vehicle) || !isVehicle(vehicle)) {
            if (shulker.getVehicle() != null) shulker.leaveVehicle();
            ItemDisplay vehicle = EntityUtils.spawnEntity(location, ItemDisplay.class, entity -> {
                entity.setPersistent(true);
                entity.getPersistentDataContainer().set(SHULKER_VEHICLE_KEY, PersistentDataType.BOOLEAN, true);
            });
            if (vehicle == null) return false;
            if (!vehicle.addPassenger(shulker)) {
                vehicle.remove();
                return false;
            }
        }
        // Mounting resets the attached face, so apply it after the shulker rides its vehicle.
        shulker.setAttachedFace(FurnitureShulkerHitbox.attachedFace(direction(shulker), yaw));
        applyPeek(shulker);
        // Moving into another block closes a shulker, which happens when it first follows its vehicle.
        SchedulerUtil.runForEntityLater(shulker, 2L, () -> applyPeek(shulker));
        return true;
    }

    private static void applyPeek(Shulker shulker) {
        int peek = shulker.getPersistentDataContainer().getOrDefault(SHULKER_PEEK_KEY, PersistentDataType.INTEGER, 0);
        if (Math.round(shulker.getPeek() * 100) != peek) shulker.setPeek(peek / 100f);
    }

    private static void keepInPeaceful(Shulker shulker) {
        try {
            shulker.setDespawnInPeacefulOverride(TriState.FALSE);
        } catch (NoSuchMethodError ignored) {
            // Minecraft 1.21.7 and older despawn hostile mobs in peaceful difficulty
        }
    }

    private static BlockFace direction(Entity shulker) {
        String direction = shulker.getPersistentDataContainer().get(SHULKER_DIRECTION_KEY, PersistentDataType.STRING);
        try {
            return direction == null ? BlockFace.UP : BlockFace.valueOf(direction);
        } catch (IllegalArgumentException ignored) {
            return BlockFace.UP;
        }
    }

    private static void discard(Entity shulker) {
        NMSHandlers.getHandler().untrackFurnitureHitbox(shulker);
        Entity vehicle = shulker.getVehicle();
        if (vehicle != null && isVehicle(vehicle)) vehicle.remove();
        shulker.remove();
    }

    private static void addLink(Entity base, Entity shulker) {
        List<UUID> links = new ArrayList<>(links(base));
        links.add(shulker.getUniqueId());
        base.getPersistentDataContainer().set(SHULKERS_KEY, DataType.asList(DataType.UUID), links);
    }

    private static List<UUID> links(Entity base) {
        return base.getPersistentDataContainer().getOrDefault(SHULKERS_KEY, DataType.asList(DataType.UUID), List.of());
    }

    static void remove(Entity base) {
        PersistentDataContainer data = base.getPersistentDataContainer();
        if (!data.has(SHULKERS_KEY)) return;
        for (UUID id : links(base)) {
            Entity shulker = Bukkit.getEntity(id);
            if (shulker != null) SchedulerUtil.runOnOwningThread(shulker, () -> discard(shulker));
        }
        data.remove(SHULKERS_KEY);
        data.remove(SHULKER_YAW_KEY);
    }

    static void rotate(Entity base, float yaw) {
        PersistentDataContainer data = base.getPersistentDataContainer();
        if (!data.has(SHULKERS_KEY)) return;
        data.set(SHULKER_YAW_KEY, PersistentDataType.FLOAT, yaw);
        Location root = base.getLocation();
        for (UUID id : links(base)) {
            Entity shulker = Bukkit.getEntity(id);
            if (shulker != null) reposition(shulker, root, yaw);
        }
    }

    private static float shulkerYaw(Entity base) {
        return base.getPersistentDataContainer().getOrDefault(SHULKER_YAW_KEY, PersistentDataType.FLOAT, getFurnitureYaw(base));
    }

    private static void reposition(Entity entity, Location root, float yaw) {
        if (!(entity instanceof Shulker shulker)) return;
        SchedulerUtil.runOnOwningThread(shulker, () -> {
            List<Double> offset = shulker.getPersistentDataContainer().get(SHULKER_OFFSET_KEY, DataType.asList(PersistentDataType.DOUBLE));
            if (offset == null || offset.size() != 3) return;
            Location target = hitboxLocation(root, new Vector(offset.get(0), offset.get(1), offset.get(2)), yaw);
            shulker.getPersistentDataContainer().set(SHULKER_ROOT_KEY, DataType.LOCATION, root);
            if (!activate(shulker, target, yaw)) {
                discard(shulker);
                return;
            }
            Entity vehicle = shulker.getVehicle();
            Location current = vehicle.getLocation();
            if (!current.getWorld().equals(target.getWorld()) || current.distanceSquared(target) > 1.0E-8)
                vehicle.teleportAsync(target, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS)
                        .thenAccept(success -> SchedulerUtil.runForEntityLater(shulker, 2L, () -> applyPeek(shulker)));
        });
    }

    private static Location hitboxLocation(Location root, Vector offset, float yaw) {
        return BlockHelpers.toCenterBlockLocation(root).add(rotateGroundOffset(offset, yaw));
    }

    @EventHandler
    public void onItemsLoaded(OraxenItemsLoadedEvent event) {
        // Spawn chunks can already contain shulkers before this listener is registered.
        // Reloads can also change whether debug mode shows them.
        for (World world : Bukkit.getWorlds()) {
            for (Shulker shulker : world.getEntitiesByClass(Shulker.class)) {
                SchedulerUtil.runOnOwningThread(shulker, () -> {
                    if (isHitbox(shulker)) activate(shulker, shulker.getLocation(), currentYaw(shulker));
                });
            }
        }
    }

    private static float currentYaw(Shulker shulker) {
        Entity base = baseEntity(shulker);
        return base == null ? 0f : shulkerYaw(base);
    }

    private static Entity baseEntity(Entity shulker) {
        UUID baseId = shulker.getPersistentDataContainer().get(BASE_ENTITY_KEY, DataType.UUID);
        return baseId == null ? null : Bukkit.getEntity(baseId);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (isVehicle(entity)) {
                if (entity.getPassengers().stream().noneMatch(FurnitureShulkerEntities::isHitbox)) entity.remove();
                continue;
            }
            if (!(entity instanceof Shulker shulker) || !isHitbox(shulker)) continue;
            PersistentDataContainer data = shulker.getPersistentDataContainer();
            UUID baseId = data.get(BASE_ENTITY_KEY, DataType.UUID);
            Location root = data.get(SHULKER_ROOT_KEY, DataType.LOCATION);
            if (baseId == null || root == null || !root.isWorldLoaded()) {
                discard(shulker);
                continue;
            }
            Entity base = Bukkit.getEntity(baseId);
            if (base != null) {
                checkLink(base, shulker);
                continue;
            }
            // A hitbox offset can put it in a different chunk from the furniture.
            root.getWorld().getChunkAtAsync(root, false).thenAccept(chunk -> {
                if (chunk == null) {
                    SchedulerUtil.runOnOwningThread(shulker, () -> discard(shulker));
                    return;
                }
                SchedulerUtil.runAtLocation(root, () -> {
                    Entity loadedBase = Bukkit.getEntity(baseId);
                    if (loadedBase == null) {
                        for (Entity chunkEntity : chunk.getEntities()) {
                            if (chunkEntity.getUniqueId().equals(baseId)) {
                                loadedBase = chunkEntity;
                                break;
                            }
                        }
                    }
                    if (loadedBase == null) SchedulerUtil.runOnOwningThread(shulker, () -> discard(shulker));
                    else checkLink(loadedBase, shulker);
                });
            });
        }
    }

    private static void checkLink(Entity base, Shulker shulker) {
        SchedulerUtil.runOnOwningThread(base, () -> {
            if (links(base).contains(shulker.getUniqueId())) reposition(shulker, base.getLocation(), shulkerYaw(base));
            else SchedulerUtil.runOnOwningThread(shulker, () -> discard(shulker));
        });
    }

    // Clients reset the attached face when the shulker mounts its vehicle after its metadata arrives.
    @EventHandler
    public void onPlayerTrackEntity(PlayerTrackEntityEvent event) {
        if (!(event.getEntity() instanceof Shulker shulker) || !isHitbox(shulker)
                || shulker.getAttachedFace() == BlockFace.DOWN) return;
        SchedulerUtil.runForEntityLater(shulker, 1L, () -> {
            BlockFace face = shulker.getAttachedFace();
            shulker.setAttachedFace(face.getOppositeFace());
            shulker.setAttachedFace(face);
        });
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event) {
        Entity entity = event.getEntity();
        if (isHitbox(entity)) {
            NMSHandlers.getHandler().untrackFurnitureHitbox(entity);
            Entity vehicle = entity.getVehicle();
            if (event.getCause() != EntityRemoveEvent.Cause.UNLOAD && vehicle != null && isVehicle(vehicle))
                SchedulerUtil.runOnOwningThread(vehicle, vehicle::remove);
        }
        if (event.getCause() != EntityRemoveEvent.Cause.UNLOAD) remove(entity);
    }
}
