package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.backpack.BackpackCosmeticMechanic;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackCosmeticMechanicTest extends MechanicTestSupport {

    @Test
    void readsBackpackDisplaySettings() {
        BackpackCosmeticMechanic mechanic = new BackpackCosmeticMechanic(mechanicFactory(), mechanicSection("backpack_cosmetic",
                "slot", "CHEST",
                "model", "default/backpack",
                "offset", java.util.Map.of("x", 0.1, "y", 0.2, "z", -0.4),
                "scale", 1.5,
                "view_distance", 24,
                "hide_in_spectator", false,
                "hide_while_swimming", false,
                "hide_while_gliding", true,
                "small", true,
                "visible_to_self", false));

        assertEquals(EquipmentSlot.CHEST, mechanic.getTriggerSlot());
        assertFalse(mechanic.triggersFromInventory());
        assertEquals("default/backpack", mechanic.getDisplayModel());
        assertEquals(0.1, mechanic.getOffsetX());
        assertEquals(0.2, mechanic.getOffsetY());
        assertEquals(-0.4, mechanic.getOffsetZ());
        assertEquals(1.5f, mechanic.getScale());
        assertEquals(24, mechanic.getViewDistance());
        assertFalse(mechanic.hideInSpectator());
        assertFalse(mechanic.hideWhileSwimming());
        assertTrue(mechanic.hideWhileGliding());
        assertTrue(mechanic.isSmallArmorStand());
        assertFalse(mechanic.isVisibleToSelf());
    }
}
