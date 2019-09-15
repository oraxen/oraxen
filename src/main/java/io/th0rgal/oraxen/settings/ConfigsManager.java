package io.th0rgal.oraxen.settings;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigsManager {

    JavaPlugin plugin;
    public ConfigsManager (JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private YamlConfiguration settings;
    public YamlConfiguration getSettings() {
        if (settings == null)
            settings = getConfiguration("settings.yml", settings);
        return settings;
    }

    private YamlConfiguration items;
    public YamlConfiguration getItems() {
        if (items == null)
            items = getConfiguration("items.yml", items);
        return items;
    }

    public YamlConfiguration getConfiguration(String fileName, YamlConfiguration yamlConfiguration) {
        File itemsFile = new File(this.plugin.getDataFolder(), fileName);
        if (!itemsFile.exists())
            this.plugin.saveResource(fileName, false);
        return YamlConfiguration.loadConfiguration(itemsFile);
    }

}
