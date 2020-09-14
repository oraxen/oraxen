package io.th0rgal.oraxen.settings;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.event.config.OraxenConfigEvent;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemParser;
import io.th0rgal.oraxen.settings.update.ExampleUpdate;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigsManager implements Listener {

    private final JavaPlugin plugin;
    private final YamlConfiguration defaultConfiguration;
    private File itemsFolder;

    public ConfigsManager(JavaPlugin plugin) {
        this.plugin = plugin;

        InputStreamReader inputStreamReader = new InputStreamReader(plugin.getResource("settings.yml"));
        try {
            defaultConfiguration = YamlConfiguration.loadConfiguration(inputStreamReader);
        } finally {
            if (inputStreamReader != null)
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @EventHandler
    public void onUpdate(OraxenConfigEvent event) {
        event.registerUpdates(ExampleUpdate.class);
    }

    public boolean validatesConfig() {
        ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());
        File userConfigurationFile = resourcesManager.extractConfiguration("settings.yml");
        YamlConfiguration userConfiguration = YamlConfiguration.loadConfiguration(userConfigurationFile);
        boolean updated = ConfigUpdater.update(userConfigurationFile, userConfiguration);
        for (String key : defaultConfiguration.getKeys(true))
            if (userConfiguration.get(key) == null) {
                updated = true;
                MessageOld.UPDATING_CONFIG.logError(key);
                userConfiguration.set(key, defaultConfiguration.get(key));
            }

        if (updated)
            try {
                userConfiguration.save(userConfigurationFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        // check itemsFolder
        itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            new ResourcesManager(plugin).extractConfigsInFolder("items", "yml");
        }

        return true; // todo : return false when an error is detected + prints a detailed error
    }

//    public void updatesConfigs() {
//        if (!(boolean) Plugin.UPDATE_CONFIGS.getValue())
//            return;
//
//        int configsVersion = (int) Plugin.CONFIGS_VERSION.getValue();
//
//        if (configsVersion == currentversion)
//            MessageOld.CONFIGS_NOT_UPDATED.logError();
//
//        else if (configsVersion > currentversion) {
//            MessageOld.UNCONSISTENT_CONFIG_VERSION.logError();
//
//        } else {
//            for (int i = configsVersion + 1; i <= currentversion; i++) { // so that you can update from 1 to n
//
//                switch (i) {
//
//                case 2:
//                    // replaces layers by textures
//                    applyToAllItems(itemSection -> {
//                        if (!itemSection.isConfigurationSection("Pack"))
//                            return itemSection;
//                        ConfigurationSection packSection = itemSection.getConfigurationSection("Pack");
//                        if (packSection.isList("layers")) {
//                            packSection.set("textures", packSection.getList("layers"));
//                            packSection.set("layers", null);
//                        }
//                        return itemSection;
//                    });
//                    break;
//
//                case 3:
//                    // code to update from config version 2 to 3
//                    break;
//
//                default:
//                    MessageOld.CONFIGS_UPDATING_FAILED.logError();
//                    return;
//                }
//            }
//            // todo: updates configs version
//        }
//    }

    public Map<File, Map<String, ItemBuilder>> parsesConfigs() {
        Map<File, Map<String, ItemBuilder>> parseMap = new LinkedHashMap<>();
        List<File> configs = Arrays
                .stream(getItemsFiles())
                .filter(file -> file.getName().endsWith(".yml"))
                .collect(Collectors.toList());
        for (File file : configs) {
            parseMap.put(file, parsesConfig(YamlConfiguration.loadConfiguration(file), file));
        }
        return parseMap;
    }

    public Map<String, ItemBuilder> parsesConfig(YamlConfiguration config, File itemFile) {
        Map<String, ItemParser> parseMap = new LinkedHashMap<>();
        ItemParser errorItem = new ItemParser((ConfigurationSection) Plugin.ERROR_ITEM.getValue());
        for (String itemSectionName : config.getKeys(false)) {
            if (!config.isConfigurationSection(itemSectionName))
                continue;
            ConfigurationSection itemSection = config.getConfigurationSection(itemSectionName);
            parseMap.put(itemSectionName, new ItemParser(itemSection));
        }
        boolean configUpdated = ConfigUpdater.update(itemFile, config);
        // because we must have parse all the items before building them to be able to
        // use available models
        Map<String, ItemBuilder> map = new LinkedHashMap<>();
        for (Map.Entry<String, ItemParser> entry : parseMap.entrySet()) {
            ItemParser itemParser = entry.getValue();
            try {
                map.put(entry.getKey(), itemParser.buildItem());
            } catch (Exception e) {
                map
                        .put(entry.getKey(),
                                errorItem
                                        .buildItem(String.valueOf(ChatColor.DARK_RED) + ChatColor.BOLD
                                                + e.getClass().getSimpleName() + ": " + ChatColor.RED + entry.getKey()));
                Logs.logError("ERROR BUILDING ITEM \"" + entry.getKey() + "\"");
                e.printStackTrace();
            }
            if (itemParser.isConfigUpdated())
                configUpdated = true;
        }
        if (configUpdated)
            try {
                config.save(itemFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        return map;
    }

    private File[] getItemsFiles() {
        File[] itemsConfig = itemsFolder.listFiles();
        Arrays.sort(itemsConfig);
        return itemsConfig;
    }

//    public void applyToAllItems(Function<ConfigurationSection, ConfigurationSection> itemSectionModifier) {
//        for (File configFile : getItemsFiles()) {
//            if (!configFile.getName().endsWith(".yml"))
//                continue;
//            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
//            for (String itemSectionName : configuration.getKeys(false)) {
//                ConfigurationSection itemSection = configuration.getConfigurationSection(itemSectionName);
//                itemSectionModifier.apply(itemSection);
//            }
//            try {
//                configuration.save(configFile);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

}
