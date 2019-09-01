package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigsManager {

    private static File settingsFile = new File(OraxenPlugin.get().getDataFolder(), "settings.yml");
    private static YamlConfiguration settings;

    public static YamlConfiguration getSettings() {
        if (settings == null) {
            if (!settingsFile.exists())
                OraxenPlugin.get().saveResource("settings.yml", false);
            settings = YamlConfiguration.loadConfiguration(settingsFile);
        }
        return settings;
    }


    private static File itemsFile = new File(OraxenPlugin.get().getDataFolder(), "items.yml");
    private static YamlConfiguration items;

    public static YamlConfiguration getItems() {
        if (items == null) {
            if (!itemsFile.exists())
                OraxenPlugin.get().saveResource("items.yml", false);
            items = YamlConfiguration.loadConfiguration(itemsFile);
        }
        return items;
    }

}
