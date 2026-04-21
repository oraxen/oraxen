package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registration entry for a placed furniture base that has one or more
 * text displays attached to it.
 */
public final class FurnitureTextEntry {

    private static final AtomicInteger VIRTUAL_ID_COUNTER = new AtomicInteger(-1_000_000_000);

    private final UUID baseUuid;
    private final int baseEntityId;
    private final Location baseLocation;
    private final List<FurnitureTextDefinition> definitions;
    private final int[] virtualEntityIds;
    private final UUID[] virtualUuids;
    private final UUID[] textEntityUuids;
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
        this.textEntityUuids = new UUID[definitions.size()];
        for (int i = 0; i < definitions.size(); i++) {
            virtualEntityIds[i] = VIRTUAL_ID_COUNTER.getAndDecrement();
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
        this.textEntityUuids = new UUID[definitions.size()];
        this.viewers.addAll(previous.viewers);
    }

    public UUID getBaseUuid() { return baseUuid; }
    public int getBaseEntityId() { return baseEntityId; }
    public Location getBaseLocation() { return baseLocation.clone(); }
    public List<FurnitureTextDefinition> getDefinitions() { return definitions; }
    public int[] getVirtualEntityIds() { return Arrays.copyOf(virtualEntityIds, virtualEntityIds.length); }
    public UUID virtualUuid(int index) { return virtualUuids[index]; }
    public int virtualEntityId(int index) { return virtualEntityIds[index]; }
    public int size() { return definitions.size(); }
    public void addViewer(UUID viewer) { if (viewer != null) viewers.add(viewer); }
    public void removeViewer(UUID viewer) { if (viewer != null) viewers.remove(viewer); }
    public Set<UUID> getViewers() { return Set.copyOf(viewers); }

    public void spawnTextDisplays() {
        if (baseLocation.getWorld() == null) return;
        removeTextDisplays();
        for (int i = 0; i < definitions.size(); i++) {
            FurnitureTextDefinition definition = definitions.get(i);
            Vector3f offset = definition.getTranslation();
            Location textLocation = baseLocation.clone().add(offset.x, offset.y, offset.z);
            TextDisplay display = baseLocation.getWorld().spawn(textLocation, TextDisplay.class, text -> {
                applyDefinition(text, definition);
                text.setPersistent(false);
                text.setInvulnerable(true);
                text.setGravity(false);
            });
            textEntityUuids[i] = display.getUniqueId();
        }
    }

    public void updateTextDisplays() {
        for (int i = 0; i < textEntityUuids.length; i++) {
            UUID uuid = textEntityUuids[i];
            if (uuid == null) continue;
            Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof TextDisplay text && !text.isDead()) applyDefinition(text, definitions.get(i));
        }
    }

    public void removeTextDisplays() {
        for (int i = 0; i < textEntityUuids.length; i++) {
            UUID uuid = textEntityUuids[i];
            if (uuid == null) continue;
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && !entity.isDead()) entity.remove();
            textEntityUuids[i] = null;
        }
    }

    private static void applyDefinition(TextDisplay text, FurnitureTextDefinition definition) {
        text.text(definition.renderComponent(null));
        text.setLineWidth(definition.getLineWidth());
        text.setBackgroundColor(Color.fromARGB(definition.getBackgroundArgb()));
        text.setTextOpacity(definition.getTextOpacity());
        text.setShadowed(definition.hasShadow());
        text.setSeeThrough(definition.isSeeThrough());
        text.setDefaultBackground(definition.hasDefaultBackground());
        text.setAlignment(switch (definition.getAlignment()) {
            case LEFT -> TextDisplay.TextAlignment.LEFT;
            case RIGHT -> TextDisplay.TextAlignment.RIGHT;
            case CENTER -> TextDisplay.TextAlignment.CENTER;
        });
        text.setBillboard(switch (definition.getBillboard()) {
            case FIXED -> Display.Billboard.FIXED;
            case VERTICAL -> Display.Billboard.VERTICAL;
            case HORIZONTAL -> Display.Billboard.HORIZONTAL;
            case CENTER -> Display.Billboard.CENTER;
        });
        Vector3f scale = definition.getScale();
        text.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(),
                new Vector3f(scale.x, scale.y, scale.z),
                new AxisAngle4f()
        ));
        text.setViewRange(definition.getViewRange());
    }

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
}
