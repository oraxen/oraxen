package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.soulbound.SoulBoundMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoulBoundMechanicTest extends MechanicTestSupport {

    @Test
    void readsLoseChance() {
        SoulBoundMechanic mechanic = new SoulBoundMechanic(mechanicFactory(), mechanicSection("soulbound", "lose_chance", 0.25));

        assertEquals(0.25, mechanic.getLoseChance());
    }
}
