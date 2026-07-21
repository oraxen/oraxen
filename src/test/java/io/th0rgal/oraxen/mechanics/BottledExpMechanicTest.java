package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.farming.bottledexp.BottledExpMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BottledExpMechanicTest extends MechanicTestSupport {

    @Test
    void convertsPlayerExperienceToBottleCount() {
        BottledExpMechanic mechanic = new BottledExpMechanic(mechanicFactory(), mechanicSection("bottledexp", "ratio", 1.0));

        assertTrue(mechanic.getBottleEquivalent(10, 0.5f) > 0);
    }
}
