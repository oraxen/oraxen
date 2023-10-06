package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.Utils;
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
        settings = removeKeys(settings, RemovedSettings.toStringList());

        if (settings.saveToString().equals(oldSettings)) return;

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

    public YamlConfiguration removeKeys(YamlConfiguration settings, List<String> keys) {
        for (String key : keys) {
            if (settings.contains(key)) {
                Logs.logWarning("Found outdated setting " + key + ". This will be removed.");
            }
            settings.set(key, null);
            ConfigurationSection parent = settings.getConfigurationSection(Utils.getStringBeforeLastInSplit(key, "\\."));
            if (parent != null && parent.getKeys(false).isEmpty()) {
                settings.set(parent.getCurrentPath(), null);
            }

        }
        return settings;
    }

}
