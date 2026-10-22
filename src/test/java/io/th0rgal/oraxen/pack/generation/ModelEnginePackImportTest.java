package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ModelEnginePackImportTest {

    @TempDir
    Path tempDir;

    private final YamlConfiguration settings = new YamlConfiguration();
    private MockedStatic<OraxenPlugin> pluginAccess;
    private MockedStatic<Logs> logs;

    @BeforeEach
    void setUp() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenReturn(settings);
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        logs = mockStatic(Logs.class);
        Settings.invalidateCache();
    }

    @AfterEach
    void tearDown() {
        Settings.invalidateCache();
        logs.close();
        pluginAccess.close();
    }

    @ParameterizedTest
    @CsvSource({"true, true", "true, false", "false, true", "false, false"})
    void importsAssetsAndOverlaysWithoutReplacingPackMetadata(boolean zip, boolean removeCoreShaders) throws IOException {
        settings.set(Settings.REMOVE_CORE_SHADERS_FROM_IMPORTED_PACKS.getPath(), removeCoreShaders);
        String overlayEntry = "{\"formats\":{\"min_inclusive\":46,\"max_inclusive\":55},\"directory\":\"overlay\"}";
        Map<String, String> source = new LinkedHashMap<>();
        source.put("pack.mcmeta", "{\"pack\":{\"pack_format\":46,\"description\":\"ModelEngine\"},\"overlays\":{\"entries\":[" + overlayEntry + "]}}");
        source.put("pack.png", "ModelEngine icon");
        source.put("assets/modelengine/models/mob.json", "{\"textures\":{\"all\":\"modelengine:mob\"}}");
        source.put("assets/modelengine/textures/mob.png", "texture bytes");
        source.put("assets/modelengine/items/mob.json", "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"modelengine:mob\"}}");
        source.put("overlay/assets/modelengine/models/mob.json", "{\"parent\":\"modelengine:mob\"}");
        source.put("assets/minecraft/shaders/core/rendertype_entity_cutout.vsh", "base shader");
        source.put("overlay/assets/minecraft/shaders/core/rendertype_entity_cutout.vsh", "overlay shader");
        source.put("assets/minecraft/shaders/include/modelengine.glsl", "shader include");

        Path pack = tempDir.resolve(zip ? "resource pack.zip" : "resource pack");
        writePack(pack, source, zip);
        PackMerger merger = new PackMerger(tempDir.resolve("oraxen-pack").toFile());
        Map<String, String> imported = new LinkedHashMap<>();
        for (VirtualFile file : merger.mergeModelEnginePack(pack.toFile())) {
            try (var stream = file.getInputStream()) {
                imported.put(file.getPath(), new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }

        Map<String, String> expected = new LinkedHashMap<>(source);
        expected.remove("pack.mcmeta");
        expected.remove("pack.png");
        String overlayModel = expected.remove("overlay/assets/modelengine/models/mob.json");
        String overlayShader = expected.remove("overlay/assets/minecraft/shaders/core/rendertype_entity_cutout.vsh");
        if (removeCoreShaders) {
            expected.remove("assets/minecraft/shaders/core/rendertype_entity_cutout.vsh");
        }
        Map<String, String> snapshot = new LinkedHashMap<>(expected);
        snapshot.put("assets/modelengine/models/mob.json", overlayModel);
        if (!removeCoreShaders) snapshot.put("assets/minecraft/shaders/core/rendertype_entity_cutout.vsh", overlayShader);
        snapshot.forEach((path, content) -> expected.put("oraxen_import_0/" + path, content));
        assertEquals(expected, imported);
        var compiled = merger.getMergedOverlayEntries();
        assertEquals(1, compiled.size());
        var declaration = compiled.get(0).getAsJsonObject();
        assertEquals("oraxen_import_0", declaration.get("directory").getAsString());
        assertEquals(46, declaration.get("min_format").getAsInt());
        assertEquals(55, declaration.get("max_format").getAsInt());
        assertEquals(JsonParser.parseString("{\"min_inclusive\":46,\"max_inclusive\":55}"), declaration.get("formats"));
    }

    private void writePack(Path target, Map<String, String> files, boolean zip) throws IOException {
        if (zip) {
            try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(target))) {
                for (var entry : files.entrySet()) {
                    output.putNextEntry(new ZipEntry(entry.getKey()));
                    output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                    output.closeEntry();
                }
            }
        } else {
            for (var entry : files.entrySet()) {
                Path file = target.resolve(entry.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
            }
        }
    }
}
