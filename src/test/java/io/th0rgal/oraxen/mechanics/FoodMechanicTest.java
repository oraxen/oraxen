package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.food.FoodMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FoodMechanicTest extends MechanicTestSupport {

    @Test
    void readsFoodValuesWithoutOptionalReplacementOrEffects() {
        FoodMechanic mechanic = new FoodMechanic(mechanicFactory(), mechanicSection("food", "hunger", 6, "saturation", 4, "effect_probability", 1.5));

        assertEquals(6, mechanic.getHunger());
        assertEquals(4, mechanic.getSaturation());
        assertEquals(1.0, mechanic.getEffectProbability());
        assertFalse(mechanic.hasReplacement());
        assertFalse(mechanic.hasEffects());
    }
}
