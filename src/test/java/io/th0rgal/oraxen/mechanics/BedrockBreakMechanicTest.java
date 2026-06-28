package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.farming.bedrockbreak.BedrockBreakMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockBreakMechanicTest extends MechanicTestSupport {

    @Test
    void readsPeriodFromHardness() {
        BedrockBreakMechanic mechanic = new BedrockBreakMechanic(mechanicFactory(), mechanicSection("bedrockbreak", "delay", 10L, "hardness", 40L, "probability", 0.5));

        assertEquals(40L, mechanic.getPeriod());
    }
}
