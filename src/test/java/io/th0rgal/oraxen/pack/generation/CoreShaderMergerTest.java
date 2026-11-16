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

    @Test void mergesGeneratedConstantsAndRejectsConflictingInitializers() {
        String generated = "#version 150\nconst bool ORAXEN_ANIMATED_GLYPHS = true;\n"
                + "const int ORAXEN_ANIM_CONFIG_COUNT = 1;\n"
                + "const ivec2 ORAXEN_ANIM_CONFIGS[1] = ivec2[](ivec2(10, 2));\n"
                + "void main() { float color = 1.0; }";
        String merged = merge(generated, "#version 150\nvoid main() { float color = 1.0; color *= 0.5; }");
        assertTrue(merged.contains("const ivec2 ORAXEN_ANIM_CONFIGS[1] = ivec2[](ivec2(10, 2));"));
        assertTrue(merged.contains("color *= 0.5;"));
        assertThrows(IllegalArgumentException.class, () -> merge(generated, generated.replace("= true", "= false")));
    }

    @Test void rejectsMultipleGlobalDeclaratorsButAllowsConstructorCommas() {
        String low = "#version 150\nconst float a = 1.0, b = 2.0;\nvoid main() {}";
        String high = "#version 150\nconst float b = 3.0;\nvoid main() {}";
        assertThrows(IllegalArgumentException.class, () -> merge(low, high));
        assertThrows(IllegalArgumentException.class, () -> merge(high, low));
        String constructor = "#version 150\nconst vec2 pair = vec2(1.0, 2.0);\nvoid main() {}";
        assertTrue(merge(constructor, high).contains("const vec2 pair = vec2(1.0, 2.0);"));
    }

    @Test void rejectsUnbracedConditionalsAndUnsafeLocalBindings() {
        String base = "#version 150\nuniform float scale;\nvoid main() { scaleOutput(); }";
        for (String body : new String[] {
                "if (true) scaleOutput(); else otherOutput();",
                "if (true) {} else otherOutput();",
                "float a = 1.0, b = 2.0;",
                "float scale = 1.0;",
                "if (true) { float scale = 1.0; }"}) {
            assertThrows(IllegalArgumentException.class, () -> merge(base,
                    "#version 150\nuniform float scale;\nvoid main() { " + body + " }"), body);
        }
        assertThrows(IllegalArgumentException.class, () -> merge(
                "#version 150\nvoid main() { float a = 1.0; }",
                "#version 150\nvoid main() { float a = 1.0, b = 2.0; }"));
    }

    @Test void rejectsReorderedSharedEffectsInsteadOfExecutingThemTwice() {
        assertThrows(IllegalArgumentException.class, () -> merge(
                "#version 150\nvoid main() { color *= 0.5; color += 0.1; }",
                "#version 150\nvoid main() { color += 0.1; color *= 0.5; }"));
    }

    @Test void boundsMergeWorkBeforeAllocatingStatementMatrix() {
        String many = "#version 150\nvoid main() {" + "color *= 0.5;".repeat(600) + "}";
        assertThrows(IllegalArgumentException.class, () -> merge(many, many.replace("0.5", "0.6")));
        assertThrows(IllegalArgumentException.class, () -> merge(many, " ".repeat(262_145)));
    }

    private static String merge(String low, String high) {
        return new String(CoreShaderMerger.merge("assets/minecraft/shaders/core/test.fsh", bytes(low), bytes(high)), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
