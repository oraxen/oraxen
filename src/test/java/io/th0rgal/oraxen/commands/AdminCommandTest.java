package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.api.OraxenFurniture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class AdminCommandTest {

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
