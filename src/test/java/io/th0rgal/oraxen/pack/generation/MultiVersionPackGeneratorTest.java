package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.HashUtils;
import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MultiVersionPackGeneratorTest {

    @TempDir
    File tempDir;

    @Test
    void generatedCoreShaderFilterMatchesMaterializedShaderPath() throws Exception {
        assertTrue(excludesGeneratedShader("assets/minecraft/shaders/core/rendertype_text.vsh"));
    }

    @Test
    void generatedCoreShaderFilterRemovesOverlayPathsForLegacyTargets() throws Exception {
        assertTrue(excludesGeneratedShader("overlay_1_21_4/assets/minecraft/shaders/core/rendertype_text.vsh"));
    }

    @Test
    void generatesMainAndVersionedZipsAtConfiguredLocation() throws Exception {
        File source = new File(tempDir, "pack");
        Path outputFolder = tempDir.toPath().resolve("web/packs");
        YamlConfiguration settings = new YamlConfiguration();
        settings.set(Settings.PACK_LOCATION.getPath(), outputFolder.resolve("custom.zip").toString());
        settings.set(Settings.COMPRESSION.getPath(), "BEST_COMPRESSION");
        settings.set(Settings.COMMENT.getPath(), "Test pack");
        settings.set(Settings.PACK_SIZE_SPOOFING.getPath(), false);
        settings.set(Settings.GENERATE_MCMETA.getPath(), false);
        String metadata = "{\"pack\":{\"pack_format\":46,\"description\":\"Test pack\"}}";
        List<VirtualFile> files = List.of(new VirtualFile("", "pack.mcmeta",
                () -> new ByteArrayInputStream(metadata.getBytes(StandardCharsets.UTF_8))));
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenReturn(settings);
        try (var plugins = mockStatic(OraxenPlugin.class); var bukkit = mockStatic(Bukkit.class);
             var versions = mockStatic(MinecraftVersion.class); var logs = mockStatic(Logs.class);
             var scheduler = mockStatic(SchedulerUtil.class)) {
            plugins.when(OraxenPlugin::get).thenReturn(plugin);
            PluginManager pluginManager = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            versions.when(MinecraftVersion::getCurrentVersion).thenReturn(new MinecraftVersion("1.21.4"));
            Settings.invalidateCache();

            File destination = PackOutputLocation.resolve(source, Settings.PACK_LOCATION.getValue());
            assertTrue(new MultiVersionPackGenerator(source, destination, Map.of())
                    .generatePacks(files, false, new JsonArray()));
            for (String name : List.of("custom.zip", "custom_1_21_3.zip", "custom_1_21_4.zip")) {
                assertMetadata(outputFolder.resolve(name), metadata);
            }
            assertFalse(new File(source, "pack.zip").exists());

            File firstSource = new File(tempDir, "alternate");
            new MultiVersionPackGenerator(firstSource).generateMultipleVersions(files, false);
            for (String name : List.of("alternate.zip", "pack_1_21_3.zip", "pack_1_21_4.zip")) {
                assertMetadata(firstSource.toPath().resolve(name), metadata);
            }

            File secondSource = new File(tempDir, "other");
            new MultiVersionPackGenerator(secondSource, Map.of()).generateMultipleVersions(files, false, new JsonArray());
            for (String name : List.of("other.zip", "pack_1_21_3.zip", "pack_1_21_4.zip")) {
                assertMetadata(secondSource.toPath().resolve(name), metadata);
            }
        } finally {
            Settings.invalidateCache();
        }
    }

    private static void assertMetadata(Path path, String metadata) throws Exception {
        try (var zip = new ZipFile(path.toFile());
             var entry = zip.getInputStream(zip.getEntry("pack.mcmeta"))) {
            assertEquals(metadata, new String(entry.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private boolean excludesGeneratedShader(String path) throws Exception {
        byte[] content = "generated shader".getBytes(StandardCharsets.UTF_8);
        MultiVersionPackGenerator generator = new MultiVersionPackGenerator(tempDir,
                Map.of(path, HashUtils.bytesToHex(MessageDigest.getInstance("SHA-256").digest(content))));

        Method method = MultiVersionPackGenerator.class.getDeclaredMethod(
                "shouldExcludeGeneratedCoreShader", MinecraftVersion.class, String.class, byte[].class);
        method.setAccessible(true);

        return (boolean) method.invoke(generator, new MinecraftVersion("1.20.2"), path, content);
    }
}
