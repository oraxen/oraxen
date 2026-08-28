package io.th0rgal.oraxen.pack.dispatch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackSenderTest {

    @Test
    void preJoinRequiresSupportedVersion() {
        assertTrue(PackSender.isPreJoinSupported(true));
        assertFalse(PackSender.isPreJoinSupported(false));
    }
}
