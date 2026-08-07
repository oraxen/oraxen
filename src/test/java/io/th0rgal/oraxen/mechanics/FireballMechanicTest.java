package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.spell.fireball.FireballMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FireballMechanicTest extends MechanicTestSupport {

    @Test
    void readsYieldAndSpeed() {
        FireballMechanic mechanic = new FireballMechanic(mechanicFactory(), mechanicSection("fireball", "yield", 2.5, "speed", 1.75));

        assertEquals(2.5, mechanic.getYield());
        assertEquals(1.75, mechanic.getSpeed());
    }
}
