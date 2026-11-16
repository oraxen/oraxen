package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CoreShaderMergerTest {
    @Test void composesSharedVanillaStatementsAndPackEditsAcrossThreePacks() {
        String low = "#version 150\n#moj_import <fog.glsl>\nout vec4 color;\nvoid main() { color = vec4(1.0); color.r *= 0.5; }";
        String high = "#version 150\n#moj_import <fog.glsl>\nout vec4 color;\nvoid main() { color = vec4(1.0); color *= 0.5; }";
        String merged = merge(low, high);
        assertEquals(1, merged.split("out vec4 color;", -1).length - 1);
        assertEquals(1, merged.split("#moj_import", -1).length - 1);
        assertEquals(merged.indexOf("color = vec4(1.0);"), merged.lastIndexOf("color = vec4(1.0);"));
        assertTrue(merged.indexOf("color.r *= 0.5;") < merged.indexOf("color *= 0.5;"));
        String third = merge(merged, high);
        assertEquals(third.indexOf("color *= 0.5;"), third.lastIndexOf("color *= 0.5;"));
        assertEquals(1, third.split("void main\\(", -1).length - 1);
    }

    @Test void rejectsConflictingGlobalsAndUnsupportedPreprocessing() {
        String low = "#version 150\nuniform float value;\nvoid main() {}";
        assertThrows(IllegalArgumentException.class, () -> merge(low,
                "#version 150\nuniform vec2 value;\nvoid main() {}"));
        assertThrows(IllegalArgumentException.class, () -> merge(low,
                "#version 150\n#ifdef FOO\n#endif\nvoid main() {}"));
        assertThrows(IllegalArgumentException.class, () -> merge(low, "#version 330\nvoid main() {}"));
    }

    @Test void rejectsConflictingLocalInitializersAndEarlyReturns() {
        String low = "#version 150\nvoid main() { float scale = 1.0; }";
        assertThrows(IllegalArgumentException.class, () -> merge(low,
                "#version 150\nvoid main() { float scale = 2.0; }"));
        assertThrows(IllegalArgumentException.class, () -> merge(low,
                "#version 150\nvoid main() { if (true) { return; } }"));
    }

    @Test void mergesCompatibleProgramBindingsButRejectsConflictingUniforms() {
        String low = "{\"vertex\":\"test\",\"samplers\":[{\"name\":\"Sampler0\"}],\"uniforms\":[{\"name\":\"Scale\",\"values\":[1]}]}";
        String high = "{\"vertex\":\"test\",\"samplers\":[{\"name\":\"Sampler1\"}],\"uniforms\":[{\"name\":\"Scale\",\"values\":[1]}]}";
        var program = JsonParser.parseString(new String(CoreShaderMerger.merge(
                "assets/minecraft/shaders/core/test.json", bytes(low), bytes(high)), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(2, program.getAsJsonArray("samplers").size());
        assertEquals(1, program.getAsJsonArray("uniforms").size());
        assertThrows(IllegalArgumentException.class, () -> CoreShaderMerger.merge(
                "assets/minecraft/shaders/core/test.json", bytes(low), bytes(high.replace("[1]", "[2]"))));
    }

    private static String merge(String low, String high) {
        return new String(CoreShaderMerger.merge("assets/minecraft/shaders/core/test.fsh", bytes(low), bytes(high)), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
