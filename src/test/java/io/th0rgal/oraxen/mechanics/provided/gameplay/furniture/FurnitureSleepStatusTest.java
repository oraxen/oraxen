package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FurnitureSleepStatusTest {

    private static final FurnitureSleepStatus.State awake = new FurnitureSleepStatus.State(false, false, false, false);
    private static final FurnitureSleepStatus.State inFurnitureBed = new FurnitureSleepStatus.State(false, false, false, true);
    private static final FurnitureSleepStatus.State inVanillaBed = new FurnitureSleepStatus.State(false, true, false, false);
    private static final FurnitureSleepStatus.State deepInVanillaBed = new FurnitureSleepStatus.State(false, true, true, false);
    private static final FurnitureSleepStatus.State spectator = new FurnitureSleepStatus.State(true, false, false, false);

    @Test
    void skipsWhenEveryPlayerLiesInAFurnitureBed() {
        assertTrue(FurnitureSleepStatus.enoughSleeping(List.of(inFurnitureBed, inFurnitureBed), 100));
    }

    @Test
    void waitsForAwakePlayersAtTheDefaultPercentage() {
        assertFalse(FurnitureSleepStatus.enoughSleeping(List.of(inFurnitureBed, awake), 100));
    }

    @Test
    void usesTheSleepingPercentage() {
        assertTrue(FurnitureSleepStatus.enoughSleeping(List.of(inFurnitureBed, awake), 50));
    }

    @Test
    void ignoresSpectatorsAndPlayersThatLeft() {
        assertTrue(FurnitureSleepStatus.enoughSleeping(Arrays.asList(inFurnitureBed, spectator, null), 100));
    }

    @Test
    void waitsForVanillaSleepersToFallAsleep() {
        assertFalse(FurnitureSleepStatus.enoughSleeping(List.of(inFurnitureBed, inVanillaBed), 100));
    }

    @Test
    void leavesTheSkipToTheServerOnceAVanillaSleeperIsDeepAsleep() {
        assertFalse(FurnitureSleepStatus.enoughSleeping(List.of(inFurnitureBed, deepInVanillaBed), 100));
    }
}
