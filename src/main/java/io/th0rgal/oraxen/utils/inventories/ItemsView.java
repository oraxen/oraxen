package io.th0rgal.oraxen.utils.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ItemsView {

    private final YamlConfiguration settings = new ResourcesManager(OraxenPlugin.get()).getSettings();
    PaginatedGui mainGui;

    public PaginatedGui create() {
        final Map<File, PaginatedGui> files = new HashMap<>();
        for (final File file : OraxenItems.getMap().keySet()) {
            final List<ItemBuilder> unexcludedItems = OraxenItems.getUnexcludedItems(file);
            if (!unexcludedItems.isEmpty())
                files.put(file, createSubGUI(file.getName(), unexcludedItems));
        }
        mainGui = Gui.paginated().rows((int) Settings.ORAXEN_INV_ROWS.getValue()).title(Settings.ORAXEN_INV_TITLE.toComponent()).create();
        mainGui.disableAllInteractions();
        int i = 0;

        Set<Integer> usedSlots = files.keySet().stream().map(e -> getItemStack(e).getRight()).filter(e -> e > -1).collect(Collectors.toSet());

        for (final var entry : files.entrySet()) {
            Pair<ItemStack, Integer> itemSlotPair = getItemStack(entry.getKey());
            ItemStack itemStack = itemSlotPair.getLeft();
            int slot = itemSlotPair.getRight() > -1 ? itemSlotPair.getRight() : getUnusedSlot(i, usedSlots);
            final GuiItem item = new GuiItem(itemStack);
            item.setAction(event -> entry.getValue().open(event.getWhoClicked()));
            mainGui.setItem(slot, item);
            i++;
        }

        return mainGui;
    }

    private int getUnusedSlot(int i, Set<Integer> usedSlots) {
        int slot = usedSlots.contains(i) ? getUnusedSlot(i + 1, usedSlots) : i;
        usedSlots.add(slot);
        return slot;
    }

    private PaginatedGui createSubGUI(final String fileName, final List<ItemBuilder> items) {
        final PaginatedGui gui = Gui.paginated().rows(6).title(AdventureUtils.MINI_MESSAGE.deserialize(settings.getString(
                        String.format("oraxen_inventory.menu_layout.%s.title", Utils.removeExtension(fileName)), Settings.ORAXEN_INV_TITLE.toString())
                .replace("<main_menu_title>", Settings.ORAXEN_INV_TITLE.toString()))).create();

        for (int i = 0; i < (items.size() - 1) / 45 + 1; i++) {
            final List<ItemStack> itemStackList = extractPageItems(items, i);
            for (int itemIndex = 0; itemIndex < itemStackList.size(); itemIndex++) {
                GuiItem guiItem = new GuiItem(itemStackList.get(itemIndex));
                guiItem.setAction(e -> e.getWhoClicked().getInventory().addItem(ItemUpdater.updateItem(guiItem.getItemStack())));
                gui.setItem(itemIndex, guiItem);
            }
        }

        //page selection
        if (gui.getCurrentPageNum() > 1) {
            gui.setItem(6, 2, new GuiItem((OraxenItems.exists("arrow_previous_icon")
                    ? new ItemBuilder(Material.ARROW) : OraxenItems.getItemById("arrow_previous_icon"))
                    .build(), event -> gui.previous()));
        }

        if (gui.getPagesNum() > 1 && gui.getNextPageNum() != gui.getCurrentPageNum()) {
            gui.setItem(6, 7, new GuiItem((OraxenItems.exists("arrow_next_icon")
                    ? new ItemBuilder(Material.ARROW) : OraxenItems.getItemById("arrow_next_icon"))
                    .build(), event -> gui.next()));
        }

        gui.setItem(6, 5, new GuiItem((OraxenItems.exists("exit_icon")
                ? new ItemBuilder(Material.BARRIER) : OraxenItems.getItemById("exit_icon"))
                .build(), event -> mainGui.open(event.getWhoClicked())
        ));

        return gui;
    }

    private List<ItemStack> extractPageItems(final List<ItemBuilder> items, final int page) {
        final List<ItemStack> output = new ArrayList<>();
        for (int i = page * 45; i < (page + 1) * 45 && i < items.size(); i++) output.add(items.get(i).build());
        return output;
    }

    private Pair<ItemStack, Integer> getItemStack(final File file) {
        ItemStack itemStack;
        String material = settings.getString(String.format("oraxen_inventory.menu_layout.%s.icon", Utils.removeExtension(file.getName())), "PAPER");

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

        return Pair.of(itemStack, settings.getInt(String.format("oraxen_inventory.menu_layout.%s.slot", Utils.removeExtension(file.getName())), -1) - 1);
    }
}
