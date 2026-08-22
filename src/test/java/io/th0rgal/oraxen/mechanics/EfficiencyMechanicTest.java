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

    @Test
    void positiveAmountSpeedsUpMiningLikeHaste() {
        try (MockedStatic<PotionUtils> potionUtils = mockStatic(PotionUtils.class)) {
            potionUtils.when(() -> PotionUtils.getEffectType("haste")).thenReturn(null);

            EfficiencyMechanic mechanic = new EfficiencyMechanic(mechanicFactory(), mechanicSection("efficiency", "amount", 3));

            assertEquals(1.6D, mechanic.getMiningSpeedMultiplier(), 1.0E-9D);
        }
    }

    @Test
    void negativeAmountSlowsDownMiningLikeMiningFatigue() {
        try (MockedStatic<PotionUtils> potionUtils = mockStatic(PotionUtils.class)) {
            potionUtils.when(() -> PotionUtils.getEffectType("mining_fatigue")).thenReturn(null);

            EfficiencyMechanic levelTwo = new EfficiencyMechanic(
                    mechanicFactory(), mechanicSection("efficiency", "amount", -2));
            EfficiencyMechanic levelThree = new EfficiencyMechanic(
                    mechanicFactory(), mechanicSection("efficiency", "amount", -3));
            EfficiencyMechanic levelFour = new EfficiencyMechanic(
                    mechanicFactory(), mechanicSection("efficiency", "amount", -4));

            assertEquals(2, levelTwo.getAmount());
            assertEquals(0.09D, levelTwo.getMiningSpeedMultiplier(), 1.0E-9D);
            assertEquals(0.0027D, levelThree.getMiningSpeedMultiplier(), 1.0E-9D);
            assertEquals(0.00081D, levelFour.getMiningSpeedMultiplier(), 1.0E-9D);
            potionUtils.verify(() -> PotionUtils.getEffectType("mining_fatigue"), org.mockito.Mockito.times(3));
        }
    }
}
