package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.nms.NMSHandler;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EntityUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HappyGhast;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.*;

final class FurnitureGhastEntities implements Listener {

    private static final Set<Integer> activeGhasts = ConcurrentHashMap.newKeySet();
    private static final NamespacedKey GHAST_OFFSET_KEY = new NamespacedKey(OraxenPlugin.get(), "ghast_offset");
    private static final NamespacedKey GHAST_ROOT_KEY = new NamespacedKey(OraxenPlugin.get(), "ghast_root");
    private static final NamespacedKey GHAST_YAW_KEY = new NamespacedKey(OraxenPlugin.get(), "ghast_yaw");

    static boolean isHitbox(Entity entity) {
        return entity.getPersistentDataContainer().has(GHAST_OFFSET_KEY);
    }

    static void spawn(Entity base, Location root, float yaw, String itemId, List<FurnitureGhastHitbox> hitboxes) {
        PersistentDataContainer data = base.getPersistentDataContainer();
        if (data.has(GHASTS_KEY)) return;
        data.set(GHASTS_KEY, DataType.asList(DataType.UUID), List.of());
        data.set(GHAST_YAW_KEY, PersistentDataType.FLOAT, yaw);

        for (FurnitureGhastHitbox hitbox : hitboxes) {
            Location location = hitboxLocation(root, hitbox.offset(), yaw);
            if (Bukkit.isOwnedByCurrentRegion(location)) {
                Entity ghast = spawnHitbox(base.getUniqueId(), root, location, itemId, hitbox);
                if (ghast != null) addLink(base, ghast);
            } else {
                UUID baseId = base.getUniqueId();
                SchedulerUtil.runAtLocation(location, () -> {
                    Entity ghast = spawnHitbox(baseId, root, location, itemId, hitbox);
                    if (ghast == null) return;
                    Runnable discard = () -> SchedulerUtil.runForEntity(ghast, ghast::remove);
                    if (SchedulerUtil.runForEntity(base, () -> {
                        if (!base.isValid() || !base.getPersistentDataContainer().has(GHASTS_KEY)) {
                            discard.run();
                            return;
                        }
                        addLink(base, ghast);
                        reposition(ghast, base.getLocation(), ghastYaw(base));
                    }, discard) == null) discard.run();
                });
            }
        }
    }

    private static Entity spawnHitbox(UUID baseId, Location root, Location location, String itemId, FurnitureGhastHitbox hitbox) {
        HappyGhast ghast = EntityUtils.spawnEntity(location, HappyGhast.class, entity -> {
            entity.setAdult();
            entity.setInvisible(!Settings.DEBUG.toBool());
            entity.setSilent(true);
            entity.setAI(false);
            entity.setGravity(false);
            entity.setNoPhysics(true);
            entity.setInvulnerable(true);
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
            entity.getAttribute(Attribute.SCALE).setBaseValue(hitbox.scale());
            PersistentDataContainer data = entity.getPersistentDataContainer();
            data.set(FURNITURE_KEY, PersistentDataType.STRING, itemId);
            data.set(BASE_ENTITY_KEY, DataType.UUID, baseId);
            data.set(GHAST_OFFSET_KEY, DataType.asList(PersistentDataType.DOUBLE), List.of(hitbox.x(), hitbox.y(), hitbox.z()));
            data.set(GHAST_ROOT_KEY, DataType.LOCATION, root);
            NMSHandlers.getHandler().trackFurnitureHitbox(entity);
        });
        if (ghast == null || !ghast.isValid() || !activate(ghast)) return null;
        return ghast;
    }

    private static boolean activate(Entity ghast) {
        NMSHandlers.getHandler().trackFurnitureHitbox(ghast);
        ghast.setInvisible(!Settings.DEBUG.toBool());
        ghast.setNoPhysics(true);
        NMSHandler handler = NMSHandlers.getHandler();
        if (!handler.keepGhastStationary(ghast)) {
            ghast.remove();
            return false;
        }
        int entityId = ghast.getEntityId();
        if (!activeGhasts.add(entityId)) return true;
        if (SchedulerUtil.runForEntityTimer(ghast, 5L, 5L, () -> {
            boolean invisible = !Settings.DEBUG.toBool();
            if (ghast.isInvisible() != invisible) ghast.setInvisible(invisible);
            handler.keepGhastStationary(ghast);
        }, () -> activeGhasts.remove(entityId)) == null) {
            activeGhasts.remove(entityId);
            return false;
        }
        return true;
    }

    private static void addLink(Entity base, Entity ghast) {
        List<UUID> links = new ArrayList<>(links(base));
        links.add(ghast.getUniqueId());
        base.getPersistentDataContainer().set(GHASTS_KEY, DataType.asList(DataType.UUID), links);
    }

    private static List<UUID> links(Entity base) {
        return base.getPersistentDataContainer().getOrDefault(GHASTS_KEY, DataType.asList(DataType.UUID), List.of());
    }

