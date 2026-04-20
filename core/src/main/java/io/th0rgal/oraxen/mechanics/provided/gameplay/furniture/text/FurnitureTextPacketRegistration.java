package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

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
                .registerListener(listener, PacketListenerPriority.NORMAL);
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

    private static final class ViewerCleanupListener implements Listener {
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            FurnitureTextRegistry.removeViewer(event.getPlayer().getUniqueId());
        }
    }
}
