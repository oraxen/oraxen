package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.farming.harvesting.HarvestingMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HarvestingMechanicTest extends MechanicTestSupport {

    @Test
    void readsHarvestingConfiguration() {
        HarvestingMechanic mechanic = new HarvestingMechanic(mechanicFactory(),
                mechanicSection("harvesting", "cooldown", 10_000, "radius", 5, "height", 3));

        assertEquals(5, mechanic.getRadius());
        assertEquals(3, mechanic.getHeight());
    }
}
