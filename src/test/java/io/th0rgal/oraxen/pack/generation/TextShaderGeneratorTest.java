package io.th0rgal.oraxen.pack.generation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextShaderGeneratorTest {

    private final TextShaderGenerator generator = new TextShaderGenerator();

    @Test
    void shaderCVertexUsesVanillaLocationsAndSharedAnimationLogic() {
        String shader = generator.getAnimationVertexShader26_3(
                new TextShaderGenerator.TextShaderFeatures(false));

        assertTrue(shader.contains("#extension GL_ARB_separate_shader_objects : require"));
        assertTrue(shader.contains("#include <minecraft:globals.glsl>"));
        assertFalse(shader.contains("#moj_import"));
        assertTrue(shader.contains("layout(location = 0) in vec3 Position;"));
        assertTrue(shader.contains("layout(location = 3) in ivec2 UV2;"));
        assertTrue(shader.contains("layout(location = 2) out vec4 vertexColor;"));
        assertTrue(shader.contains("layout(location = 3) out vec2 texCoord0;"));
        assertTrue(shader.contains("sample_lightmap(Sampler2, UV2)"));
        assertTrue(shader.contains("#if defined(IS_SEE_THROUGH) || defined(IS_GUI)"));
        assertTrue(shader.contains("int currentFrame = loop ? (rawFrame % totalFrames)"));
        assertTrue(shader.contains("oraxen_lit_text_color(vec4(1.0, 1.0, 1.0, visible))"));
    }

    @Test
    void shaderCFragmentPreservesVanillaTransparencyAndGrayscalePasses() {
        String shader = generator.getAnimationFragmentShader26_3();

        assertFalse(shader.contains("#moj_import"));
        assertTrue(shader.contains("#include <minecraft:oit.glsl>"));
        assertTrue(shader.contains("layout(location = 2) in vec4 vertexColor;"));
        assertTrue(shader.contains("layout(location = 3) in vec2 texCoord0;"));
        assertTrue(shader.contains("#ifndef OIT_ALPHA_ONLY\nlayout(location = 0) out vec4 fragColor;"));
        assertTrue(shader.contains("executeAlphaOnlyPhase(gl_FragCoord.z, color.a)"));
        assertTrue(shader.contains("sampleColorForAccumulation(color)"));
        assertTrue(shader.contains("vec4(FogColor.rgb * color.a, FogColor.a)"));
        assertTrue(shader.contains("#ifdef IS_GRAYSCALE"));
        assertTrue(shader.contains("texture(Sampler0, texCoord0).rrrr"));
        assertTrue(shader.contains("if (color.a < 0.1)"));
    }

    @Test
    void unifiedTextShaderServersKeepTheirShadersInAnOverlay() {
        for (String version : new String[]{"26.2", "26.3"}) {
            TextShaderTarget target = TextShaderTarget.forVersion(version);
            ShaderOverlay overlay = ShaderOverlay.forPackFormat(target.packFormat());
            assertNotNull(overlay, version);
            assertTrue(TextShaderGenerator.keepsServerShadersInOverlay(target, overlay, false), version);
        }
    }

    @Test
    void olderServersKeepTheirShadersInTheBase() {
        for (String version : new String[]{"1.21.4", "1.21.11", "26.1.2"}) {
            TextShaderTarget target = TextShaderTarget.forVersion(version);
            ShaderOverlay overlay = ShaderOverlay.forPackFormat(target.packFormat());
            assertFalse(TextShaderGenerator.keepsServerShadersInOverlay(target, overlay, false), version);
            assertTrue(TextShaderGenerator.keepsServerShadersInOverlay(target, overlay, true), version);
        }
    }
}
