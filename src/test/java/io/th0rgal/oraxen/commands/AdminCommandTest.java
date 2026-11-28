package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.api.OraxenFurniture;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminCommandTest {

    @Test
    void removeAllUsesEntityAwareRemovalWithoutRequiringABarrierBlock() {
        World world = mock(World.class);
        Location target = new Location(world, 4, 64, 8);
        try (var furniture = mockStatic(OraxenFurniture.class)) {
            AdminCommand.removeFurniture(target, "all");

            furniture.verify(() -> OraxenFurniture.remove(target, null));
            verifyNoInteractions(world);
        }
    }

    @Test
    void allIsAcceptedOnlyForRemoval() {
        try (var furniture = mockStatic(OraxenFurniture.class)) {
            assertTrue(AdminCommand.isValidFurnitureTarget("remove", "all"));
            assertFalse(AdminCommand.isValidFurnitureTarget("place", "all"));
        }
    }

    @Test
    void registeredFurnitureIsAcceptedForBothOperations() {
        try (var furniture = mockStatic(OraxenFurniture.class)) {
            furniture.when(() -> OraxenFurniture.isFurniture("chair")).thenReturn(true);

            assertTrue(AdminCommand.isValidFurnitureTarget("remove", "chair"));
            assertTrue(AdminCommand.isValidFurnitureTarget("place", "chair"));
        }
    }

    @Test
    void unknownFurnitureIsRejectedForBothOperations() {
        try (var furniture = mockStatic(OraxenFurniture.class)) {
            assertFalse(AdminCommand.isValidFurnitureTarget("remove", "unknown"));
            assertFalse(AdminCommand.isValidFurnitureTarget("place", "unknown"));
        }
    }

    @Test
    void targetLocationsStayWithinTheWorldHeight() {
        World world = mock(World.class);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        Collection<Location> nearTop = AdminCommand.getTargetLocations(new Location(world, 0, 318, 0), 3, false);
        Collection<Location> nearBottom = AdminCommand.getTargetLocations(new Location(world, 0, -63, 0), 3, false);

        assertEquals(319, nearTop.stream().mapToInt(Location::getBlockY).max().orElseThrow());
        assertEquals(-64, nearBottom.stream().mapToInt(Location::getBlockY).min().orElseThrow());
        assertEquals(7 * 7 * 5, nearTop.size());
        assertTrue(AdminCommand.getTargetLocations(new Location(world, 0, 400, 0), 1, true).isEmpty());
    }
}
