package io.th0rgal.oraxen.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void parsesSupportedUnits() {
        assertEquals(TimeUnit.SECONDS.toMillis(30), UpdateChecker.parseIntervalMillis("30s"));
        assertEquals(TimeUnit.MINUTES.toMillis(15), UpdateChecker.parseIntervalMillis("15M"));
        assertEquals(TimeUnit.HOURS.toMillis(24), UpdateChecker.parseIntervalMillis(" 24h "));
        assertEquals(TimeUnit.DAYS.toMillis(2), UpdateChecker.parseIntervalMillis("2d"));
    }

    @Test
    void rejectsInvalidIntervals() {
        assertEquals(-1, UpdateChecker.parseIntervalMillis("24"));
        assertEquals(-1, UpdateChecker.parseIntervalMillis("null"));
        assertEquals(-1, UpdateChecker.parseIntervalMillis("0h"));
        assertEquals(-1, UpdateChecker.parseIntervalMillis("400d"));
        assertEquals(-1, UpdateChecker.parseIntervalMillis("99999999999999999999h"));
    }

    @Test
    void validIntervalsAboveMinimumAreKept() {
        assertEquals(TimeUnit.HOURS.toMillis(24), UpdateChecker.intervalMillis("24h"));
        assertEquals(UpdateChecker.MIN_INTERVAL, UpdateChecker.intervalMillis("10m"));
        assertTrue(UpdateChecker.MIN_INTERVAL <= UpdateChecker.DEFAULT_INTERVAL);
    }

    @Test
    void comparesVersions() {
        assertTrue(UpdateChecker.compareVersions("v1.200.0", "1.199.1") > 0);
        assertEquals(0, UpdateChecker.compareVersions("1.199.0", "1.199"));
    }
}
