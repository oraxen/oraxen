package io.th0rgal.oraxen.utils;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

    private static final float STEP = 0.05f;
    private static final float DELTA = 0.00001f;

    @Test
    void customRoundRoundsValuesOnBothSidesOfAStepBoundary() {
        assertEquals(0.1f, Utils.customRound(0.124, STEP), DELTA);
        assertEquals(0.15f, Utils.customRound(0.126, STEP), DELTA);
    }

    @Test
    void customRoundKeepsIntermediateBowPullStagesOnTheirNearestThreshold() {
        assertEquals(0.25f, Utils.customRound(1f / 4, STEP), DELTA);
        assertEquals(0.5f, Utils.customRound(2f / 4, STEP), DELTA);
        assertEquals(0.75f, Utils.customRound(3f / 4, STEP), DELTA);
        assertEquals(0.3f, Utils.customRound(2f / 7, STEP), DELTA);
        assertEquals(0.45f, Utils.customRound(3f / 7, STEP), DELTA);
    }

    @Test
    void customRoundDoesNotDependOnTheDefaultLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));
            assertEquals(0.15f, Utils.customRound(0.14, STEP), DELTA);
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
