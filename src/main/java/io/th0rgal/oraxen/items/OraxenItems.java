package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ConfigsManager;

import io.th0rgal.oraxen.config.Message;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OraxenItems {

    // configuration sections : their OraxenItem wrapper
    private static Map<File, Map<String, ItemBuilder>> map;
    public static final NamespacedKey ITEM_ID = new NamespacedKey(OraxenPlugin.get(), "id");
    private static ConfigsManager configsManager;
    private static String[] items;

    public static void loadItems(ConfigsManager configsManager) {
        OraxenItems.configsManager = configsManager;
        loadItems();
    }

    public static void loadItems() {
        map = configsManager.parsesConfigs();
        List<String> itemsList = new ArrayList<>();
        for (Map<String, ItemBuilder> subMap : map.values())
            itemsList.addAll(subMap.keySet());
        items = itemsList.toArray(new String[0]);
    }

    public static String getIdByItem(ItemBuilder item) {
        return item.getCustomTag(ITEM_ID, PersistentDataType.STRING);
    }

    public static String getIdByItem(ItemStack item) {
        return (item == null || !item.hasItemMeta() || item.getItemMeta().getPersistentDataContainer().isEmpty()) ? null
            : item.getItemMeta().getPersistentDataContainer().get(ITEM_ID, PersistentDataType.STRING);
    }

    public static boolean exists(String itemId) {
        return entryStream().anyMatch(entry -> entry.getKey().equals(itemId));
    }

    public static Optional<ItemBuilder> getOptionalItemById(String id) {
        return entryStream().filter(entry -> entry.getKey().equals(id)).findFirst().map(Entry::getValue);
    }

    public static ItemBuilder getItemById(String id) {
        return getOptionalItemById(id).orElse(null);
    }

    public static List<ItemBuilder> getUnexcludedItems() {
        return itemStream()
            .filter(item -> !item.getOraxenMeta().isExcludedFromInventory())
            .collect(Collectors.toList());
    }

    public static List<ItemBuilder> getUnexcludedItems(File file) {
        return map
            .get(file)
            .values()
            .stream()
            .filter(item -> !item.getOraxenMeta().isExcludedFromInventory())
            .collect(Collectors.toList());
    }

    public static List<ItemStack> getItemStacksByName(List<List<String>> lists) {
        return lists.stream().flatMap(list -> {
            ItemStack[] itemStack = new ItemStack[] { new ItemStack(Material.AIR) };
            list.stream().map(line -> line.split(":")).forEach(param -> {
                switch (param[0].toLowerCase()) {
                case "type":
                    if (exists(param[1]))
                        itemStack[0] = getItemById(param[1]).build().clone();
                    else
                        Message.ITEM_NOT_FOUND.log("item", param[1]);
                    break;
                case "amount":
                    itemStack[0].setAmount(Integer.parseInt(param[1]));
                    break;
                default:
                    break;
                }
            });
            return Stream.of(itemStack[0]);
        }).collect(Collectors.toList());
    }

    public static Map<File, Map<String, ItemBuilder>> getMap() {
        return map;
    }

    public static Map<String, ItemBuilder> getEntriesAsMap() {
        return entryStream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public static Set<Entry<String, ItemBuilder>> getEntries() {
        return entryStream().collect(Collectors.toSet());
    }

    public static Collection<ItemBuilder> getItems() {
        return itemStream().collect(Collectors.toList());
    }

    @Deprecated
    public static Set<String> getSectionsNames() {
        return getNames();
    }

    public static Set<String> getNames() {
        return nameStream().collect(Collectors.toSet());
    }

    public static String[] nameArray() {
        return nameStream().toArray(String[]::new);
    }

    public static Stream<String> nameStream() {
        return entryStream().map(Entry::getKey);
    }

    public static Stream<ItemBuilder> itemStream() {
        return entryStream().map(Entry::getValue);
    }

    public static Stream<Entry<String, ItemBuilder>> entryStream() {
        return map.values().stream().flatMap(map -> map.entrySet().stream());
    }

    public static String[] getItemNames() {
        return items;
    }

}