package io.th0rgal.oraxen.utils.inventories;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemParser;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ItemsView {

    private final YamlConfiguration settings = OraxenPlugin.get().getResourceManager().getSettings();
    ChestGui mainGui;

    public ChestGui create() {
        final Map<File, ChestGui> files = new HashMap<>();
        for (final File file : OraxenItems.getMap().keySet()) {
            final List<ItemBuilder> unexcludedItems = OraxenItems.getUnexcludedItems(file);
            if (!unexcludedItems.isEmpty())
                files.put(file, createSubGUI(file.getName(), unexcludedItems));
        }
        mainGui = new ChestGui((int) Settings.ORAXEN_INV_ROWS.getValue(), Settings.ORAXEN_INV_TITLE.toString());
        final StaticPane filesPane = new StaticPane(0, 0, 9, mainGui.getRows());
        int i = 0;

        Set<Integer> usedSlots = files.keySet().stream().map(e -> getItemStack(e).getRight()).filter(e -> e > -1).collect(Collectors.toSet());

        for (final var entry : files.entrySet()) {
            Pair<ItemStack, Integer> itemSlotPair = getItemStack(entry.getKey());
            ItemStack itemStack = itemSlotPair.getLeft();
            int slot = itemSlotPair.getRight() > -1 ? itemSlotPair.getRight() : getUnusedSlot(i, usedSlots);
            final GuiItem item = new GuiItem(itemStack, event -> entry.getValue().show(event.getWhoClicked()));
            filesPane.addItem(item, Slot.fromXY(slot % 9, slot / 9));
            i++;
        }

        mainGui.addPane(filesPane);
        mainGui.setOnTopClick(event -> event.setCancelled(true));
        return mainGui;
    }

    private int getUnusedSlot(int i, Set<Integer> usedSlots) {
        int slot = usedSlots.contains(i) ? getUnusedSlot(i + 1, usedSlots) : i;
        usedSlots.add(slot);
        return slot;
    }

    private ChestGui createSubGUI(final String fileName, final List<ItemBuilder> items) {
        final int rows = Math.min((items.size() - 1) / 9 + 2, 6);
        final ChestGui gui = new ChestGui(6, settings.getString(
                String.format("oraxen_inventory.menu_layout.%s.title", Utils.removeExtension(fileName)), Settings.ORAXEN_INV_TITLE.toString())
                .replace("<main_menu_title>", Settings.ORAXEN_INV_TITLE.toString()));
        final PaginatedPane pane = new PaginatedPane(9, rows);

        for (int i = 0; i < (items.size() - 1) / 45 + 1; i++) {
            final List<ItemStack> itemStackList = extractPageItems(items, i);
            final StaticPane staticPane = new StaticPane(9, Math.min((itemStackList.size() - 1) / 9 + 1, 5));
            for (int itemIndex = 0; itemIndex < itemStackList.size(); itemIndex++) {
                final ItemStack oraxenItem = itemStackList.get(itemIndex);
                staticPane.addItem(new GuiItem(oraxenItem,
                                event -> event.getWhoClicked().getInventory().addItem(ItemUpdater.updateItem(oraxenItem))),
                        itemIndex % 9, itemIndex / 9);
            }
            pane.addPane(i, staticPane);
        }


        //page selection
        final StaticPane back = new StaticPane(2, 5, 1, 1);
        final StaticPane forward = new StaticPane(6, 5, 1, 1);
        final StaticPane exit = new StaticPane(4, 5, 9, 1);

        back.addItem(new GuiItem((OraxenItems.getItemById("arrow_previous_icon") == null
                ? new ItemBuilder(Material.ARROW)
                : OraxenItems.getItemById("arrow_previous_icon")).build(), event -> {
            pane.setPage(pane.getPage() - 1);

            if (pane.getPage() == 0) back.setVisible(false);

            forward.setVisible(true);
            gui.update();
        }), 0, 0);

        back.setVisible(false);

        forward.addItem(new GuiItem((OraxenItems.getItemById("arrow_next_icon") == null
                ? new ItemBuilder(Material.ARROW)
                : OraxenItems.getItemById("arrow_next_icon")).build(), event -> {
            pane.setPage(pane.getPage() + 1);
            if (pane.getPage() == pane.getPages() - 1) forward.setVisible(false);

            back.setVisible(true);
            gui.update();
        }), 0, 0);
        if (pane.getPages() <= 1)
            forward.setVisible(false);

        exit.addItem(new GuiItem((OraxenItems.getItemById("exit_icon") == null
                ? new ItemBuilder(Material.BARRIER)
                : OraxenItems.getItemById("exit_icon"))
                .build(), event ->
                mainGui.show(event.getWhoClicked())
        ), 0, 0);

        gui.addPane(back);
        gui.addPane(forward);
        gui.addPane(exit);
        gui.addPane(pane);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        return gui;
    }

    private List<ItemStack> extractPageItems(final List<ItemBuilder> items, final int page) {
        final List<ItemStack> output = new ArrayList<>();
        for (int i = page * 45; i < (page + 1) * 45 && i < items.size(); i++) output.add(items.get(i).build());
        return output;
    }

    private Pair<ItemStack, Integer> getItemStack(final File file) {
        ItemStack itemStack;
        String fileName = Utils.removeExtension(file.getName());
        String material = settings.getString(String.format("oraxen_inventory.menu_layout.%s.icon", fileName), "PAPER");
        String displayName = ItemParser.parseComponentDisplayName(settings.getString(String.format("oraxen_inventory.menu_layout.%s.displayname", fileName), "<green>" + file.getName()));
        try {
            itemStack = new ItemBuilder(OraxenItems.getItemById(material).getReferenceClone())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setDisplayName(displayName)
                    .setLore(new ArrayList<>())
                    .build();
        } catch (final Exception e) {
            try {
                itemStack = new ItemBuilder(Material.getMaterial(material.toUpperCase()))
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .setDisplayName(displayName)
                        .build();
            } catch (final Exception ignored) {
                itemStack = new ItemBuilder(Material.PAPER)
                        .setDisplayName(displayName)
                        .build();
            }
        }

        if (itemStack == null)
            // avoid possible bug if isOraxenItems is available but can't be an itemstack
            itemStack = new ItemBuilder(Material.PAPER).setDisplayName(displayName).build();

        return Pair.of(itemStack, settings.getInt(String.format("oraxen_inventory.menu_layout.%s.slot", Utils.removeExtension(file.getName())), -1) - 1);
    }
}
