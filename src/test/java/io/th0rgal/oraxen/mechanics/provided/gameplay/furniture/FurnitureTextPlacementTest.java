package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text.FurnitureTextPacketBridge;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FurnitureTextPlacementTest {

    @Test
    void placementChangeDetectsMovementAndYaw() {
        Location origin = new Location(null, 1, 2, 3, 90, 0);
        assertFalse(FurnitureTextPacketBridge.placementChanged(origin, new Location(null, 1, 2, 3, 90, 0)));
        assertTrue(FurnitureTextPacketBridge.placementChanged(origin, new Location(null, 1, 2, 4, 90, 0)));
        assertTrue(FurnitureTextPacketBridge.placementChanged(origin, new Location(null, 1, 2, 3, 180, 0)));
        assertTrue(FurnitureTextPacketBridge.placementChanged(origin, null));
    }
}
