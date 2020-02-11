package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.settings.ConfigsManager;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

public class OraxenItems {

    //configuration sections : their OraxenItem wrapper
    private static Map<String, ItemBuilder> map;
    public static final NamespacedKey ITEM_ID = new NamespacedKey(OraxenPlugin.get(), "id");

    public static void loadItems(ConfigsManager configsManager) {
        map = configsManager.parsesConfigs();
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

    public static List<ItemBuilder> getUnexcludedItems() {
        List<ItemBuilder> unexcludedItems = new ArrayList<>();
        for (ItemBuilder itemBuilder : getItems())
            if (!itemBuilder.getOraxenMeta().isExcludedFromInventory())
                unexcludedItems.add(itemBuilder);
            return unexcludedItems;
    }

    public static Set<Map.Entry<String, ItemBuilder>> getEntries() {
        return map.entrySet();
    }

    public static Set<String> getSectionsNames() {
        return map.keySet();
    }

}