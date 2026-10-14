package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.consumablepotioneffects.ConsumablePotionEffectsMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumablePotionEffectsMechanicTest extends MechanicTestSupport {

    @Test
    void storesBaseMechanicDataForEmptyConfig() {
        ConsumablePotionEffectsMechanic mechanic = new ConsumablePotionEffectsMechanic(mechanicFactory(), mechanicSection("consumable_potion_effects"));

        assertEquals("test_item", mechanic.getItemID());
    }
}
