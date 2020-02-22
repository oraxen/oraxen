package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemParser;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigsManager {

    private JavaPlugin plugin;
    private YamlConfiguration defaultConfiguration;
    private int currentversion;
    private File itemsFolder;

    public ConfigsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        defaultConfiguration = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("settings.yml")));

        currentversion = defaultConfiguration.getInt("configs_version");
    }

    public boolean validatesConfig() {
        ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());
        File userConfigurationFile = resourcesManager.extractConfiguration("settings.yml");
        YamlConfiguration userConfiguration = YamlConfiguration.loadConfiguration(userConfigurationFile);
        boolean updated = false;
        for (String key : defaultConfiguration.getKeys(true))
            if (userConfiguration.get(key) == null) {
                updated = true;
                Message.UPDATING_CONFIG.logError(key);
                userConfiguration.set(key, defaultConfiguration.get(key));
            }

        if (updated)
            try {
                userConfiguration.save(userConfigurationFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        //check itemsFolder
        itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            new ResourcesManager(plugin).extractConfigsInFolder("items", "yml");
        }

        return true; //todo : return false when an error is detected + prints a detailed error
    }

    public void updatesConfigs() {
        if (!(boolean) Plugin.UPDATE_CONFIGS.getValue())
            return;

        int configsVersion = (int) Plugin.CONFIGS_VERSION.getValue();

        if (configsVersion == currentversion)
            Message.CONFIGS_NOT_UPDATED.logError();

        else if (configsVersion > currentversion) {
            Message.UNCONCISTENT_CONFIG_VERSION.logError();

        } else {
            for (int i = configsVersion + 1; i <= currentversion; i++) { //so that you can update from 1 to n

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
            //todo: updates configs version
        }
    }

    public Map<String, ItemBuilder> parsesConfigs() {
        Map<String, ItemParser> parseMap = new LinkedHashMap<>();
        File[] itemsFile = getItemsFiles();
        List<YamlConfiguration> configs = Arrays.stream(itemsFile)
                .filter(file -> file.getName().endsWith(".yml"))
                .map(YamlConfiguration::loadConfiguration)
                .collect(Collectors.toList());
        for (YamlConfiguration config : configs)
            for (String itemSectionName : config.getKeys(false)) {
                ConfigurationSection itemSection = config.getConfigurationSection(itemSectionName);
                parseMap.put(itemSectionName, new ItemParser(itemSection));
            }
        boolean configUpdated = false;
        // because we must have parse all the items before building them to be able to use available models
        Map<String, ItemBuilder> map = new LinkedHashMap<>();
        for (Map.Entry<String, ItemParser> entry : parseMap.entrySet()) {
            ItemParser itemParser = entry.getValue();
            map.put(entry.getKey(), itemParser.buildItem());
            if (itemParser.isConfigUpdated())
                configUpdated = true;
        }
        if (configUpdated)
            for (int i = 0; i < itemsFile.length; i++) {
                try {
                    configs.get(i).save(itemsFile[i]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        return map;
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
