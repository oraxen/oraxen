package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.packets.PacketAdapter;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.joml.Vector3f;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class FurnitureTextPacketBridge {

    private static final byte FLAG_SHADOW = 0x01;
    private static final byte FLAG_SEE_THROUGH = 0x02;
    private static final byte FLAG_DEFAULT_BACKGROUND = 0x04;
    private static final byte FLAG_ALIGN_LEFT = 0x08;
    private static final byte FLAG_ALIGN_RIGHT = 0x10;

    private static Listener trackingListener;
    private static SchedulerUtil.ScheduledTask refreshTask;
    private static long tick;

    private FurnitureTextPacketBridge() {
    }

    public static void register() {
        PacketAdapter adapter = adapter();
        if (adapter == null || !adapter.isEnabled() || trackingListener != null) return;

        trackingListener = new ViewerTrackingListener();
        Bukkit.getPluginManager().registerEvents(trackingListener, OraxenPlugin.get());
        refreshTask = SchedulerUtil.runTaskTimer(1L, 1L, () -> refresh(++tick));
        MechanicsManager.registerTask("furniture", refreshTask);
    }

    public static void unregister() {
        destroyRegisteredTextEntities();
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (trackingListener != null) {
            HandlerList.unregisterAll(trackingListener);
            trackingListener = null;
        }
        tick = 0L;
        FurnitureTextRegistry.clear();
    }

    public static void destroyAndUnregister(UUID uuid) {
        FurnitureTextEntry entry = FurnitureTextRegistry.byUuid(uuid);
        destroyTextEntry(entry);
        FurnitureTextRegistry.unregister(uuid);
    }

    public static void spawnForTrackedViewers(FurnitureTextEntry entry) {
        if (entry == null) return;
        register();
        Entity baseEntity = Bukkit.getEntity(entry.getBaseUuid());
        if (baseEntity == null) return;
        TrackedViewers trackedViewers = trackedViewers(baseEntity);
        for (Player viewer : trackedViewers.viewers()) {
            SchedulerUtil.runOnOwningThread(viewer, () -> sendTextEntry(entry, viewer, trackedViewers.exact()));
        }
    }

    public static void updateTrackedViewers(FurnitureTextEntry entry) {
        if (entry == null) return;
        for (UUID viewerId : entry.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null) {
                entry.removeViewer(viewerId);
                continue;
            }
            SchedulerUtil.runOnOwningThread(viewer, () -> {
                if (!viewer.isOnline()) {
                    entry.removeViewer(viewerId);
                    return;
                }
                sendTextMetadata(entry, viewer, true, -1L);
            });
        }
    }

    public static void respawnTrackedViewers(FurnitureTextEntry entry) {
        if (entry == null) return;
        destroyTextEntry(entry);
        spawnForTrackedViewers(entry);
    }

    public static boolean placementChanged(Location before, Location after) {
        if (before == null || after == null) return true;
        if (before.getWorld() == null ? after.getWorld() != null : !before.getWorld().equals(after.getWorld()))
            return true;
        return Math.abs(before.getX() - after.getX()) > 1.0E-4
                || Math.abs(before.getY() - after.getY()) > 1.0E-4
                || Math.abs(before.getZ() - after.getZ()) > 1.0E-4
                || Math.abs(before.getYaw() - after.getYaw()) > 0.01F;
    }

    private static void destroyRegisteredTextEntities() {
        for (FurnitureTextEntry entry : FurnitureTextRegistry.all()) {
            destroyTextEntry(entry);
        }
    }

    private static void destroyTextEntry(FurnitureTextEntry entry) {
        if (entry == null) return;
        for (UUID viewerId : entry.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null) continue;
            SchedulerUtil.runOnOwningThread(viewer, () -> {
                if (viewer.isOnline()) destroyTextEntry(entry, viewer);
            });
        }
    }

    private static void destroyTextEntry(FurnitureTextEntry entry, Player viewer) {
        PacketAdapter adapter = adapter();
        if (adapter == null || !adapter.isEnabled() || entry == null || viewer == null) return;
        int[] virtualIds = entry.getVirtualEntityIds();
        if (virtualIds.length != 0) adapter.destroyEntities(viewer, virtualIds);
    }

    private static void sendTextEntry(FurnitureTextEntry entry, Player viewer, boolean ignoreRange) {
        PacketAdapter adapter = adapter();
        if (adapter == null || !adapter.isEnabled() || entry == null || viewer == null || !viewer.isOnline()) return;
        if (!ignoreRange && !isWithinRange(entry, viewer)) return;

        entry.addViewer(viewer.getUniqueId());
        Location baseLocation = entry.getBaseLocation();
        float yaw = baseLocation.getYaw();
        for (int i = 0; i < entry.size(); i++) {
            FurnitureTextDefinition definition = entry.getDefinitions().get(i);
            Vector3f offset = rotateOffset(definition.getTranslation(), yaw);
            Location textLocation = baseLocation.clone().add(offset.x, offset.y, offset.z);
            adapter.spawnTextDisplay(viewer, entry.virtualEntityId(i), entry.virtualUuid(i), textLocation);
        }

        UUID baseUuid = entry.getBaseUuid();
        int baseEntityId = entry.getBaseEntityId();
        SchedulerUtil.runForEntityLater(viewer, 1L, () -> {
            if (!viewer.isOnline()) return;
            FurnitureTextEntry current = FurnitureTextRegistry.byUuid(baseUuid);
            if (current == null || current.getBaseEntityId() != baseEntityId) return;
            sendTextMetadata(current, viewer, true, -1L);
        });
    }

    private static void sendTextMetadata(FurnitureTextEntry entry, Player viewer, boolean ignoreRange, long refreshTick) {
        PacketAdapter adapter = adapter();
        if (adapter == null || !adapter.isEnabled() || entry == null || viewer == null || !viewer.isOnline()) return;
        if (!ignoreRange && !isWithinRange(entry, viewer)) return;

        entry.addViewer(viewer.getUniqueId());
        for (int i = 0; i < entry.size(); i++) {
            FurnitureTextDefinition definition = entry.getDefinitions().get(i);
            if (refreshTick >= 0 && !entry.shouldRefresh(definition, refreshTick)) continue;
            adapter.sendTextDisplayMetadata(
                    viewer,
                    entry.virtualEntityId(i),
                    definition.renderComponent(viewer),
                    definition.getScale(),
                    billboardByte(definition),
                    definition.getViewRange(),
                    definition.getLineWidth(),
                    definition.getBackgroundArgb(),
                    definition.getTextOpacity(),
                    textFlags(definition)
            );
        }
    }

    private static void refresh(long currentTick) {
        if (!FurnitureTextRegistry.hasRefreshableEntries()) return;

        for (FurnitureTextEntry entry : FurnitureTextRegistry.all()) {
            if (!entry.needsRefresh() || !entry.shouldRefresh(currentTick)) continue;
            for (UUID viewerId : entry.getViewers()) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null) {
                    entry.removeViewer(viewerId);
                    continue;
                }
                UUID baseUuid = entry.getBaseUuid();
                int baseEntityId = entry.getBaseEntityId();
                SchedulerUtil.runForEntity(viewer, () -> {
                    if (!viewer.isOnline()) {
                        entry.removeViewer(viewerId);
                        return;
                    }
                    FurnitureTextEntry current = FurnitureTextRegistry.byUuid(baseUuid);
                    if (current == null || current.getBaseEntityId() != baseEntityId) return;
                    sendTextMetadata(current, viewer, false, currentTick);
                });
            }
        }
    }

    private static TrackedViewers trackedViewers(Entity baseEntity) {
        try {
            Object tracked = Entity.class.getMethod("getTrackedBy").invoke(baseEntity);
            if (tracked instanceof Collection<?> collection) {
                List<Player> viewers = new ArrayList<>(collection.size());
                for (Object candidate : collection) {
                    if (candidate instanceof Player player) viewers.add(player);
                }
                if (!viewers.isEmpty()) return new TrackedViewers(viewers, true);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | LinkageError ignored) {
        }

        return new TrackedViewers(new ArrayList<>(Bukkit.getOnlinePlayers()), false);
    }

    static boolean isWithinRange(FurnitureTextEntry entry, Player viewer) {
        Location baseLocation = entry.getBaseLocation();
        if (baseLocation.getWorld() == null || !baseLocation.getWorld().equals(viewer.getWorld())) return false;

        double maxRange = 0.0;
        for (FurnitureTextDefinition definition : entry.getDefinitions()) {
            maxRange = Math.max(maxRange, definition.getViewRange());
        }
        double range = Math.max(8.0, maxRange);
        return baseLocation.distanceSquared(viewer.getLocation()) <= range * range;
    }

    private static byte billboardByte(FurnitureTextDefinition definition) {
        return switch (definition.getBillboard()) {
            case FIXED -> (byte) 0;
            case VERTICAL -> (byte) 1;
            case HORIZONTAL -> (byte) 2;
            case CENTER -> (byte) 3;
        };
    }

    private static byte textFlags(FurnitureTextDefinition definition) {
        byte flags = 0;
        if (definition.hasShadow()) flags |= FLAG_SHADOW;
        if (definition.isSeeThrough()) flags |= FLAG_SEE_THROUGH;
        if (definition.hasDefaultBackground()) flags |= FLAG_DEFAULT_BACKGROUND;
        switch (definition.getAlignment()) {
            case LEFT -> flags |= FLAG_ALIGN_LEFT;
            case RIGHT -> flags |= FLAG_ALIGN_RIGHT;
            case CENTER -> {
            }
        }
        return flags;
    }

    private static Vector3f rotateOffset(Vector3f offset, float yaw) {
        double radians = Math.toRadians(-(double) yaw);
        double x = offset.x;
        double z = offset.z;
        return new Vector3f(
                (float) (Math.cos(radians) * x - Math.sin(radians) * z),
                offset.y,
                (float) (Math.sin(radians) * x + Math.cos(radians) * z)
        );
    }

    private static PacketAdapter adapter() {
        OraxenPlugin plugin = OraxenPlugin.get();
        return plugin != null ? plugin.getPacketAdapter() : null;
    }

    private static final class ViewerTrackingListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onTrack(PlayerTrackEntityEvent event) {
            FurnitureTextEntry entry = FurnitureTextRegistry.byUuid(event.getEntity().getUniqueId());
            if (entry == null) return;

            Player viewer = event.getPlayer();
            UUID baseUuid = entry.getBaseUuid();
            int baseEntityId = entry.getBaseEntityId();
            SchedulerUtil.runForEntityLater(viewer, 1L, () -> {
                FurnitureTextEntry current = FurnitureTextRegistry.byUuid(baseUuid);
                if (current == null || current.getBaseEntityId() != baseEntityId) return;
                sendTextEntry(current, viewer, true);
            });
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onUntrack(PlayerUntrackEntityEvent event) {
            FurnitureTextEntry entry = FurnitureTextRegistry.byUuid(event.getEntity().getUniqueId());
            if (entry == null) return;
            Player viewer = event.getPlayer();
            SchedulerUtil.runOnOwningThread(viewer, () -> destroyTextEntry(entry, viewer));
            entry.removeViewer(viewer.getUniqueId());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onQuit(PlayerQuitEvent event) {
            FurnitureTextRegistry.removeViewer(event.getPlayer().getUniqueId());
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBaseTeleport(EntityTeleportEvent event) {
            Entity entity = event.getEntity();
            FurnitureTextEntry entry = FurnitureTextRegistry.byUuid(entity.getUniqueId());
            if (entry == null || entry.getBaseEntityId() != entity.getEntityId() || event.getTo() == null) return;

            Location destination = event.getTo().clone();
            if (entity instanceof ItemFrame)
                destination.setYaw(FurnitureMechanic.getFurnitureYaw(entity));
            if (!placementChanged(entry.getBaseLocation(), destination)) return;
            entry.updateBaseLocation(destination);
            respawnTrackedViewers(entry);
        }
    }

    private record TrackedViewers(List<Player> viewers, boolean exact) {
    }
}
