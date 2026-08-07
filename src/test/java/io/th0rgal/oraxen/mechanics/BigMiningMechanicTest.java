package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.farming.bigmining.BigMiningMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BigMiningMechanicTest extends MechanicTestSupport {

    @Test
    void readsRadiusAndDepth() {
        BigMiningMechanic mechanic = new BigMiningMechanic(mechanicFactory(), mechanicSection("bigmining", "radius", 2, "depth", 4));

        assertEquals(2, mechanic.getRadius());
        assertEquals(4, mechanic.getDepth());
    }
}
