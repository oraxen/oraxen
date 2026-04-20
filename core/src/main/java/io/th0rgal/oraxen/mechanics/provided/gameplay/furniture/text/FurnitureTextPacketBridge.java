package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.packets.PacketAdapter;

import java.util.UUID;

public final class FurnitureTextPacketBridge {

    private FurnitureTextPacketBridge() {
    }

    public static void register() {
        if (!OraxenPlugin.supportsDisplayEntities) return;
        if (!PacketAdapter.isPacketEventsEnabled()) return;
        FurnitureTextPacketRegistration.register();
    }

    public static void unregister() {
        if (PacketAdapter.isPacketEventsEnabled()) {
            try {
                FurnitureTextPacketRegistration.destroyRegisteredTextEntities();
                FurnitureTextPacketRegistration.unregister();
            } catch (NoClassDefFoundError ignored) {
            }
        }
        FurnitureTextRegistry.clear();
    }

    public static void destroyAndUnregister(UUID uuid) {
        FurnitureTextEntry entry = FurnitureTextRegistry.byUuid(uuid);
        if (PacketAdapter.isPacketEventsEnabled()) {
            try {
                FurnitureTextPacketRegistration.destroyTextEntry(entry);
            } catch (NoClassDefFoundError ignored) {
            }
        }
        FurnitureTextRegistry.unregister(uuid);
    }

    public static void spawnForTrackedViewers(FurnitureTextEntry entry) {
        if (!PacketAdapter.isPacketEventsEnabled()) return;
        try {
            FurnitureTextPacketRegistration.spawnForTrackedViewers(entry);
        } catch (NoClassDefFoundError ignored) {
        }
    }

    public static void updateTrackedViewers(FurnitureTextEntry entry) {
        if (!PacketAdapter.isPacketEventsEnabled()) return;
        try {
            FurnitureTextPacketRegistration.updateTrackedViewers(entry);
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
