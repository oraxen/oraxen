package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.lifeleech.LifeLeechMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifeLeechMechanicTest extends MechanicTestSupport {

    @Test
    void readsAmount() {
        LifeLeechMechanic mechanic = new LifeLeechMechanic(mechanicFactory(), mechanicSection("lifeleech", "amount", 6));

        assertEquals(6, mechanic.getAmount());
    }
}
