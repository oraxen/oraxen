package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FurnitureSleepStateTest {

    private static final FurnitureBeds.SleepState awake = new FurnitureBeds.SleepState(false, false, false, false, false);
    private static final FurnitureBeds.SleepState inFurnitureBed = new FurnitureBeds.SleepState(false, false, false, true, true);
    private static final FurnitureBeds.SleepState shallowInFurnitureBed = new FurnitureBeds.SleepState(false, false, false, true, false);
    private static final FurnitureBeds.SleepState inVanillaBed = new FurnitureBeds.SleepState(false, true, false, false, false);
    private static final FurnitureBeds.SleepState deepInVanillaBed = new FurnitureBeds.SleepState(false, true, true, false, false);
    private static final FurnitureBeds.SleepState spectator = new FurnitureBeds.SleepState(true, false, false, false, false);

    @Test
    void skipsWhenEveryPlayerLiesInAFurnitureBed() {
        assertTrue(FurnitureBeds.SleepState.enoughSleeping(List.of(inFurnitureBed, inFurnitureBed), 100));
    }

    @Test
    void waitsForAwakePlayersAtTheDefaultPercentage() {
        assertFalse(FurnitureBeds.SleepState.enoughSleeping(List.of(inFurnitureBed, awake), 100));
    }

    @Test
    void usesTheSleepingPercentage() {
        assertTrue(FurnitureBeds.SleepState.enoughSleeping(List.of(inFurnitureBed, awake), 50));
    }

    @Test
    void ignoresSpectatorsAndPlayersThatLeft() {
        assertTrue(FurnitureBeds.SleepState.enoughSleeping(Arrays.asList(inFurnitureBed, spectator, null), 100));
    }

    @Test
    void waitsForVanillaSleepersToFallAsleep() {
        assertFalse(FurnitureBeds.SleepState.enoughSleeping(List.of(inFurnitureBed, inVanillaBed), 100));
    }

    @Test
    void waitsForEachFurnitureSleeperToFallAsleep() {
        assertFalse(FurnitureBeds.SleepState.enoughSleeping(List.of(inFurnitureBed, shallowInFurnitureBed), 100));
    }

    @Test
    void leavesTheSkipToTheServerOnceAVanillaSleeperIsDeepAsleep() {
        assertFalse(FurnitureBeds.SleepState.enoughSleeping(List.of(inFurnitureBed, deepInVanillaBed), 100));
    }
}
