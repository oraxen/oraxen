package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.th0rgal.oraxen.packets.PacketAdapter;
import io.th0rgal.oraxen.utils.SchedulerUtil;

/**
 * Thin bridge that registers/unregisters the furniture text packet listener.
 *
 * <p>Isolated in its own class so that {@link FurnitureTextPacketListener} is not
 * referenced (and therefore not class-loaded) on servers that don't have
 * PacketEvents installed.</p>
 */
public final class FurnitureTextPacketBridge {

    private static PacketListenerCommon registered;
    private static FurnitureTextPacketListener listener;
    private static SchedulerUtil.ScheduledTask refreshTask;
    private static long tick;

    private FurnitureTextPacketBridge() {
    }

    public static void register() {
        if (registered != null) return;
        if (!PacketAdapter.isPacketEventsEnabled()) return;
        listener = new FurnitureTextPacketListener();
        registered = PacketEvents.getAPI().getEventManager()
                .registerListener(listener, PacketListenerPriority.NORMAL);
        refreshTask = SchedulerUtil.runTaskTimer(1L, 1L, () -> {
            FurnitureTextPacketListener activeListener = listener;
            if (activeListener != null) activeListener.refresh(++tick);
        });
    }

    public static void unregister() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (registered == null) return;
        try {
            PacketEvents.getAPI().getEventManager().unregisterListener(registered);
        } catch (Throwable ignored) {
        }
        registered = null;
        listener = null;
        tick = 0L;
        FurnitureTextRegistry.clear();
    }
}
