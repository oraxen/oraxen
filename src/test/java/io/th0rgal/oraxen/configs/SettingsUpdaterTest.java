package io.th0rgal.oraxen.configs;

import org.bukkit.configuration.file.YamlConfiguration;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.mockito.MockedStatic;
import java.util.List;
import static org.mockito.Mockito.mockStatic;
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
        settings.set("oraxen_inventory.next_page_icon", "custom_next");
        settings.set("oraxen_inventory.previous_page_icon", "custom_previous");
        settings.set("oraxen_inventory.exit_icon", "custom_exit");
        settings.set("oraxen_inventory.menu_layout.custom.slot", 12);
        settings.set("oraxen_inventory.menu_layout.custom.icon", "custom_icon");
        settings.set("oraxen_inventory.menu_layout.custom.displayname", "Custom name");
        settings.set("oraxen_inventory.menu_layout.custom.title", "Category title");

        assertTrue(SettingsUpdater.migrateInventoryMenu(settings));

        assertFalse(settings.contains("oraxen_inventory"));
        assertEquals("Custom title", settings.getString("inventory.title"));
        assertEquals(4, settings.getInt("inventory.rows"));
        assertEquals(27, settings.getInt("inventory.slots"));
        assertEquals("custom_next", settings.getString("inventory.next-page-icon"));
        assertEquals("custom_previous", settings.getString("inventory.previous-page-icon"));
        assertEquals("custom_exit", settings.getString("inventory.exit-icon"));
        assertEquals(12, settings.getInt("inventory.layout.custom.slot"));
        assertEquals("custom_icon", settings.getString("inventory.layout.custom.icon"));
        assertEquals("Custom name", settings.getString("inventory.layout.custom.name"));
        assertEquals("Category title", settings.getString("inventory.layout.custom.title"));
    }

    @Test
    void ignoresSettingsWithoutLegacyInventoryMenu() {
        assertFalse(SettingsUpdater.migrateInventoryMenu(new YamlConfiguration()));
    }

    @Test
    void relocatesLegacyGuiInventory() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("gui_inventory.custom.displayname", "Custom name");
        settings.set("gui_inventory.custom.icon", "custom_icon");

        assertTrue(SettingsUpdater.migrateInventoryMenu(settings));

        assertFalse(settings.contains("gui_inventory"));
        assertEquals("Custom name", settings.getString("inventory.layout.custom.name"));
        assertEquals("custom_icon", settings.getString("inventory.layout.custom.icon"));
    }

    @Test
    void mergesBothLegacyInventoryLayouts() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("gui_inventory.gui_category.icon", "gui_icon");
        settings.set("oraxen_inventory.menu_layout.oraxen_category.icon", "oraxen_icon");

        assertTrue(SettingsUpdater.migrateInventoryMenu(settings));

        assertFalse(settings.contains("gui_inventory"));
        assertFalse(settings.contains("oraxen_inventory"));
        assertEquals("gui_icon", settings.getString("inventory.layout.gui_category.icon"));
        assertEquals("oraxen_icon", settings.getString("inventory.layout.oraxen_category.icon"));
    }

    @Test
    void preservesCurrentInventoryValuesAndMergesMissingLegacyLeaves() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("inventory.title", "Current title");
        settings.set("inventory.layout.Custom_ID.icon", "current_icon");
        settings.set("oraxen_inventory.main_menu_title", "Legacy title");
        settings.set("oraxen_inventory.menu_layout.Custom_ID.icon", "legacy_icon");
        settings.set("oraxen_inventory.menu_layout.Custom_ID.displayname", "Legacy name");
        settings.set("gui_inventory.Another_ID.icon", "another_icon");

        assertTrue(SettingsUpdater.migrateInventoryMenu(settings));

        assertEquals("Current title", settings.getString("inventory.title"));
        assertEquals("current_icon", settings.getString("inventory.layout.Custom_ID.icon"));
        assertEquals("Legacy name", settings.getString("inventory.layout.Custom_ID.name"));
        assertEquals("another_icon", settings.getString("inventory.layout.Another_ID.icon"));
        assertFalse(settings.contains("oraxen_inventory"));
        assertFalse(settings.contains("gui_inventory"));
        assertFalse(settings.contains("inventory-menu"));
        String migrated = settings.saveToString();
        assertFalse(SettingsUpdater.migrateInventoryMenu(settings));
        assertEquals(migrated, settings.saveToString());
    }

    @Test
    void migratesHistoricalSettingsAfterRenamingTheirParents() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("Pack.generation.generate_atlas_file", false);
        settings.set("Misc.auto_update_items", false);
        settings.set("Pack.dispatch.send_pre_join", true);
        settings.set("Pack.dispatch.stop", List.of("< 1.20"));
        settings.set("Pack.dispatch.disable_movement_on_load", false);
        settings.set("Plugin.experimental.merge_font_files", false);
        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            assertTrue(SettingsUpdater.migrateBeforeDefaults(settings));
            assertFalse(settings.getBoolean("pack.generation.atlas.generate"));
            assertTrue(settings.contains("pack.generation.atlas.generate", true));
            assertFalse(settings.getBoolean("updater.items.update-items"));
            assertTrue(settings.contains("updater.items.update-items", true));
            assertTrue(settings.getBoolean("pack.dispatch.send"));
            assertEquals("PRE-JOIN", settings.getString("pack.dispatch.mode"));
            assertEquals(List.of("< 1.20"), settings.getStringList("pack.dispatch.exclude"));
            assertFalse(settings.getBoolean("pack.dispatch.disable.movement"));
            assertTrue(settings.contains("pack.dispatch.disable.movement", true));
            assertFalse(settings.getBoolean("pack.import.merge.fonts"));
            assertFalse(settings.contains("pack.dispatch.send_pre_join", true));
            assertFalse(settings.contains("pack.generation.generate_atlas_file", true));
            assertFalse(SettingsUpdater.migrateBeforeDefaults(settings));
        }
    }

    @Test
    void renamesCurrentSettingsWhenAutomaticHistoricalUpdatesAreDisabled() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("Plugin.keep_this_up_to_date", false);
        settings.set("Plugin.language", "german");
        settings.set("Pack.generation.disable_mcmeta_generation", true);
        settings.set("Misc.auto_update_items", false);
        settings.set("oraxen_inventory.main_menu_title", "Custom title");

        assertTrue(SettingsUpdater.migrateBeforeDefaults(settings));

        assertFalse(settings.getBoolean("general.auto-update.settings"));
        assertEquals("german", settings.getString("general.language"));
        assertFalse(settings.getBoolean("pack.generation.mcmeta"));
        assertEquals("Custom title", settings.getString("inventory.title"));
        assertTrue(settings.contains("Misc.auto_update_items", true));
        assertFalse(settings.contains("updater.items.update-items", true));
        assertFalse(SettingsUpdater.migrateBeforeDefaults(settings));
    }

    @Test
    void fallbackItemAdapterUsesItemKeySpellingsWithoutMutatingSettings() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("configuration.items.fallback.material", "STONE");
        settings.set("configuration.items.fallback.exclude-from-inventory", true);
        settings.set("configuration.items.fallback.inject-id", true);
        settings.set("configuration.items.fallback.Pack.model", "old_model");
        settings.set("configuration.items.fallback.custom.keep", "custom_value");
        settings.set("configuration.items.fallback.custom.remove", "obsolete");
        String original = settings.saveToString();

        var item = ConfigsManager.fallbackItemConfig(
                settings.getConfigurationSection("configuration.items.fallback"));

        assertEquals("STONE", item.getString("material"));
        assertTrue(item.getBoolean("excludeFromInventory"));
        assertTrue(item.getBoolean("injectId"));
        assertFalse(item.contains("exclude-from-inventory"));
        assertFalse(item.contains("inject-id"));
        assertEquals("old_model", item.getString("Pack.model"));
        assertEquals("custom_value", item.getString("custom.keep"));
        item.set("Pack.model", "updated_model");
        item.set("custom.added", 17);
        item.set("custom.remove", null);
        item.set("excludeFromInventory", false);
        item.set("injectId", null);
        assertEquals(original, settings.saveToString());

        var fallback = settings.getConfigurationSection("configuration.items.fallback");
        ConfigsManager.persistFallbackItemConfig(fallback, item);

        assertEquals("updated_model", fallback.getString("Pack.model"));
        assertEquals("custom_value", fallback.getString("custom.keep"));
        assertEquals(17, fallback.getInt("custom.added"));
        assertFalse(fallback.contains("custom.remove", true));
        assertFalse(fallback.getBoolean("exclude-from-inventory"));
        assertTrue(fallback.contains("exclude-from-inventory", true));
        assertFalse(fallback.contains("excludeFromInventory", true));
        assertFalse(fallback.contains("injectId", true));
        assertFalse(fallback.contains("inject-id", true));
        item.set("Pack.model", "changed_after_persistence");
        assertEquals("updated_model", fallback.getString("Pack.model"));
    }

    @Test
    void removesHistoricalSettingsOnlyWhenAutomaticUpdatesAreEnabled() {
        for (boolean automaticUpdates : List.of(false, true)) {
            YamlConfiguration settings = new YamlConfiguration();
            settings.set("Plugin.keep_this_up_to_date", automaticUpdates);
            settings.set("Pack.generation.texture_slicer", true);
            try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
                assertTrue(SettingsUpdater.migrateBeforeDefaults(settings));
                assertFalse(settings.contains("Pack.generation.texture_slicer", true));
                assertEquals(!automaticUpdates, settings.contains("pack.generation.texture_slicer", true));
                assertFalse(settings.contains("pack.generation.texture-slicer", true));
                assertFalse(SettingsUpdater.migrateBeforeDefaults(settings));
            }
        }
    }

    @Test
    void retainsSkippedModelDataNumbersSetting() {
        assertFalse(RemovedSettings.toStringList().contains("ConfigsTools.skipped_model_data_numbers"));
    }
}
