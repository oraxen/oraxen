package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.api.OraxenFurniture;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;

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
}
