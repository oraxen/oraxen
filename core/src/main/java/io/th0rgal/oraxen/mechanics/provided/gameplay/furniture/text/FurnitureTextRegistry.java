package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of placed furniture bases that own virtual text entities.
 *
 * <p>Lookup is done by the base entity's UUID (matches the UUID carried in the
 * SPAWN_ENTITY packet for the base ItemDisplay/ArmorStand/ItemFrame) as well as
 * by the integer entity id (used when the DESTROY_ENTITIES packet is intercepted).</p>
 */
public final class FurnitureTextRegistry {

    private static final Map<UUID, FurnitureTextEntry> BY_UUID = new ConcurrentHashMap<>();
    private static final Map<Integer, FurnitureTextEntry> BY_ENTITY_ID = new ConcurrentHashMap<>();

    private FurnitureTextRegistry() {
    }

    public static FurnitureTextEntry register(Entity baseEntity, List<FurnitureTextDefinition> definitions) {
        if (baseEntity == null || definitions == null || definitions.isEmpty()) return null;
        UUID uuid = baseEntity.getUniqueId();
        int entityId = baseEntity.getEntityId();
        Location location = baseEntity.getLocation();
        FurnitureTextEntry entry = new FurnitureTextEntry(uuid, entityId, location, List.copyOf(definitions));
        FurnitureTextEntry previous = BY_UUID.put(uuid, entry);
        if (previous != null) BY_ENTITY_ID.remove(previous.getBaseEntityId());
        BY_ENTITY_ID.put(entityId, entry);
        return entry;
    }

    public static FurnitureTextEntry byUuid(UUID uuid) {
        return uuid == null ? null : BY_UUID.get(uuid);
    }

    public static FurnitureTextEntry byEntityId(int entityId) {
        return BY_ENTITY_ID.get(entityId);
    }

    public static void unregister(UUID uuid) {
        if (uuid == null) return;
        FurnitureTextEntry entry = BY_UUID.remove(uuid);
        if (entry != null) BY_ENTITY_ID.remove(entry.getBaseEntityId());
    }

    public static boolean isEmpty() {
        return BY_UUID.isEmpty();
    }

    public static Iterable<FurnitureTextEntry> all() {
        return BY_UUID.values();
    }

    public static boolean hasRefreshableEntries() {
        for (FurnitureTextEntry entry : BY_UUID.values()) {
            if (entry.needsRefresh()) return true;
        }
        return false;
    }

    public static void clear() {
        BY_UUID.clear();
        BY_ENTITY_ID.clear();
    }
}
