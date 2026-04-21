package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

final class FurnitureTextPacketRegistration {

    private static PacketListenerCommon registered;
    private static FurnitureTextPacketListener listener;
    private static Listener bukkitListener;
    private static SchedulerUtil.ScheduledTask refreshTask;
    private static long tick;

    private FurnitureTextPacketRegistration() {
    }

    static void register() {
        if (registered != null) return;
        listener = new FurnitureTextPacketListener();
        registered = PacketEvents.getAPI().getEventManager()
                .registerListener(listener, PacketListenerPriority.MONITOR);
        bukkitListener = new ViewerCleanupListener();
        Bukkit.getPluginManager().registerEvents(bukkitListener, OraxenPlugin.get());
        refreshTask = SchedulerUtil.runTaskTimer(1L, 1L, () -> {
            FurnitureTextPacketListener activeListener = listener;
            if (activeListener != null) activeListener.refresh(++tick);
        });
        MechanicsManager.registerTask("furniture", refreshTask);
    }

    static void unregister() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (registered != null) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(registered);
            } catch (Throwable ignored) {
            }
        }
        if (bukkitListener != null) {
            HandlerList.unregisterAll(bukkitListener);
            bukkitListener = null;
        }
        registered = null;
        listener = null;
        tick = 0L;
    }

    static void destroyRegisteredTextEntities() {
        for (FurnitureTextEntry entry : FurnitureTextRegistry.all()) {
            destroyTextEntry(entry);
        }
    }

    static void destroyTextEntry(FurnitureTextEntry entry) {
        if (entry == null) return;
        int[] virtualIds = entry.getVirtualEntityIds();
        if (virtualIds.length == 0) return;
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(virtualIds);
        for (UUID viewerId : entry.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) continue;
            PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(destroy);
        }
    }

    static void spawnForTrackedViewers(FurnitureTextEntry entry) {
        FurnitureTextPacketListener activeListener = listener;
        if (entry == null || activeListener == null) return;
        Entity baseEntity = Bukkit.getEntity(entry.getBaseUuid());
        if (baseEntity == null) return;
        for (Player viewer : trackedViewers(entry, baseEntity)) {
            activeListener.sendTextEntry(entry, viewer, true);
        }
    }

    private static List<Player> trackedViewers(FurnitureTextEntry entry, Entity baseEntity) {
        try {
            Object tracked = Entity.class.getMethod("getTrackedBy").invoke(baseEntity);
            if (tracked instanceof Collection<?> collection) {
                List<Player> viewers = new ArrayList<>(collection.size());
                for (Object candidate : collection) {
                    if (candidate instanceof Player player) viewers.add(player);
                }
                if (!viewers.isEmpty()) return viewers;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | LinkageError ignored) {
        }

        List<Player> viewers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (FurnitureTextPacketListener.isWithinRange(entry, player)) viewers.add(player);
        }
        return viewers;
    }

    static void updateTrackedViewers(FurnitureTextEntry entry) {
        FurnitureTextPacketListener activeListener = listener;
        if (entry == null || activeListener == null) return;
        for (UUID viewerId : entry.getViewers()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) {
                entry.removeViewer(viewerId);
                continue;
            }
            activeListener.sendTextMetadata(entry, viewer, true);
        }
    }

    private static final class ViewerCleanupListener implements Listener {
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            FurnitureTextRegistry.removeViewer(event.getPlayer().getUniqueId());
        }
    }
}
