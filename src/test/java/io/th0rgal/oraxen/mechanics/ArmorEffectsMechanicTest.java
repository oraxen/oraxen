package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.armor_effects.ArmorEffectsMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorEffectsMechanicTest extends MechanicTestSupport {

    @Test
    void emptyConfigCreatesNoArmorEffects() {
        ArmorEffectsMechanic mechanic = new ArmorEffectsMechanic(mechanicFactory(), mechanicSection("armor_effects"));

        assertTrue(mechanic.getArmorEffects().isEmpty());
    }
}
