package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.settings.ResourcesManager;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class OraxenItems {

    //configuration sections : their OraxenItem wrapper
    private static Map<String, ItemBuilder> map;
    public static final NamespacedKey ITEM_ID = new NamespacedKey(OraxenPlugin.get(), "id");

    public static void loadItems(JavaPlugin plugin) {
        map = new LinkedHashMap<>();

        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            new ResourcesManager(plugin).extractConfigsInFolder("items", "yml");
        }

        File[] itemsConfig = itemsFolder.listFiles();
        Arrays.sort(itemsConfig);
        Map<String, ItemParser> parseMap = new LinkedHashMap<>();
        List<YamlConfiguration> configs = Arrays.stream(itemsConfig)
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
        for (Map.Entry<String, ItemParser> entry : parseMap.entrySet()) {
            ItemParser itemParser = entry.getValue();
            map.put(entry.getKey(), itemParser.buildItem());
            if (itemParser.isConfigUpdated())
                configUpdated = true;
        }
        if (configUpdated)
            for (int i = 0; i < itemsConfig.length; i++) {
                try {
                    configs.get(i).save(itemsConfig[i]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

    }

    public static String getIdByItem(ItemBuilder item) {
        return item.getCustomTag(ITEM_ID, PersistentDataType.STRING);
    }

    public static String getIdByItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getPersistentDataContainer().isEmpty())
            return null;
        else
            return item.getItemMeta()
                    .getPersistentDataContainer()
                    .get(ITEM_ID, PersistentDataType.STRING);
    }

    public static boolean isAnItem(String itemID) {
        return map.containsKey(itemID);
    }

    public static ItemBuilder getItemById(String id) {
        return map.get(id);
    }

    public static Collection<ItemBuilder> getItems() {
        return map.values();
    }

    public static Set<Map.Entry<String, ItemBuilder>> getEntries() {
        return map.entrySet();
    }

    public static Set<String> getSectionsNames() {
        return map.keySet();
    }

}