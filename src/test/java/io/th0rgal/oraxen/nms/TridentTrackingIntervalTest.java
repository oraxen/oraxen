package io.th0rgal.oraxen.nms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TridentTrackingIntervalTest {
    private static final class LegacyTracker {
        private final int updateInterval;

        private LegacyTracker(int interval) { updateInterval = interval; }
        int interval() { return updateInterval; }
    }

    public interface UpdateInterval {
        boolean test(int tick);

        static UpdateInterval periodic(int ticks) { return tick -> tick % ticks == 0; }
    }

    private static final class Tracker {
        private final UpdateInterval updateInterval;

        private Tracker(int interval) { updateInterval = UpdateInterval.periodic(interval); }
        boolean updatesAt(int tick) { return updateInterval.test(tick); }
    }

    @Test
    void updatesLegacyTrackerEveryTickWithoutChangingOtherProjectiles() throws ReflectiveOperationException {
        LegacyTracker custom = new LegacyTracker(20);
        LegacyTracker vanilla = new LegacyTracker(20);
        TridentTrackingInterval.everyTick(custom);
        assertEquals(1, custom.interval());
        assertEquals(20, vanilla.interval());
    }

    @Test
    void updatesModernTrackerEveryTickWithoutChangingOtherProjectiles() throws ReflectiveOperationException {
        Tracker custom = new Tracker(20);
        Tracker vanilla = new Tracker(20);
        TridentTrackingInterval.everyTick(custom);
        for (int tick = 1; tick < 40; tick++) {
            assertTrue(custom.updatesAt(tick));
            assertEquals(tick % 20 == 0, vanilla.updatesAt(tick));
        }
    }

    @Test
    void appliesCachedAccessToReplacementTrackers() throws ReflectiveOperationException {
        LegacyTracker first = new LegacyTracker(20);
        LegacyTracker replacement = new LegacyTracker(20);
        TridentTrackingInterval.everyTick(first);
        TridentTrackingInterval.everyTick(replacement);
        assertEquals(1, replacement.interval());
        Tracker modern = new Tracker(20);
        TridentTrackingInterval.everyTick(modern);
        TridentTrackingInterval.everyTick(modern);
        assertTrue(modern.updatesAt(1));
    }
}
