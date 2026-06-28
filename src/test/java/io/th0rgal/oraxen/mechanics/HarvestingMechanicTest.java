package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.farming.harvesting.HarvestingMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HarvestingMechanicTest extends MechanicTestSupport {

    @Test
    void readsHarvestingAreaAndDurabilityFlag() {
        HarvestingMechanic mechanic = new HarvestingMechanic(mechanicFactory(), mechanicSection("harvesting", "radius", 3, "height", 2, "lower_item_durability", false, "cooldown", 5));

        assertEquals(3, mechanic.getRadius());
        assertEquals(2, mechanic.getHeight());
        assertFalse(mechanic.shouldLowerItemDurability());
    }
}
