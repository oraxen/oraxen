package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory registry of placed furniture bases that own text entities.
 *
 * <p>Lookup is done by the base entity's UUID (matches the UUID carried in the
 * SPAWN_ENTITY packet for the base ItemDisplay/ArmorStand/ItemFrame) as well as
 * by the integer entity id (used when the DESTROY_ENTITIES packet is intercepted).</p>
 */
public final class FurnitureTextRegistry {

    private static final Map<UUID, FurnitureTextEntry> BY_UUID = new ConcurrentHashMap<>();
    private static final Map<Integer, FurnitureTextEntry> BY_ENTITY_ID = new ConcurrentHashMap<>();
    private static final AtomicInteger REFRESHABLE_ENTRIES = new AtomicInteger();

    private FurnitureTextRegistry() {
    }

    public static FurnitureTextEntry register(Entity baseEntity, List<FurnitureTextDefinition> definitions) {
        if (baseEntity == null || definitions == null || definitions.isEmpty()) return null;
        UUID uuid = baseEntity.getUniqueId();
        int entityId = baseEntity.getEntityId();
        Location location = baseLocation(baseEntity);
        FurnitureTextEntry previous = BY_UUID.get(uuid);
        List<FurnitureTextDefinition> copiedDefinitions = List.copyOf(definitions);
        FurnitureTextEntry entry = previous != null && previous.size() == copiedDefinitions.size()
                ? new FurnitureTextEntry(previous, entityId, location, copiedDefinitions)
                : new FurnitureTextEntry(uuid, entityId, location, copiedDefinitions);
        previous = BY_UUID.put(uuid, entry);
        if (previous != null) {
            BY_ENTITY_ID.remove(previous.getBaseEntityId());
        }
        BY_ENTITY_ID.put(entityId, entry);
        updateRefreshableCount(previous, entry);
        return entry;
    }

    public static boolean canReuse(UUID uuid, int definitionCount) {
        FurnitureTextEntry entry = byUuid(uuid);
        return entry != null && entry.size() == definitionCount;
    }

    public static FurnitureTextEntry byUuid(UUID uuid) {
        return uuid == null ? null : BY_UUID.get(uuid);
    }

    public static FurnitureTextEntry byEntityId(int entityId) {
        return BY_ENTITY_ID.get(entityId);
    }

    public static FurnitureTextEntry updateBaseEntity(Entity baseEntity) {
        if (baseEntity == null) return null;
        FurnitureTextEntry entry = byUuid(baseEntity.getUniqueId());
        if (entry == null || entry.getBaseEntityId() != baseEntity.getEntityId()) return null;
        entry.updateBaseLocation(baseLocation(baseEntity));
        return entry;
    }

    public static void unregister(UUID uuid) {
        if (uuid == null) return;
        FurnitureTextEntry entry = BY_UUID.remove(uuid);
        if (entry != null) {
            BY_ENTITY_ID.remove(entry.getBaseEntityId());
        }
        updateRefreshableCount(entry, null);
    }

    public static void unregister(UUID uuid, int entityId) {
        if (uuid == null) return;
        FurnitureTextEntry entry = BY_UUID.get(uuid);
        if (entry == null || entry.getBaseEntityId() != entityId) return;
        if (BY_UUID.remove(uuid, entry)) {
            updateRefreshableCount(entry, null);
        }
        BY_ENTITY_ID.remove(entityId, entry);
    }

    public static boolean isEmpty() {
        return BY_UUID.isEmpty();
    }

    public static Iterable<FurnitureTextEntry> all() {
        return BY_UUID.values();
    }

    public static boolean hasRefreshableEntries() {
        return REFRESHABLE_ENTRIES.get() > 0;
    }

    public static void removeViewer(UUID viewer) {
        if (viewer == null) return;
        for (FurnitureTextEntry entry : BY_UUID.values()) {
            entry.removeViewer(viewer);
        }
    }

    public static void clear() {
        BY_UUID.clear();
        BY_ENTITY_ID.clear();
        REFRESHABLE_ENTRIES.set(0);
    }

    private static void updateRefreshableCount(FurnitureTextEntry previous, FurnitureTextEntry current) {
        if (previous != null && previous.needsRefresh()) REFRESHABLE_ENTRIES.decrementAndGet();
        if (current != null && current.needsRefresh()) REFRESHABLE_ENTRIES.incrementAndGet();
    }

    private static Location baseLocation(Entity baseEntity) {
        Location location = baseEntity.getLocation();
        location.setYaw(FurnitureMechanic.getFurnitureYaw(baseEntity));
        return location;
    }
}
