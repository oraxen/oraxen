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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ModelEnginePackPriorityTest {
    @TempDir
    Path tempDir;

    private final YamlConfiguration settings = new YamlConfiguration();
    private MockedStatic<OraxenPlugin> pluginAccess;
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<Logs> logs;
    private Path modelEnginePack;

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

        Path modelEngineFolder = tempDir.resolve("modelengine");
        modelEnginePack = Files.createDirectories(modelEngineFolder.resolve("resource pack"));
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
    void tearDown() {
        Settings.invalidateCache();
        bukkit.close();
        logs.close();
        pluginAccess.close();
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, false", "true, true"})
    void livePackReplacesUploadedCopiesOnlyWhenImported(boolean importEnabled, boolean emptyLivePack) throws Exception {
        settings.set(Settings.IMPORT_MODEL_ENGINE.getPath(), importEnabled);
        List<String> sharedPaths = List.of(
                "assets/modelengine/textures/mob.png",
                "assets/modelengine/models/mob.json",
                "overlay/assets/modelengine/models/mob.json");
        List<VirtualFile> uploaded = new ArrayList<>();
        List<VirtualFile> live = new ArrayList<>();
        for (String path : sharedPaths) {
            uploaded.add(file(path, "uploaded"));
            if (!emptyLivePack) live.add(file(path, "live"));
        }
        VirtualFile uploadedOnly = file("assets/other/models/uploaded.json", "uploaded only");
        uploaded.add(uploadedOnly);
        VirtualFile generated = file("assets/oraxen/models/generated.json", "generated");
        List<VirtualFile> output = new ArrayList<>(List.of(generated));
        List<VirtualFile> expected = new ArrayList<>(List.of(generated));
        if (importEnabled && !emptyLivePack) {
            expected.add(uploadedOnly);
            expected.addAll(live);
        } else {
            expected.addAll(uploaded);
        }

        try (var mergers = mockConstruction(PackMerger.class, (merger, context) -> {
            when(merger.mergeUploadedPacks()).thenReturn(uploaded);
            when(merger.mergeModelEnginePack(modelEnginePack.toFile())).thenReturn(live);
            when(merger.mergeWithHigherPriorityFiles(any())).thenReturn(expected);
        })) {
            ResourcePack pack = new ResourcePack();
            var merge = ResourcePack.class.getDeclaredMethod("mergeUploadedPacks", List.class);
            merge.setAccessible(true);
            merge.invoke(pack, output);

            assertEquals(expected, output);
            PackMerger merger = mergers.constructed().getFirst();
            verify(merger).mergeUploadedPacks();
            if (importEnabled) verify(merger).mergeModelEnginePack(modelEnginePack.toFile());
            else verify(merger, never()).mergeModelEnginePack(any());
        }
    }

    private static VirtualFile file(String path, String content) {
        int separator = path.lastIndexOf('/');
        return new VirtualFile(path.substring(0, separator), path.substring(separator + 1),
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
