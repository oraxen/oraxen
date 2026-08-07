package io.th0rgal.oraxen.pack.dispatch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackSenderTest {

    @Test
    void preJoinRequiresPaperAndSupportedVersion() {
        assertTrue(PackSender.isPreJoinSupported(true, true));
        assertFalse(PackSender.isPreJoinSupported(false, true));
        assertFalse(PackSender.isPreJoinSupported(true, false));
        assertFalse(PackSender.isPreJoinSupported(false, false));
    }
}
