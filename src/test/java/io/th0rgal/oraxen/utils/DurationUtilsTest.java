package io.th0rgal.oraxen.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurationUtilsTest {

    @Test
    void parsesEachUnit() {
        assertEquals(80L, DurationUtils.parseMillis("80ms").orElseThrow());
        assertEquals(2_000L, DurationUtils.parseMillis("2s").orElseThrow());
        assertEquals(180_000L, DurationUtils.parseMillis("3m").orElseThrow());
        assertEquals(3_600_000L, DurationUtils.parseMillis("1h").orElseThrow());
        assertEquals(172_800_000L, DurationUtils.parseMillis("2d").orElseThrow());
        assertEquals(2_000L, DurationUtils.parseMillis(" 2 S ").orElseThrow());
        assertEquals(0L, DurationUtils.parseMillis("0s").orElseThrow());
    }

    @Test
    void rejectsInvalidDurations() {
        assertTrue(DurationUtils.parseMillis(null).isEmpty());
        for (String value : new String[]{"", " ", "5", "1.5s", "-1s", "3w", "s",
                "9999999999999999999d", "9223372036854775807d"}) {
            assertTrue(DurationUtils.parseMillis(value).isEmpty(), value);
        }
    }
}
