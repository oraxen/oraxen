package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

public class SettingsUpdater {

    public void updateKeys() {
        YamlConfiguration settings = OraxenPlugin.get().getConfigsManager().getSettings();

        settings = removeKeys(settings, RemovedSettings.toStringList());

        try {
            settings.save(OraxenPlugin.get().getDataFolder().getAbsoluteFile().toPath().resolve("settings.yml").toFile());
            Logs.logSuccess("Successfully updated settings.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
