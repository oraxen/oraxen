package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FurnitureBedTest {

    @Test
    void defaultsToVanillaBedBehavior() {
        assertEquals(new FurnitureBed(0, 0.5, 0.3, true, true, true, false), FurnitureBed.parse("0,0.5,0.3"));
    }

    @Test
    void parsesOptionsInOrder() {
        assertEquals(new FurnitureBed(1, 0.5, 0.3, false, true, false, true),
                FurnitureBed.parse(" 1,0.5,0.3  false TRUE false true "));
    }

    @Test
    void keepsDefaultsForOmittedOptions() {
        assertEquals(new FurnitureBed(1, 0, 0, true, false, true, false), FurnitureBed.parse("1,0,0 true false"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0,0", "0,0,0,0", "0,0,NaN", "0,0,0 yes", "0,0,0 1", "0,0,0 true true true true true"})
    void rejectsInvalidEntries(String entry) {
        assertNull(FurnitureBed.parse(entry));
    }
}
