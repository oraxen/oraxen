package io.th0rgal.oraxen.utils.inventories;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FilenameUtils;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemsView {

    private final YamlConfiguration settings = new ResourcesManager(OraxenPlugin.get()).getSettings();
    ChestGui mainGui;

    public ChestGui create() {
        final Map<File, ChestGui> files = new HashMap<>();
        for (final File file : OraxenItems.getMap().keySet()) {
            final List<ItemBuilder> unexcludedItems = OraxenItems.getUnexcludedItems(file);
            if (unexcludedItems.size() > 0)
                files.put(file, createSubGUI(file.getName(), unexcludedItems));
        }
        final int rows = (files.size() - 1) / 9 + 1;
        mainGui = new ChestGui(rows, "Oraxen Inventory");
        final StaticPane filesPane = new StaticPane(0, 0, 9, rows);
        int i = 0;
        for (final var entry : files.entrySet()) {
            final GuiItem item = new GuiItem(getItemStack(entry.getKey()), event ->
                    entry.getValue().show(event.getWhoClicked()));
            filesPane.addItem(item, i % 9, i / 9);
            i++;
        }

        mainGui.addPane(filesPane);
        mainGui.setOnGlobalClick(event -> event.setCancelled(true));
        return mainGui;
    }

    private ChestGui createSubGUI(final String fileName, final List<ItemBuilder> items) {
        final int rows = Math.min((items.size() - 1) / 9 + 2, 6);
        final ChestGui gui = new ChestGui(rows, "Oraxen items in " + fileName);
        final PaginatedPane pane = new PaginatedPane(9, rows);

        for (int i = 0; i < (items.size() - 1) / 54 + 1; i++) {
            final List<ItemStack> itemStackList = extractPageItems(items, i);
            final StaticPane staticPane = new StaticPane(9, Math.min((itemStackList.size() - 1) / 9 + 1, 4));
            for (int itemIndex = 0; itemIndex < itemStackList.size(); itemIndex++) {
                final ItemStack oraxenItem = itemStackList.get(itemIndex);
                staticPane.addItem(new GuiItem(oraxenItem,
                                event -> event.getWhoClicked().getInventory().addItem(oraxenItem)),
                        itemIndex % 9, itemIndex / 9);
            }
            pane.addPane(i, staticPane);
        }

        final StaticPane footer = new StaticPane(0, rows - 1, 9, 1);
        footer.addItem(new GuiItem(
                new ItemBuilder(Material.BARRIER)
                        .setDisplayName(Message.BACK_TO_MAIN_MENU.toSerializedString())
                        .build(), event ->
                mainGui.show(event.getWhoClicked())
        ), 4, 0);
        gui.addPane(footer);
        gui.addPane(pane);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        return gui;
    }

    private List<ItemStack> extractPageItems(final List<ItemBuilder> items, final int page) {
        final List<ItemStack> output = new ArrayList<>();
        for (int i = page * 54; i < (page + 1) * 54 && i < items.size(); i++) output.add(items.get(i).build());
        return output;
    }

    private ItemStack getItemStack(final File file) {
        ItemStack itemStack;
        String material = settings
                .getString(String.format("gui_inventory.%s", FilenameUtils.removeExtension(file.getName())),
                        "PAPER");
        if (material == null)
            material = "PAPER";

        try {
            itemStack = new ItemBuilder(OraxenItems.getItemById(material).build())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setDisplayName(ChatColor.GREEN + file.getName())
                    .setLore(new ArrayList<>())
                    .build();
        } catch (final Exception e) {
            try {
                itemStack = new ItemBuilder(Material.getMaterial(material.toUpperCase()))
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .setDisplayName(ChatColor.GREEN + file.getName())
                        .build();
            } catch (final Exception ignored) {
                itemStack = new ItemBuilder(Material.PAPER)
                        .setDisplayName(ChatColor.GREEN + file.getName())
                        .build();
            }
        }

        if (itemStack == null)
            // avoid possible bug if isOraxenItems is available but can't be an itemstack
            itemStack = new ItemBuilder(Material.PAPER).setDisplayName(ChatColor.GREEN + file.getName()).build();

        return itemStack;
    }
}
