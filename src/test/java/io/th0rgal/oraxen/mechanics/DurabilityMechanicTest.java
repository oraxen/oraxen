package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurabilityMechanicTest extends MechanicTestSupport {

    @Test
    void readsDurabilityAndAddsItemModifier() {
        DurabilityMechanic mechanic = new DurabilityMechanic(mechanicFactory(), mechanicSection("durability", "value", 128));

        assertEquals(128, mechanic.getItemMaxDurability());
        assertTrue(mechanic.getItemModifiers().length > 0);
    }
}
