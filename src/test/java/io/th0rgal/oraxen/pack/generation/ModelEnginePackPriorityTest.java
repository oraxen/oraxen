package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@Isolated
class ModelEnginePackPriorityTest {
    @TempDir
    static Path tempDir;

    private final YamlConfiguration settings = new YamlConfiguration();
    private MockedStatic<OraxenPlugin> pluginAccess;
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<Logs> logs;
    private Path modelEnginePack;
    private Path packFolder;
    private Path uploadedPacks;
    private Path uploadsBackupParent;
    private Path uploadsBackup;
    private ResourcePack resourcePack;

    @BeforeEach
    void setUp() throws Exception {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.resolve("oraxen").toFile());
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenReturn(settings);
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        logs = mockStatic(Logs.class);

        resourcePack = new ResourcePack();
        packFolder = resourcePack.getPackFolder().toPath().toAbsolutePath().normalize();
        Files.createDirectories(packFolder);
        uploadedPacks = packFolder.resolve("uploads").normalize();
        if (!uploadedPacks.getParent().equals(packFolder)) throw new IOException("Unexpected uploads directory");
        if (Files.exists(uploadedPacks, LinkOption.NOFOLLOW_LINKS)) {
            uploadsBackupParent = Files.createTempDirectory(packFolder, "priority-test-uploads-");
            uploadsBackup = uploadsBackupParent.resolve("uploads");
            Files.move(uploadedPacks, uploadsBackup);
        }
        Files.createDirectories(uploadedPacks);

        Path modelEngineFolder = tempDir.resolve("modelengine");
        modelEnginePack = modelEngineFolder.resolve("resource pack");
        clearDirectory(modelEnginePack);
        Files.createDirectories(modelEnginePack);
        Plugin modelEngine = mock(Plugin.class);
        when(modelEngine.getDataFolder()).thenReturn(modelEngineFolder.toFile());
        when(modelEngine.isEnabled()).thenReturn(true);
        PluginManager pluginManager = mock(PluginManager.class);
        when(pluginManager.getPlugin("ModelEngine")).thenReturn(modelEngine);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
        bukkit.when(Bukkit::getMinecraftVersion).thenReturn("1.21.4");
        settings.set(Settings.APPEARANCE_MODEL_DATA_IDS.getPath(), false);
        settings.set(Settings.APPEARANCE_MODEL_DATA_FLOAT.getPath(), false);
        Settings.invalidateCache();
    }

    @AfterEach
    void tearDown() throws IOException {
        try {
            deleteTestUpload();
            if (uploadsBackup != null) {
                Files.move(uploadsBackup, uploadedPacks);
                Files.delete(uploadsBackupParent);
            }
        } finally {
            Settings.invalidateCache();
            bukkit.close();
            logs.close();
            pluginAccess.close();
        }
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, false", "true, true"})
    void livePackReplacesUploadedCopiesOnlyWhenImported(boolean importEnabled, boolean emptyLivePack) throws Exception {
        settings.set(Settings.IMPORT_MODEL_ENGINE.getPath(), importEnabled);
        writePack(uploadedPacks.resolve("uploaded"), Map.of(
                "pack.mcmeta", metadata("uploaded"),
                "assets/modelengine/textures/mob.png", "uploaded texture",
                "assets/modelengine/models/mob.json", "{\"source\":\"uploaded base\"}",
                "assets/other/models/uploaded.json", "{\"source\":\"uploaded unique\"}",
                "overlay/assets/modelengine/models/mob.json", "{\"source\":\"uploaded overlay\"}"));
        if (!emptyLivePack) {
            writePack(modelEnginePack, Map.of(
                    "pack.mcmeta", metadata("live"),
                    "assets/modelengine/textures/mob.png", "live texture",
                    "assets/modelengine/models/mob.json", "{\"source\":\"live base\"}",
                    "assets/modelengine/models/live.json", "{\"source\":\"live unique\"}",
                    "overlay/assets/modelengine/models/mob.json", "{\"source\":\"live overlay\"}"));
        }

        List<VirtualFile> output = new ArrayList<>(List.of(
                file("assets/oraxen/models/generated.json", "{\"source\":\"generated\"}")));
        var merge = ResourcePack.class.getDeclaredMethod("mergeUploadedPacks", List.class);
        merge.setAccessible(true);
        merge.invoke(resourcePack, output);

        Map<String, String> files = contents(output);
        boolean liveImported = importEnabled && !emptyLivePack;
        assertEquals(liveImported ? "live texture" : "uploaded texture",
                files.get("assets/modelengine/textures/mob.png"));
        assertEquals(liveImported ? "{\"source\":\"live base\"}" : "{\"source\":\"uploaded base\"}",
                files.get("assets/modelengine/models/mob.json"));
        assertEquals("{\"source\":\"uploaded unique\"}", files.get("assets/other/models/uploaded.json"));
        assertEquals(liveImported, files.containsKey("assets/modelengine/models/live.json"));
        if (liveImported) {
            assertEquals("{\"source\":\"live unique\"}", files.get("assets/modelengine/models/live.json"));
        }
        assertEquals(liveImported ? "{\"source\":\"live overlay\"}" : "{\"source\":\"uploaded overlay\"}",
                files.get("oraxen_import_0/assets/modelengine/models/mob.json"));
        assertEquals("{\"source\":\"generated\"}", files.get("assets/oraxen/models/generated.json"));
        assertEquals("{\"source\":\"generated\"}", files.get("oraxen_import_0/assets/oraxen/models/generated.json"));
    }

    private static String metadata(String description) {
        return "{\"pack\":{\"pack_format\":46,\"description\":\"" + description
                + "\"},\"overlays\":{\"entries\":[{\"formats\":{\"min_inclusive\":46,\"max_inclusive\":55},\"directory\":\"overlay\"}]}}";
    }

    private static void writePack(Path directory, Map<String, String> files) throws IOException {
        for (var entry : files.entrySet()) {
            Path file = directory.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    private static void clearDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    private void deleteTestUpload() throws IOException {
        Path expectedUploads = packFolder.resolve("uploads").toAbsolutePath().normalize();
        Path testUpload = uploadedPacks.resolve("uploaded").toAbsolutePath().normalize();
        if (!uploadedPacks.toAbsolutePath().normalize().equals(expectedUploads)
                || !testUpload.startsWith(expectedUploads) || testUpload.equals(expectedUploads))
            throw new IOException("Refusing to remove files outside the test upload directory");
        clearDirectory(testUpload);
        Files.deleteIfExists(uploadedPacks);
    }

    private static Map<String, String> contents(List<VirtualFile> files) throws IOException {
        Map<String, String> contents = new LinkedHashMap<>();
        for (VirtualFile file : files) {
            try (var input = file.getInputStream()) {
                String previous = contents.put(file.getPath(), new String(input.readAllBytes(), StandardCharsets.UTF_8));
                assertNull(previous, "Duplicate generated path " + file.getPath());
            }
        }
        return contents;
    }

    private static VirtualFile file(String path, String content) {
        int separator = path.lastIndexOf('/');
        return new VirtualFile(path.substring(0, separator), path.substring(separator + 1),
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
