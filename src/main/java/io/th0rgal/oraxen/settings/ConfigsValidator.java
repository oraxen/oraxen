package io.th0rgal.oraxen.settings;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;

public class ConfigsValidator {

    private JavaPlugin plugin;
    private int currentversion = 2;
    private File itemsFolder;

    public ConfigsValidator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean validatesConfig() {

        //check itemsFolder
        itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            new ResourcesManager(plugin).extractConfigsInFolder("items", "yml");
        }

        if ((boolean) Plugin.UPDATE_CONFIGS.getValue())
            updatesConfig();

        return true; //todo : return false when an error is detected + prints a detailed error
    }

    public void updatesConfig() {
        int configsVersion = (int) Plugin.CONFIGS_VERSION.getValue();

        if (configsVersion == currentversion)
            Message.CONFIGS_NOT_UPDATED.log();

        else if (configsVersion > currentversion) {
            Message.UNCONCISTENT_CONFIG_VERSION.logError();

        } else for (int i = configsVersion + 1; i <= currentversion; i++) { //so that you can update from 1 to n

            switch (i) {

                case 2:
                    // replaces layers by textures
                    applyToAllItems(itemSection -> {
                        if (!itemSection.isConfigurationSection("Pack"))
                            return itemSection;
                        ConfigurationSection packSection = itemSection.getConfigurationSection("Pack");
                        if (packSection.isList("layers")) {
                            packSection.set("textures", packSection.getList("layers"));
                            packSection.set("layers", null);
                        }
                        return itemSection;
                    });
                    break;

                case 3:
                    //code to update from config version 2 to 3
                    break;

                default:
                    Message.CONFIGS_UPDATING_FAILED.logError();
                    return;
            }
        }
    }

    private File[] getItemsFiles() {
        File[] itemsConfig = itemsFolder.listFiles();
        Arrays.sort(itemsConfig);
        return itemsConfig;
    }

    public void applyToAllItems(Function<ConfigurationSection, ConfigurationSection> itemSectionModifier) {
        for (File configFile : getItemsFiles()) {
            if (!configFile.getName().endsWith(".yml"))
                continue;
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
            for (String itemSectionName : configuration.getKeys(false)) {
                ConfigurationSection itemSection = configuration.getConfigurationSection(itemSectionName);
                itemSectionModifier.apply(itemSection);
            }
            try {
                configuration.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
