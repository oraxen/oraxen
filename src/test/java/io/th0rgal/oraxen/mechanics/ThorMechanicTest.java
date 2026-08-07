package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.spell.thor.ThorMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThorMechanicTest extends MechanicTestSupport {

    @Test
    void readsLightningAmount() {
        ThorMechanic mechanic = new ThorMechanic(mechanicFactory(), mechanicSection("thor", "lightning_bolts_amount", 3, "random_location_variation", 1.5, "delay", 20L));

        assertEquals(3, mechanic.getLightningBoltsAmount());
    }
}
