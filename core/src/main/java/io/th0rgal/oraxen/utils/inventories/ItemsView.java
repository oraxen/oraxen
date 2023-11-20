package io.th0rgal.oraxen.utils.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
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

public class ItemsView {

    private final YamlConfiguration settings = OraxenPlugin.get().resourceManager().settings();
    PaginatedGui mainGui;

    public PaginatedGui create() {
        final Map<File, PaginatedGui> files = new HashMap<>();
        for (final File file : OraxenItems.getMap().keySet()) {
            final List<ItemBuilder> unexcludedItems = OraxenItems.getUnexcludedItems(file);
            if (!unexcludedItems.isEmpty())
                files.put(file, createSubGUI(file.getName(), unexcludedItems));
        }
        int rows = (int) Settings.ORAXEN_INV_ROWS.getValue();
        mainGui = Gui.paginated().pageSize((rows - 1) * 9).rows(rows).title(Settings.ORAXEN_INV_TITLE.toComponent()).create();
        mainGui.disableAllInteractions();

        // Make a list of all slots to allow using mainGui.addItem easier
        List<GuiItem> pageItems = new ArrayList<>(Collections.nCopies(files.size(), null));
        // Make a list of all used slots to avoid using them later
        for (Map.Entry<File, PaginatedGui> entry : files.entrySet()) {
            int slot = getItemStack(entry.getKey()).getRight();
            if (slot == -1) continue;
            GuiItem guiItem = new GuiItem(getItemStack(entry.getKey()).getLeft(), e -> entry.getValue().open(e.getWhoClicked()));
            pageItems.add(slot, guiItem);
        }

        // Add all items without a specified slot to the earliest available slot
        for (Map.Entry<File, PaginatedGui> entry : files.entrySet()) {
            if (getItemStack(entry.getKey()).getRight() != -1) continue;
            pageItems.add(new GuiItem(getItemStack(entry.getKey()).getLeft(), e -> entry.getValue().open(e.getWhoClicked())));
        }

        mainGui.addItem(pageItems.stream().filter(Objects::nonNull).toArray(GuiItem[]::new));

        if (mainGui.getPagesNum() > 1) {
            mainGui.setItem(6, 2, new GuiItem((OraxenItems.exists("arrow_previous_icon")
                    ? new ItemBuilder(Material.ARROW).setDisplayName(AdventureUtils.parseLegacyThroughMiniMessage("<gray>Previous page"))
                    : OraxenItems.getItemById("arrow_previous_icon")).build(), event -> mainGui.previous()
            ));

            mainGui.setItem(6, 8, new GuiItem((OraxenItems.exists("arrow_next_icon")
                    ? new ItemBuilder(Material.ARROW).setDisplayName(AdventureUtils.parseLegacyThroughMiniMessage("<gray>Next page"))
                    : OraxenItems.getItemById("arrow_next_icon")).build(), event -> mainGui.next()
            ));
        }

        mainGui.setItem(6, 5, new GuiItem((OraxenItems.exists("exit_icon")
                ? new ItemBuilder(Material.BARRIER).setDisplayName(AdventureUtils.parseLegacyThroughMiniMessage("<red>Exit"))
                : OraxenItems.getItemById("exit_icon")).build(), event -> event.getWhoClicked().closeInventory())
        );

        return mainGui;
    }

    private PaginatedGui createSubGUI(final String fileName, final List<ItemBuilder> items) {
        final PaginatedGui gui = Gui.paginated().rows(6).pageSize(45).title(AdventureUtils.MINI_MESSAGE.deserialize(settings.getString(
                        String.format("oraxen_inventory.menu_layout.%s.title", Utils.removeExtension(fileName)), Settings.ORAXEN_INV_TITLE.toString())
                .replace("<main_menu_title>", Settings.ORAXEN_INV_TITLE.toString()))).create();
        gui.disableAllInteractions();

        for (ItemBuilder builder : items) {
            if (builder == null) continue;
            ItemStack itemStack = builder.build();
            if (itemStack == null || itemStack.getType().isAir()) continue;

            GuiItem guiItem = new GuiItem(itemStack);
            guiItem.setAction(e -> e.getWhoClicked().getInventory().addItem(ItemUpdater.updateItem(guiItem.getItemStack())));
            gui.addItem(guiItem);
        }

        if (gui.getPagesNum() > 1) {
            gui.setItem(6, 2, new GuiItem((OraxenItems.exists("arrow_previous_icon")
                    ? new ItemBuilder(Material.ARROW).setDisplayName(AdventureUtils.parseLegacyThroughMiniMessage("<gray>Previous page"))
                    : OraxenItems.getItemById("arrow_previous_icon")).build(), event -> gui.previous()
            ));

            gui.setItem(6, 8, new GuiItem((OraxenItems.exists("arrow_next_icon")
                    ? new ItemBuilder(Material.ARROW).setDisplayName(AdventureUtils.parseLegacyThroughMiniMessage("<gray>Next page"))
                    : OraxenItems.getItemById("arrow_next_icon")).build(), event -> gui.next()
            ));
        }

        gui.setItem(6, 5, new GuiItem((OraxenItems.exists("exit_icon")
                ? new ItemBuilder(Material.BARRIER).setDisplayName(AdventureUtils.parseLegacyThroughMiniMessage("<red>Exit"))
                : OraxenItems.getItemById("exit_icon")).build(), event -> mainGui.open(event.getWhoClicked())
        ));

        return gui;
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

        // avoid possible bug if isOraxenItems is available but can't be an itemstack
        if (itemStack == null) itemStack = new ItemBuilder(Material.PAPER).setDisplayName(ChatColor.GREEN + file.getName()).build();
        int slot = settings.getInt(String.format("oraxen_inventory.menu_layout.%s.slot", Utils.removeExtension(file.getName())), -1) - 1;
        return Pair.of(itemStack, Math.max(slot, -1));
    }
}
