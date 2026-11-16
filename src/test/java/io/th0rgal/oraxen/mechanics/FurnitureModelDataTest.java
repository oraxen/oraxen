package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.items.ModelData;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureModelData;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FurnitureModelDataTest {
    @TempDir Path directory;

    @AfterEach
    void clearAssignments() {
        ModelData.DATAS.clear();
        FurnitureModelData.load(directory.resolve("empty.yml").toFile());
        Settings.invalidateCache();
    }

    @Test
    void reloadReservesBaseAndStageNumbersBeforeNewItemsLoad() {
        var file = directory.resolve("furniture-model-data.yml").toFile();
        ModelData.DATAS.clear();
        FurnitureModelData.load(file);
        new ModelData(Material.PAPER, "inventory", 1000);
        new ModelData(Material.PAPER, "furniture/chair.v2", 1001);
        new ModelData(Material.PAPER, "furniture/chair.v2/grown", 1002);
        assertEquals(1001, FurnitureModelData.generateId("furniture/chair.v2", Material.PAPER));
        assertEquals(1002, FurnitureModelData.generateId("furniture/chair.v2/grown", Material.PAPER));

        ModelData.DATAS.clear();
        new ModelData(Material.PAPER, "inventory", 1000);
        FurnitureModelData.load(file);
        assertEquals(1001, ModelData.DATAS.get(Material.PAPER).get("furniture/chair.v2"));
        assertEquals(1002, ModelData.DATAS.get(Material.PAPER).get("furniture/chair.v2/grown"));
        try (var plugins = mockStatic(OraxenPlugin.class)) {
            var plugin = mock(OraxenPlugin.class);
            var configs = mock(ConfigsManager.class);
            plugins.when(OraxenPlugin::get).thenReturn(plugin);
            when(plugin.getConfigsManager()).thenReturn(configs);
            when(configs.getSettings()).thenReturn(new YamlConfiguration());
            Settings.invalidateCache();
            assertEquals(1003, ModelData.generateId("new_inventory", Material.PAPER));
        }
    }

    @Test
    void refusesToReuseSavedFurnitureNumberForAnotherItem() throws Exception {
        var file = directory.resolve("furniture-model-data.yml").toFile();
        java.nio.file.Files.writeString(file.toPath(), "PAPER:\n  furniture/chair: 1001\n");
        ModelData.DATAS.clear();
        new ModelData(Material.PAPER, "new_inventory", 1001);
        assertThrows(IllegalStateException.class, () -> FurnitureModelData.load(file));
    }
}
