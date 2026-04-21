package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import java.util.UUID;

public final class FurnitureTextPacketBridge {

    private FurnitureTextPacketBridge() {
    }

    public static void register() {
    }

    public static void unregister() {
        FurnitureTextRegistry.clear();
    }

    public static void destroyAndUnregister(UUID uuid) {
        FurnitureTextRegistry.unregister(uuid);
    }

    public static void spawnForTrackedViewers(FurnitureTextEntry entry) {
    }

    public static void updateTrackedViewers(FurnitureTextEntry entry) {
        if (entry != null) entry.updateTextDisplays();
    }
}
