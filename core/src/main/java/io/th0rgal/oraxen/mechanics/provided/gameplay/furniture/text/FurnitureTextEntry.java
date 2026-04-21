package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import io.th0rgal.oraxen.nms.NMSHandlers;
import org.bukkit.Location;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registration entry for a placed furniture base that has one or more
 * text displays attached to it.
 */
public final class FurnitureTextEntry {

    private static final int FALLBACK_VIRTUAL_ID_START = Integer.MAX_VALUE / 2;
    private static int fallbackVirtualId = FALLBACK_VIRTUAL_ID_START;

    private final UUID baseUuid;
    private final int baseEntityId;
    private volatile Location baseLocation;
    private final List<FurnitureTextDefinition> definitions;
    private final int[] virtualEntityIds;
    private final UUID[] virtualUuids;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    public FurnitureTextEntry(UUID baseUuid,
                               int baseEntityId,
                               Location baseLocation,
                               List<FurnitureTextDefinition> definitions) {
        this.baseUuid = baseUuid;
        this.baseEntityId = baseEntityId;
        this.baseLocation = baseLocation.clone();
        this.definitions = definitions;
        this.virtualEntityIds = new int[definitions.size()];
        this.virtualUuids = new UUID[definitions.size()];
        for (int i = 0; i < definitions.size(); i++) {
            virtualEntityIds[i] = nextVirtualEntityId();
            virtualUuids[i] = UUID.randomUUID();
        }
    }

    FurnitureTextEntry(FurnitureTextEntry previous,
                       int baseEntityId,
                       Location baseLocation,
                       List<FurnitureTextDefinition> definitions) {
        this.baseUuid = previous.baseUuid;
        this.baseEntityId = baseEntityId;
        this.baseLocation = baseLocation.clone();
        this.definitions = definitions;
        this.virtualEntityIds = Arrays.copyOf(previous.virtualEntityIds, previous.virtualEntityIds.length);
        this.virtualUuids = Arrays.copyOf(previous.virtualUuids, previous.virtualUuids.length);
        this.viewers.addAll(previous.viewers);
    }

    public UUID getBaseUuid() { return baseUuid; }
    public int getBaseEntityId() { return baseEntityId; }
    public Location getBaseLocation() { return baseLocation.clone(); }
    public void updateBaseLocation(Location location) {
        if (location != null) baseLocation = location.clone();
    }
    public List<FurnitureTextDefinition> getDefinitions() { return definitions; }
    public int[] getVirtualEntityIds() { return Arrays.copyOf(virtualEntityIds, virtualEntityIds.length); }
    public UUID virtualUuid(int index) { return virtualUuids[index]; }
    public int virtualEntityId(int index) { return virtualEntityIds[index]; }
    public int size() { return definitions.size(); }
    public void addViewer(UUID viewer) { if (viewer != null) viewers.add(viewer); }
    public void removeViewer(UUID viewer) { if (viewer != null) viewers.remove(viewer); }
    public Set<UUID> getViewers() { return Set.copyOf(viewers); }

    public boolean needsRefresh() {
        for (FurnitureTextDefinition definition : definitions) {
            if (definition.getRefreshTicks() > 0 || definition.usesPlaceholders()) return true;
        }
        return false;
    }

    public boolean shouldRefresh(long tick) {
        for (FurnitureTextDefinition definition : definitions) {
            int refreshTicks = refreshInterval(definition);
            if (refreshTicks > 0 && tick % refreshTicks == 0) return true;
        }
        return false;
    }

    public boolean shouldRefresh(FurnitureTextDefinition definition, long tick) {
        int refreshTicks = refreshInterval(definition);
        return refreshTicks > 0 && tick % refreshTicks == 0;
    }

    private static int refreshInterval(FurnitureTextDefinition definition) {
        return definition.getRefreshTicks() > 0 ? definition.getRefreshTicks() : (definition.usesPlaceholders() ? 20 : 0);
    }

    private static synchronized int nextVirtualEntityId() {
        int entityId = NMSHandlers.getHandler().getNextEntityId();
        if (entityId != -1) return entityId;
        return --fallbackVirtualId;
    }
}
