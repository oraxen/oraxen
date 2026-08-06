package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.MinecraftVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextShaderTargetTest {

    @Test
    void unifiedTextShaderIsSelectedByPackFormat() {
        assertTrue(new TextShaderTarget(88, new MinecraftVersion("26.1.2")).usesUnifiedTextShader());
        assertFalse(new TextShaderTarget(84, new MinecraftVersion("26.2")).usesUnifiedTextShader());
    }

    @Test
    void resolvesPackFormatFromSharedMapping() {
        assertEquals(64, TextShaderTarget.forVersion("1.21.7").packFormat());
        assertEquals(88, TextShaderTarget.forVersion("26.2").packFormat());
    }
}
