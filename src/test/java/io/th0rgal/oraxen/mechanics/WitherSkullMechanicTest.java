package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.spell.witherskull.WitherSkullMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WitherSkullMechanicTest extends MechanicTestSupport {

    @Test
    void readsChargedFlag() {
        WitherSkullMechanic mechanic = new WitherSkullMechanic(mechanicFactory(), mechanicSection("witherskull", "charged", true, "delay", 20L));

        assertTrue(mechanic.charged);
    }
}
