package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ModelDataTest {

    @Test
    void nextModelDataReturnsCurrentValueWhenItIsNotSkipped() {
        assertEquals(1001, ModelData.getNextNotSkippedCustomModelData(1001, Set.of(1000, 1002)));
    }

    @Test
    void nextModelDataSkipsConsecutiveReservedValues() {
        assertEquals(1004, ModelData.getNextNotSkippedCustomModelData(1001, Set.of(1001, 1002, 1003)));
    }

    @Test
    void nextModelDataHandlesReservedValueAboveCurrentCandidate() {
        assertEquals(1003, ModelData.getNextNotSkippedCustomModelData(1003, Set.of(1004)));
    }

    @Test
    void invalidSkippedNumbersAreIgnoredWhileValidOnesStillApply() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set(Settings.SKIPPED_MODEL_DATA_NUMBERS.getPath(), List.of("1000", "abc", "1002 - 1003", "5-", "1-2-3", "2147483647"));
        try (var plugins = mockStatic(OraxenPlugin.class); var logs = mockStatic(Logs.class)) {
            OraxenPlugin plugin = mock(OraxenPlugin.class);
            ConfigsManager configs = mock(ConfigsManager.class);
            plugins.when(OraxenPlugin::get).thenReturn(plugin);
            when(plugin.getConfigsManager()).thenReturn(configs);
            when(configs.getSettings()).thenReturn(settings);
            ModelData.DATAS.remove(Material.STRUCTURE_VOID);

            assertEquals(1001, ModelData.generateId("first", Material.STRUCTURE_VOID));
            assertEquals(1004, ModelData.generateId("second", Material.STRUCTURE_VOID));
            logs.verify(() -> Logs.logWarning(contains("skipped model data")), times(3));
        } finally {
            ModelData.DATAS.remove(Material.STRUCTURE_VOID);
        }
    }
}
