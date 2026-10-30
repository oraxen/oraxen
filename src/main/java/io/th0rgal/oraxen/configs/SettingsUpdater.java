package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Map;

public class SettingsUpdater {

    public void handleSettingsUpdate() {
        YamlConfiguration settings = OraxenPlugin.get().getConfigsManager().getSettings();
        String oldSettings = settings.saveToString();

        migrateBeforeDefaults(settings);

        if (settings.saveToString().equals(oldSettings)) return;

        Settings.invalidateCache();

        try {
            settings.save(OraxenPlugin.get().getDataFolder().getAbsoluteFile().toPath().resolve("settings.yml").toFile());
            Logs.logSuccess("Successfully updated settings.yml", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean migrateBeforeDefaults(YamlConfiguration settings) {
        String before = settings.saveToString();
        SettingsMigration.migrate(settings);
        migrateInventoryMenu(settings);
        if (settings.getBoolean("general.auto-update.settings", true)) {
            SettingsUpdater updater = new SettingsUpdater();
            updater.updateKeys(settings, UpdatedSettings.toStringMap());
            updater.migrateDispatchSettings(settings);
            updater.removeKeys(settings, RemovedSettings.toStringList());
            // Historical migrations may leave empty legacy parent sections.
            SettingsMigration.migrate(settings);
        }
        return !before.equals(settings.saveToString());
    }

    public YamlConfiguration updateKeys(YamlConfiguration settings, Map<String, String> newKeyPaths) {
        for (Map.Entry<String, String> entry : newKeyPaths.entrySet()) {
            String key = existingPath(settings, entry.getKey());
            if (settings.contains(key)) {
                Logs.logWarning("Found outdated setting-path " + key + ". This will be updated.");
                mergeMissing(settings, settings.get(key), entry.getValue());
                settings.set(key, null);

            }
        }
        return settings;
    }

    static boolean migrateInventoryMenu(YamlConfiguration settings) {
        boolean updated = migrateLegacyGuiInventoryLayout(settings);
        ConfigurationSection legacyMenu = settings.getConfigurationSection("oraxen_inventory");
        if (legacyMenu == null) return updated;

        migrateInventoryMenuValue(settings, legacyMenu, "main_menu_title", "title");
        migrateInventoryMenuValue(settings, legacyMenu, "menu_rows", "rows");
        migrateInventoryMenuValue(settings, legacyMenu, "menu_size", "slots");
        migrateInventoryMenuValue(settings, legacyMenu, "next_page_icon", "next-page-icon");
        migrateInventoryMenuValue(settings, legacyMenu, "previous_page_icon", "previous-page-icon");
        migrateInventoryMenuValue(settings, legacyMenu, "exit_icon", "exit-icon");

        ConfigurationSection legacyLayout = legacyMenu.getConfigurationSection("menu_layout");
        if (legacyLayout != null) {
            for (Map.Entry<String, Object> entry : legacyLayout.getValues(true).entrySet()) {
                if (entry.getValue() instanceof ConfigurationSection) continue;
                String path = entry.getKey().replaceAll("(^|\\.)displayname$", "$1name");
                mergeMissing(settings, entry.getValue(), "inventory.layout." + path);
            }
        }

        settings.set("oraxen_inventory", null);
        return true;
    }

    private static boolean migrateLegacyGuiInventoryLayout(YamlConfiguration settings) {
        ConfigurationSection legacyLayout = settings.getConfigurationSection("gui_inventory");
        if (legacyLayout == null) return false;

        for (Map.Entry<String, Object> entry : legacyLayout.getValues(true).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection) continue;
            String path = entry.getKey().replaceAll("(^|\\.)displayname$", "$1name");
            mergeMissing(settings, entry.getValue(), "inventory.layout." + path);
        }
        settings.set("gui_inventory", null);
        return true;
    }

    private static void migrateInventoryMenuValue(YamlConfiguration settings, ConfigurationSection legacyMenu,
            String oldPath, String newPath) {
        if (legacyMenu.contains(oldPath))
            mergeMissing(settings, legacyMenu.get(oldPath), "inventory." + newPath);
    }

    public YamlConfiguration migrateDispatchSettings(YamlConfiguration settings) {
        boolean hasLegacySendPreJoin = settings.contains(existingPath(settings, "Pack.dispatch.send_pre_join"));
        boolean hasLegacySendOnJoin = settings.contains(existingPath(settings, "Pack.dispatch.send_on_join"));
        boolean hasLegacySendPack = settings.contains(existingPath(settings, "Pack.dispatch.send_pack"));

        if (hasLegacySendPreJoin || hasLegacySendOnJoin || hasLegacySendPack) {
            boolean sendPreJoin = settings.getBoolean(existingPath(settings, "Pack.dispatch.send_pre_join"), false);
            boolean sendOnJoin = hasLegacySendOnJoin
                    ? settings.getBoolean(existingPath(settings, "Pack.dispatch.send_on_join"), false)
                    : settings.getBoolean(existingPath(settings, "Pack.dispatch.send_pack"), false);

            Logs.logWarning("Found outdated pack dispatch send settings. These will be updated.");
            mergeMissing(settings, sendPreJoin || sendOnJoin, "pack.dispatch.send");
            mergeMissing(settings, sendPreJoin ? "PRE-JOIN" : "JOIN", "pack.dispatch.mode");
        }

        migrateKey(settings, "Pack.dispatch.stop", "pack.dispatch.exclude");
        migrateKey(settings, "Pack.dispatch.disable_movement_on_load", "pack.dispatch.disable.movement");
        migrateKey(settings, "Pack.dispatch.disable_damage_on_load", "pack.dispatch.disable.damage");

        return settings;
    }

    private void migrateKey(YamlConfiguration settings, String oldPath, String newPath) {
        oldPath = existingPath(settings, oldPath);
        if (!settings.contains(oldPath)) return;
        Logs.logWarning("Found outdated setting-path " + oldPath + ". This will be updated.");
        mergeMissing(settings, settings.get(oldPath), newPath);
        settings.set(oldPath, null);
    }

    private static String existingPath(YamlConfiguration settings, String oldPath) {
        return settings.contains(oldPath, true) ? oldPath : SettingsMigration.newPath(oldPath);
    }

    private static void mergeMissing(YamlConfiguration settings, Object value, String path) {
        if (value instanceof ConfigurationSection section) {
            for (var entry : section.getValues(false).entrySet())
                mergeMissing(settings, entry.getValue(), path + "." + entry.getKey());
        } else if (!settings.contains(path, true)) {
            settings.set(path, value);
        }
    }

    /**
     * Targeted migration notices for removed settings whose removal changes behavior in a way
     * server owners should understand, instead of only the generic "outdated setting" log.
     */
    private static final Map<String, String> REMOVAL_NOTICES = Map.of(
            "Chat.chat_handler", "The legacy chat handler has been removed; Oraxen now always uses the modern Paper chat handler. "
                    + "Messages containing raw glyph characters the sender lacks permission for are blocked with a no-permission notice.");

    public YamlConfiguration removeKeys(YamlConfiguration settings, List<String> keys) {
        for (String originalKey : keys) {
            String key = existingPath(settings, originalKey);
            if (settings.contains(key)) {
                Logs.logWarning("Found outdated setting " + key + ". This will be removed.");
                String notice = REMOVAL_NOTICES.get(originalKey);
                if (notice != null) Logs.logWarning(notice);
            }
            settings.set(key, null);

            String parentPath = key;
            while ((parentPath = getParentPath(parentPath)) != null) {
                ConfigurationSection parent = settings.getConfigurationSection(parentPath);
                if (parent == null || !parent.getKeys(false).isEmpty()) break;
                settings.set(parentPath, null);
            }

        }
        return settings;
    }

    private String getParentPath(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(0, lastDot) : null;
    }

}
