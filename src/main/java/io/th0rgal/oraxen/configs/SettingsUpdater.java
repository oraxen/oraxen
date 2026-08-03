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

        settings = updateKeys(settings, UpdatedSettings.toStringMap());
        settings = migrateDispatchSettings(settings);
        settings = removeKeys(settings, RemovedSettings.toStringList());

        if (settings.saveToString().equals(oldSettings)) return;

        Settings.invalidateCache();

        try {
            settings.save(OraxenPlugin.get().getDataFolder().getAbsoluteFile().toPath().resolve("settings.yml").toFile());
            Logs.logSuccess("Successfully updated settings.yml", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public YamlConfiguration updateKeys(YamlConfiguration settings, Map<String, String> newKeyPaths) {
        for (Map.Entry<String, String> entry : newKeyPaths.entrySet()) {
            String key = entry.getKey();
            if (settings.contains(key)) {
                Logs.logWarning("Found outdated setting-path " + key + ". This will be updated.");
                settings.set(entry.getValue(), settings.get(key));
                settings.set(key, null);

            }
        }
        return settings;
    }

    public YamlConfiguration migrateDispatchSettings(YamlConfiguration settings) {
        boolean hasLegacySendPreJoin = settings.contains("Pack.dispatch.send_pre_join");
        boolean hasLegacySendOnJoin = settings.contains("Pack.dispatch.send_on_join");
        boolean hasLegacySendPack = settings.contains("Pack.dispatch.send_pack");

        if (hasLegacySendPreJoin || hasLegacySendOnJoin || hasLegacySendPack) {
            boolean sendPreJoin = settings.getBoolean("Pack.dispatch.send_pre_join", false);
            boolean sendOnJoin = hasLegacySendOnJoin
                    ? settings.getBoolean("Pack.dispatch.send_on_join", false)
                    : settings.getBoolean("Pack.dispatch.send_pack", false);

            Logs.logWarning("Found outdated pack dispatch send settings. These will be updated.");
            settings.set("Pack.dispatch.send", sendPreJoin || sendOnJoin);
            settings.set("Pack.dispatch.mode", sendPreJoin ? "PRE-JOIN" : "JOIN");
        }

        migrateKey(settings, "Pack.dispatch.stop", "Pack.dispatch.exclude");
        migrateKey(settings, "Pack.dispatch.disable_movement_on_load", "Pack.dispatch.disable.movement");
        migrateKey(settings, "Pack.dispatch.disable_damage_on_load", "Pack.dispatch.disable.damage");

        return settings;
    }

    private void migrateKey(YamlConfiguration settings, String oldPath, String newPath) {
        if (!settings.contains(oldPath)) return;
        Logs.logWarning("Found outdated setting-path " + oldPath + ". This will be updated.");
        settings.set(newPath, settings.get(oldPath));
    }

    public YamlConfiguration removeKeys(YamlConfiguration settings, List<String> keys) {
        for (String key : keys) {
            if (settings.contains(key)) {
                Logs.logWarning("Found outdated setting " + key + ". This will be removed.");
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
