package io.th0rgal.oraxen.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogDumpCommandTest {

    @Test
    void redactSensitiveLogContent_redactsPackUrlAndStudioSecrets() {
        String raw = "[Oraxen] Resourcepack uploaded to http://atlas.oraxen.com:8080/pack.zip?id=abc123\n"
                + "[Oraxen] Token: oxn_studio_eyJ2IjoxfQ.sig12345 and customSecretValue999";

        String redacted = LogDumpCommand.redactSensitiveLogContent(raw, "customSecretValue999");

        assertFalse(redacted.contains("atlas.oraxen.com"));
        assertFalse(redacted.contains("oxn_studio_eyJ2IjoxfQ.sig12345"));
        assertFalse(redacted.contains("customSecretValue999"));
        assertTrue(redacted.contains("*redacted pack url*"));
        assertTrue(redacted.contains("*redacted studio secret*"));
    }
}
