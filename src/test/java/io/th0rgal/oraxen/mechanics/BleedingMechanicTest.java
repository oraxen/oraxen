package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.bleeding.BleedingMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BleedingMechanicTest extends MechanicTestSupport {

    @Test
    void readsBleedingSettings() {
        BleedingMechanic mechanic = new BleedingMechanic(mechanicFactory(), mechanicSection("bleeding",
                "chance", 0.75,
                "duration", 80,
                "damage_per_interval", 1.25,
                "interval", 10));

        assertEquals("test_item", mechanic.getItemID());
        assertEquals(0.75, mechanic.getChance());
        assertEquals(80, mechanic.getDuration());
        assertEquals(1.25, mechanic.getDamagePerTick());
        assertEquals(10, mechanic.getTickInterval());
    }
}
