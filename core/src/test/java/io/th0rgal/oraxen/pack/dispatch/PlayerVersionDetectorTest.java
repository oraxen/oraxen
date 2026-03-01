package io.th0rgal.oraxen.pack.dispatch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerVersionDetectorTest {

    @Test
    void testProtocolToVersionString_1_21_4_andLater() {
        assertEquals("1.21.4", PlayerVersionDetector.protocolToVersionString(769));
        assertEquals("1.21.5", PlayerVersionDetector.protocolToVersionString(770));
        assertEquals("1.21.11", PlayerVersionDetector.protocolToVersionString(774));
        assertEquals("1.21.11+", PlayerVersionDetector.protocolToVersionString(775));
        assertEquals("1.21.11+", PlayerVersionDetector.protocolToVersionString(800));
    }

    @Test
    void testProtocolToVersionString_1_21_2() {
        assertEquals("1.21.2", PlayerVersionDetector.protocolToVersionString(768));
    }

    @Test
    void testProtocolToVersionString_1_21() {
        assertEquals("1.21", PlayerVersionDetector.protocolToVersionString(767));
    }

    @Test
    void testProtocolToVersionString_1_20_5() {
        assertEquals("1.20.5", PlayerVersionDetector.protocolToVersionString(766));
    }

    @Test
    void testProtocolToVersionString_1_20_3() {
        assertEquals("1.20.3", PlayerVersionDetector.protocolToVersionString(765));
    }

    @Test
    void testProtocolToVersionString_1_20_2() {
        assertEquals("1.20.2", PlayerVersionDetector.protocolToVersionString(764));
    }

    @Test
    void testProtocolToVersionString_1_20() {
        assertEquals("1.20", PlayerVersionDetector.protocolToVersionString(763));
    }

    @Test
    void testProtocolToVersionString_1_19_4() {
        assertEquals("1.19.4", PlayerVersionDetector.protocolToVersionString(762));
    }

    @Test
    void testProtocolToVersionString_1_19_3() {
        assertEquals("1.19.3", PlayerVersionDetector.protocolToVersionString(761));
    }

    @Test
    void testProtocolToVersionString_1_19_1() {
        assertEquals("1.19.1", PlayerVersionDetector.protocolToVersionString(760));
    }

    @Test
    void testProtocolToVersionString_1_19() {
        assertEquals("1.19", PlayerVersionDetector.protocolToVersionString(759));
    }

    @Test
    void testProtocolToVersionString_1_18_2() {
        assertEquals("1.18.2", PlayerVersionDetector.protocolToVersionString(758));
    }

    @Test
    void testProtocolToVersionString_1_18() {
        assertEquals("1.18", PlayerVersionDetector.protocolToVersionString(757));
    }

    @Test
    void testProtocolToVersionString_UnknownVersions() {
        String result = PlayerVersionDetector.protocolToVersionString(100);
        assertTrue(result.startsWith("Unknown"), "Protocol 100 should return Unknown, got: " + result);

        result = PlayerVersionDetector.protocolToVersionString(1);
        assertTrue(result.startsWith("Unknown"), "Protocol 1 should return Unknown, got: " + result);
    }

    @Test
    void testProtocolToVersionString_EdgeCases() {
        assertEquals("1.21.2", PlayerVersionDetector.protocolToVersionString(768), "Protocol 768 should be 1.21.2");
        assertEquals("1.21.4", PlayerVersionDetector.protocolToVersionString(769), "Protocol 769 should be 1.21.4");
    }

    @Test
    void testProtocolBoundaries() {
        assertEquals("1.20.3", PlayerVersionDetector.protocolToVersionString(765));
        assertEquals("1.20.2", PlayerVersionDetector.protocolToVersionString(764));
        assertEquals("1.20", PlayerVersionDetector.protocolToVersionString(763));
        
        assertEquals("1.21", PlayerVersionDetector.protocolToVersionString(767));
        assertEquals("1.21.2", PlayerVersionDetector.protocolToVersionString(768));
        assertEquals("1.21.4", PlayerVersionDetector.protocolToVersionString(769));
    }

    @Test
    void testVersionDetectionMethodEnum() {
        PlayerVersionDetector.VersionDetectionMethod[] methods = PlayerVersionDetector.VersionDetectionMethod.values();
        assertEquals(3, methods.length);

        assertEquals(PlayerVersionDetector.VersionDetectionMethod.NONE,
                     PlayerVersionDetector.VersionDetectionMethod.valueOf("NONE"));
        assertEquals(PlayerVersionDetector.VersionDetectionMethod.VIA_VERSION,
                     PlayerVersionDetector.VersionDetectionMethod.valueOf("VIA_VERSION"));
        assertEquals(PlayerVersionDetector.VersionDetectionMethod.PROTOCOL_SUPPORT,
                     PlayerVersionDetector.VersionDetectionMethod.valueOf("PROTOCOL_SUPPORT"));
    }

    @Test
    void testProtocolToVersionStringThreadSafety() throws InterruptedException {
        // Verify protocolToVersionString is safe to call from multiple threads
        int threadCount = 8;
        int iterationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        boolean[] errors = {false};

        for (int t = 0; t < threadCount; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < iterationsPerThread; i++) {
                    try {
                        assertEquals("1.21.4", PlayerVersionDetector.protocolToVersionString(769));
                        assertEquals("1.20.3", PlayerVersionDetector.protocolToVersionString(765));
                        assertEquals("1.21", PlayerVersionDetector.protocolToVersionString(767));
                    } catch (Throwable e) {
                        errors[0] = true;
                    }
                }
            });
        }

        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) thread.join();

        assertFalse(errors[0], "Protocol version lookups should be thread-safe");
    }
}
