package io.th0rgal.oraxen.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandsManagerTest {

    @Test
    void giveAmountMustBePositive() {
        assertFalse(CommandsManager.isValidGiveAmount(0));
        assertFalse(CommandsManager.isValidGiveAmount(-1));
        assertTrue(CommandsManager.isValidGiveAmount(1));
    }

    @Test
    void giveAmountIsCappedToInventorySizedStackSplit() {
        assertEquals(1, CommandsManager.capGiveAmountToInventory(1, 64));
        assertEquals(64, CommandsManager.capGiveAmountToInventory(64, 64));
        assertEquals(65, CommandsManager.capGiveAmountToInventory(65, 64));
        assertEquals(2304, CommandsManager.capGiveAmountToInventory(2304, 64));
        assertEquals(2304, CommandsManager.capGiveAmountToInventory(2305, 64));
        assertEquals(36, CommandsManager.capGiveAmountToInventory(37, 1));
    }
}
