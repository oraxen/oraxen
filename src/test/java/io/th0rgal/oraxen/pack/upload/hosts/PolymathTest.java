package io.th0rgal.oraxen.pack.upload.hosts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PolymathTest {

    @Test
    void normalizePackUrl_rewritesLegacyAtlasHttpUrlsToHttps() {
        assertEquals(
                "https://atlas.oraxen.com/pack.zip?id=abc123",
                Polymath.normalizePackUrl("http://atlas.oraxen.com:8080/pack.zip?id=abc123")
        );
        assertEquals(
                "https://atlas.oraxen.com/pack.zip?id=abc123",
                Polymath.normalizePackUrl("http://atlas.oraxen.com/pack.zip?id=abc123")
        );
        assertEquals(
                "https://atlas.oraxen.com/pack.zip?id=abc123",
                Polymath.normalizePackUrl("https://atlas.oraxen.com/pack.zip?id=abc123")
        );
        assertEquals(
                "https://custom.example.com:8080/pack.zip?id=abc123",
                Polymath.normalizePackUrl("https://custom.example.com:8080/pack.zip?id=abc123")
        );
        assertNull(Polymath.normalizePackUrl(null));
    }
}
