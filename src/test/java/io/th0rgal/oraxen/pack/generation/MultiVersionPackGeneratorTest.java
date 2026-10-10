package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.HashUtils;
import io.th0rgal.oraxen.utils.MinecraftVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiVersionPackGeneratorTest {

    @TempDir
    File tempDir;

    @Test
    void generatedCoreShaderFilterMatchesMaterializedShaderPath() throws Exception {
        byte[] content = "generated shader".getBytes(StandardCharsets.UTF_8);
        String materializedPath = "assets/minecraft/shaders/core/rendertype_text.vsh";
        MultiVersionPackGenerator generator = new MultiVersionPackGenerator(tempDir,
                Map.of(materializedPath, bytesToHex(MessageDigest.getInstance("SHA-256").digest(content))));

        Method method = MultiVersionPackGenerator.class.getDeclaredMethod(
                "shouldExcludeGeneratedCoreShader", MinecraftVersion.class, String.class, byte[].class);
        method.setAccessible(true);

        boolean excluded = (boolean) method.invoke(generator, new MinecraftVersion("1.20.2"), materializedPath, content);

        assertTrue(excluded);
    }

    @Test
    void generatedCoreShaderFilterRemovesOverlayPathsForLegacyTargets() throws Exception {
        byte[] content = "generated shader".getBytes(StandardCharsets.UTF_8);
        String overlayPath = "overlay_1_21_4/assets/minecraft/shaders/core/rendertype_text.vsh";
        MultiVersionPackGenerator generator = new MultiVersionPackGenerator(tempDir,
                Map.of(overlayPath, bytesToHex(MessageDigest.getInstance("SHA-256").digest(content))));

        Method method = MultiVersionPackGenerator.class.getDeclaredMethod(
                "shouldExcludeGeneratedCoreShader", MinecraftVersion.class, String.class, byte[].class);
        method.setAccessible(true);

        boolean excluded = (boolean) method.invoke(generator, new MinecraftVersion("1.20.2"), overlayPath, content);

        assertTrue(excluded);
    }

    private String bytesToHex(byte[] bytes) {
        return HashUtils.bytesToHex(bytes);
    }
}
