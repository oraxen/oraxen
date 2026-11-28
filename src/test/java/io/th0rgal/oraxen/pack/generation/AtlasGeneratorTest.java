package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class AtlasGeneratorTest {

    private final YamlConfiguration settings = new YamlConfiguration();
    private MockedStatic<OraxenPlugin> pluginAccess;
    private MockedStatic<Logs> logs;

    @BeforeEach
    void setUp() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.namespace()).thenReturn("oraxen");
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenReturn(settings);
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        logs = mockStatic(Logs.class);
        settings.set(Settings.DEBUG.getPath(), false);
        settings.set(Settings.EXCLUDE_MALFORMED_ATLAS.getPath(), false);
        settings.set(Settings.ATLAS_GENERATION_TYPE.getPath(), "DIRECTORY");
        Settings.invalidateCache();
    }

    @AfterEach
    void tearDown() {
        Settings.invalidateCache();
        logs.close();
        pluginAccess.close();
    }

    @Test
    void directoryAtlasKeepsNestedDirectoriesUnlessAnAncestorIsListed() {
        List<VirtualFile> output = new ArrayList<>(List.of(
                texture("assets/oraxen/textures/block/stone.png"),
                texture("assets/oraxen/textures/block/ores/ruby.png"),
                texture("assets/oraxen/textures/block/ores/deep/sapphire.png"),
                texture("assets/oraxen/textures/custom/armor/helmet.png"),
                texture("assets/oraxen/textures/custom/armor/gold/boots.png"),
                texture("assets/oraxen/textures/icon.png")));

        AtlasGenerator.generateAtlasFile(output, Set.of());

        Set<String> expected = Set.of(
                "{\"type\":\"directory\",\"source\":\"block\",\"prefix\":\"block/\"}",
                "{\"type\":\"directory\",\"source\":\"custom/armor\",\"prefix\":\"custom/armor/\"}",
                "{\"type\":\"single\",\"resource\":\"oraxen:icon\",\"sprite\":\"oraxen:icon\"}");
        assertEquals(expected, atlasSources(output));
    }

    @Test
    void legacyDirectoryAtlasRetainsTheChildThatRestoresFilteredSprites() {
        byte[] blocks = """
                {"sources":[{"type":"directory","source":"block","prefix":"block/"},
                {"type":"directory","source":"block/custom","prefix":"block/custom/"},
                {"type":"filter","pattern":{"path":"block/custom/icon"}}]}
                """.getBytes(StandardCharsets.UTF_8);
        byte[] items = """
                {"sources":[{"type":"directory","source":"block/custom","prefix":"block/custom/"}]}
                """.getBytes(StandardCharsets.UTF_8);
        byte[] merged = AtlasGenerator.mergeSplitAtlasesForLegacyTarget(blocks, items);
        assertEquals(JsonParser.parseString("""
                {"sources":[{"type":"directory","source":"block","prefix":"block/"},
                {"type":"filter","pattern":{"path":"block/custom/icon"}},
                {"type":"directory","source":"block/custom","prefix":"block/custom/"}]}
                """), JsonParser.parseString(new String(merged, StandardCharsets.UTF_8)));
    }

    private static Set<String> atlasSources(List<VirtualFile> output) {
        JsonObject atlas = output.stream()
                .filter(file -> file.getPath().equals("assets/minecraft/atlases/blocks.json"))
                .findFirst().orElseThrow().toJsonObject();
        Set<String> sources = new HashSet<>();
        for (JsonElement source : atlas.getAsJsonArray("sources")) sources.add(source.toString());
        return sources;
    }

    private static VirtualFile texture(String path) {
        return new VirtualFile(path.substring(0, path.lastIndexOf('/')), path.substring(path.lastIndexOf('/') + 1),
                new ByteArrayInputStream(new byte[0]));
    }
}
