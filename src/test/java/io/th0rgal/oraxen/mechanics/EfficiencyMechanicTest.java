package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency.EfficiencyMechanic;
import io.th0rgal.oraxen.utils.PotionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

class EfficiencyMechanicTest extends MechanicTestSupport {

    @Test
    void positiveAmountUsesHasteLookup() {
        try (MockedStatic<PotionUtils> potionUtils = mockStatic(PotionUtils.class)) {
            potionUtils.when(() -> PotionUtils.getEffectType("haste")).thenReturn(null);

            EfficiencyMechanic mechanic = new EfficiencyMechanic(mechanicFactory(), mechanicSection("efficiency", "amount", 2));

            assertEquals(2, mechanic.getAmount());
            assertNull(mechanic.getType());
            potionUtils.verify(() -> PotionUtils.getEffectType("haste"));
        }
    }
}
