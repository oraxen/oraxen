package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.farming.smelting.SmeltingMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SmeltingMechanicTest extends MechanicTestSupport {

    @Test
    void readsPlaySoundFlag() {
        SmeltingMechanic mechanic = new SmeltingMechanic(mechanicFactory(), mechanicSection("smelting", "play_sound", true));

        assertTrue(mechanic.playSound());
    }
}
