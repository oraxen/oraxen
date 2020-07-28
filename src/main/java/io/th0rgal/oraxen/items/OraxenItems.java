package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.settings.ConfigsManager;

import io.th0rgal.oraxen.settings.MessageOld;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class OraxenItems {

    //configuration sections : their OraxenItem wrapper
    private static Map<File, Map<String, ItemBuilder>> map;
    public static final NamespacedKey ITEM_ID = new NamespacedKey(OraxenPlugin.get(), "id");
    private static ConfigsManager configsManager;

    public static void loadItems(ConfigsManager configsManager) {
        OraxenItems.configsManager = configsManager;
        loadItems();
    }

    public static void loadItems() {
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
        return getAllItems().containsKey(itemID);
    }

    public static ItemBuilder getItemById(String id) {
        return getAllItems().get(id);
    }

    public static Collection<ItemBuilder> getItems() {
        return getAllItems().values();
    }

    public static List<ItemBuilder> getUnexcludedItems() {
        List<ItemBuilder> unexcludedItems = new ArrayList<>();
        for (ItemBuilder itemBuilder : getItems())
            if (!itemBuilder.getOraxenMeta().isExcludedFromInventory())
                unexcludedItems.add(itemBuilder);
        return unexcludedItems;
    }

    public static List<ItemBuilder> getUnexcludedItems(File file) {
        List<ItemBuilder> unexcludedItems = new ArrayList<>();
        for (ItemBuilder itemBuilder : map.get(file).values())
            if (!itemBuilder.getOraxenMeta().isExcludedFromInventory())
                unexcludedItems.add(itemBuilder);
        return unexcludedItems;
    }

    public static List<ItemStack> getItemStacksByName(List<List<String>> lists) {
        List<ItemStack> itemStacks = new ArrayList<>();
        for (List<String> itemsList : lists) {
            ItemStack itemStack = new ItemStack(Material.AIR);
            for (String line : itemsList) {
                String[] params = line.split(":");
                if (params[0].equalsIgnoreCase("type"))
                    if (OraxenItems.isAnItem(params[1]))
                        itemStack = getItemById(params[1]).build().clone();
                    else {
                        MessageOld.ITEM_NOT_FOUND.logError(params[1]);
                        break;
                    }
                else if (params[0].equalsIgnoreCase("amount"))
                    itemStack.setAmount(Integer.parseInt(params[1]));
            }
            itemStacks.add(itemStack);
        }
        return itemStacks;
    }

    public static Set<Map.Entry<String, ItemBuilder>> getEntries() {
        return getAllItems().entrySet();
    }

    public static Set<String> getSectionsNames() {
        return getAllItems().keySet();
    }

    public static Map<File, Map<String, ItemBuilder>> getMap(){
        return map;
    }

    public static Map<String, ItemBuilder> getAllItems(){
        Map<String, ItemBuilder> items = new LinkedHashMap<>();
        map.values().forEach(items::putAll);
        return items;
    }

}