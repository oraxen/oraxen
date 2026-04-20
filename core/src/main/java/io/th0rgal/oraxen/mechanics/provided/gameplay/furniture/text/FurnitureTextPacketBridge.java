package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import io.th0rgal.oraxen.packets.PacketAdapter;

public final class FurnitureTextPacketBridge {

    private FurnitureTextPacketBridge() {
    }

    public static void register() {
        if (!PacketAdapter.isPacketEventsEnabled()) return;
        FurnitureTextPacketRegistration.register();
    }

    public static void unregister() {
        FurnitureTextRegistry.clear();
        if (!PacketAdapter.isPacketEventsEnabled()) return;
        try {
            FurnitureTextPacketRegistration.unregister();
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
