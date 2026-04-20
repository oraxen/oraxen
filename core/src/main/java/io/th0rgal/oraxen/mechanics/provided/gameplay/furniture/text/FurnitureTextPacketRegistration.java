package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.th0rgal.oraxen.utils.SchedulerUtil;

final class FurnitureTextPacketRegistration {

    private static PacketListenerCommon registered;
    private static FurnitureTextPacketListener listener;
    private static SchedulerUtil.ScheduledTask refreshTask;
    private static long tick;

    private FurnitureTextPacketRegistration() {
    }

    static void register() {
        if (registered != null) return;
        listener = new FurnitureTextPacketListener();
        registered = PacketEvents.getAPI().getEventManager()
                .registerListener(listener, PacketListenerPriority.NORMAL);
        refreshTask = SchedulerUtil.runTaskTimer(1L, 1L, () -> {
            FurnitureTextPacketListener activeListener = listener;
            if (activeListener != null) activeListener.refresh(++tick);
        });
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
        registered = null;
        listener = null;
        tick = 0L;
    }
}
