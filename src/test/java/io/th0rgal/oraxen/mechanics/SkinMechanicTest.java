package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.skin.SkinMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinMechanicTest extends MechanicTestSupport {

    @Test
    void readsConsumeFlag() {
        SkinMechanic mechanic = new SkinMechanic(mechanicFactory(), mechanicSection("skin", "consume", true));

        assertTrue(mechanic.doConsume());
    }
}
