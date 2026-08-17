package io.th0rgal.oraxen.configs;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsUpdaterTest {

    @Test
    void migratesLegacyInventoryMenuAndCustomCategories() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("oraxen_inventory.main_menu_title", "Custom title");
        settings.set("oraxen_inventory.menu_rows", 4);
        settings.set("oraxen_inventory.menu_size", 27);
        settings.set("oraxen_inventory.menu_layout.custom.slot", 12);
        settings.set("oraxen_inventory.menu_layout.custom.icon", "custom_icon");
        settings.set("oraxen_inventory.menu_layout.custom.displayname", "Custom name");
        settings.set("oraxen_inventory.menu_layout.custom.title", "Category title");

        assertTrue(SettingsUpdater.migrateInventoryMenu(settings));

        assertFalse(settings.contains("oraxen_inventory"));
        assertEquals("Custom title", settings.getString("inventory-menu.title"));
        assertEquals(4, settings.getInt("inventory-menu.rows"));
        assertEquals(27, settings.getInt("inventory-menu.slots"));
        assertEquals(12, settings.getInt("inventory-menu.layout.custom.slot"));
        assertEquals("custom_icon", settings.getString("inventory-menu.layout.custom.icon"));
        assertEquals("Custom name", settings.getString("inventory-menu.layout.custom.name"));
        assertEquals("Category title", settings.getString("inventory-menu.layout.custom.title"));
    }

    @Test
    void ignoresSettingsWithoutLegacyInventoryMenu() {
        assertFalse(SettingsUpdater.migrateInventoryMenu(new YamlConfiguration()));
    }

    @Test
    void renamesDisplayNamesBeforeLegacyGuiInventoryIsRelocated() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("gui_inventory.custom.displayname", "Custom name");
        settings.set("gui_inventory.custom.icon", "custom_icon");

        assertTrue(SettingsUpdater.migrateInventoryMenu(settings));

        assertFalse(settings.contains("gui_inventory.custom.displayname"));
        assertEquals("Custom name", settings.getString("gui_inventory.custom.name"));
        assertEquals("custom_icon", settings.getString("gui_inventory.custom.icon"));
    }

    @Test
    void retainsSkippedModelDataNumbersSetting() {
        assertFalse(RemovedSettings.toStringList().contains("ConfigsTools.skipped_model_data_numbers"));
    }
}