    static void remove(Entity base) {
        PersistentDataContainer data = base.getPersistentDataContainer();
        if (!data.has(GHASTS_KEY)) return;
        for (UUID id : links(base)) {
            Entity ghast = Bukkit.getEntity(id);
            if (ghast != null) SchedulerUtil.runOnOwningThread(ghast, () -> {
                NMSHandlers.getHandler().untrackFurnitureHitbox(ghast);
                ghast.remove();
            });
        }
        data.remove(GHASTS_KEY);
        data.remove(GHAST_YAW_KEY);
    }

    static void rotate(Entity base, float yaw) {
        PersistentDataContainer data = base.getPersistentDataContainer();
        if (!data.has(GHASTS_KEY)) return;
        data.set(GHAST_YAW_KEY, PersistentDataType.FLOAT, yaw);
        Location root = base.getLocation();
        for (UUID id : links(base)) {
            Entity ghast = Bukkit.getEntity(id);
            if (ghast != null) reposition(ghast, root, yaw);
        }
    }

    private static float ghastYaw(Entity base) {
        return base.getPersistentDataContainer().getOrDefault(GHAST_YAW_KEY, PersistentDataType.FLOAT, getFurnitureYaw(base));
    }

    private static void reposition(Entity ghast, Location root, float yaw) {
        SchedulerUtil.runOnOwningThread(ghast, () -> {
            List<Double> offset = ghast.getPersistentDataContainer().get(GHAST_OFFSET_KEY, DataType.asList(PersistentDataType.DOUBLE));
            if (offset == null || offset.size() != 3) return;
            Vector vector = new Vector(offset.get(0), offset.get(1), offset.get(2));
            Location target = hitboxLocation(root, vector, yaw);
            Location current = ghast.getLocation();
            ghast.getPersistentDataContainer().set(GHAST_ROOT_KEY, DataType.LOCATION, root);
            if (!current.getWorld().equals(target.getWorld()) || current.distanceSquared(target) > 1.0E-8)
                ghast.teleportAsync(target);
        });
    }

    private static Location hitboxLocation(Location root, Vector offset, float yaw) {
        return BlockHelpers.toCenterBlockLocation(root).add(rotateGroundOffset(offset, yaw));
    }

    @EventHandler
    public void onItemsLoaded(OraxenItemsLoadedEvent event) {
        // Spawn chunks can already contain ghasts before this listener is registered.
        for (World world : Bukkit.getWorlds()) {
            for (HappyGhast ghast : world.getEntitiesByClass(HappyGhast.class)) {
                SchedulerUtil.runOnOwningThread(ghast, () -> {
                    if (isHitbox(ghast)) activate(ghast);
                });
            }
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity ghast : event.getEntities()) {
            if (!isHitbox(ghast)) continue;
            if (!activate(ghast)) continue;
            PersistentDataContainer data = ghast.getPersistentDataContainer();
            UUID baseId = data.get(BASE_ENTITY_KEY, DataType.UUID);
            Location root = data.get(GHAST_ROOT_KEY, DataType.LOCATION);
            if (baseId == null || root == null || !root.isWorldLoaded()) {
                ghast.remove();
                continue;
            }
            Entity base = Bukkit.getEntity(baseId);
            if (base != null) {
                checkLink(base, ghast);
                continue;
            }
            // A hitbox offset can put it in a different chunk from the furniture.
            root.getWorld().getChunkAtAsync(root, false).thenAccept(chunk -> {
                if (chunk == null) {
                    SchedulerUtil.runOnOwningThread(ghast, ghast::remove);
                    return;
                }
                SchedulerUtil.runAtLocation(root, () -> {
                    Entity loadedBase = Bukkit.getEntity(baseId);
                    if (loadedBase == null) {
                        for (Entity entity : chunk.getEntities()) {
                            if (entity.getUniqueId().equals(baseId)) {
                                loadedBase = entity;
                                break;
                            }
                        }
                    }
                    if (loadedBase == null) SchedulerUtil.runOnOwningThread(ghast, ghast::remove);
                    else checkLink(loadedBase, ghast);
                });
            });
        }
    }

    private static void checkLink(Entity base, Entity ghast) {
        SchedulerUtil.runOnOwningThread(base, () -> {
            if (links(base).contains(ghast.getUniqueId())) reposition(ghast, base.getLocation(), ghastYaw(base));
            else SchedulerUtil.runOnOwningThread(ghast, ghast::remove);
        });
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event) {
        if (isHitbox(event.getEntity())) {
            activeGhasts.remove(event.getEntity().getEntityId());
            NMSHandlers.getHandler().untrackFurnitureHitbox(event.getEntity());
        }
        if (event.getCause() != EntityRemoveEvent.Cause.UNLOAD) remove(event.getEntity());
    }
}
