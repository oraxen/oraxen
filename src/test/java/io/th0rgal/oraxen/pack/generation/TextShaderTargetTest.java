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
    void shaderCAndOverlayStartAt26_3() {
        assertFalse(TextShaderTarget.forVersion("26.2").usesShaderC());
        assertTrue(TextShaderTarget.forVersion("26.3").usesShaderC());
        assertTrue(TextShaderTarget.forVersion("1.26.3").usesShaderC());
        assertEquals(ShaderOverlay.V26_2, ShaderOverlay.forPackFormat(88));
        assertEquals(ShaderOverlay.V26_2, ShaderOverlay.forPackFormat(96));
        assertEquals(ShaderOverlay.V26_3, ShaderOverlay.forPackFormat(97));
        assertEquals(ShaderOverlay.V26_3, ShaderOverlay.forPackFormat(999));
    }

    @Test
    void resolvesPackFormatFromSharedMapping() {
        assertEquals(64, TextShaderTarget.forVersion("1.21.7").packFormat());
        assertEquals(84, TextShaderTarget.forVersion("26").packFormat());
        assertEquals(88, TextShaderTarget.forVersion("26.2").packFormat());
        assertEquals(97, TextShaderTarget.forVersion("26.3").packFormat());
    }

    @Test
    void preReleasesUseTheirReleaseShaderTarget() {
        assertEquals(97, TextShaderTarget.forVersion("26.3-pre1").packFormat());
        assertTrue(TextShaderTarget.forVersion("26.3-pre1").usesShaderC());
        assertTrue(TextShaderTarget.forVersion("26.3-rc1").usesShaderC());
        assertTrue(TextShaderTarget.forVersion("26.3-pre1").isAtLeast("26.3"));
        assertTrue(TextShaderTarget.forVersion("1.26.3-rc1").isAtLeast("26.3"));
        assertFalse(TextShaderTarget.forVersion("26.2-rc1").isAtLeast("26.3"));
    }
}
