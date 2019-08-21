package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.settings.ConfigsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OraxenItems {

    //configuration sections : their OraxenItem wrapper
    private static Map<String, Item> map = new HashMap<>();

    public static void loadItems() {

        YamlConfiguration config = ConfigsManager.getItems();

        for (String itemSectionName : config.getKeys(false)) {
            ConfigurationSection itemSection = config.getConfigurationSection(itemSectionName);
            map.put(itemSectionName, new ItemParser(itemSection).buildItem());
        }
    }

    public static String getIdByItem(Item item) {
        return item.getNBTBase("OxnId").toString();
    }

    public static String getIdByItem(ItemStack item) {
        return ItemUtils.getStringField(item, "OxnId");
    }

    public static Item getItemById(String id) {
        return map.get(id);
    }

    public static Collection<Item> getItems() {
        return map.values();
    }

    public static Set<Map.Entry<String, Item>> getEntries() {
        return map.entrySet();
    }

    public static Set<String> getSectionsNames() {
        return map.keySet();
    }

}