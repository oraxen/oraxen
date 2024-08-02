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
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class ItemsView {

    private final YamlConfiguration settings = OraxenPlugin.get().getResourceManager().getSettings();

    PaginatedGui mainGui;

    public PaginatedGui create() {
        final Map<File, PaginatedGui> files = new HashMap<>();
        for (final File file : OraxenItems.getMap().keySet()) {
            final List<ItemBuilder> unexcludedItems = OraxenItems.getUnexcludedItems(file);
            if (!unexcludedItems.isEmpty())
                files.put(file, createSubGUI(file.getName(), unexcludedItems));
        }
        int rows = (int) Settings.ORAXEN_INV_ROWS.getValue();

        //Get max between the highest slot and the number of files
        int highestUsedSlot = files.keySet().stream()
                .map(this::getGuiItemSlot)
                .map(GuiItemSlot::slot)
                .max(Comparator.naturalOrder())
                .map(slot -> Math.max(slot, files.keySet().size() - 1))
                .orElse(files.keySet().size() - 1);
        GuiItem emptyGuiItem = new GuiItem(Material.AIR);
        List<GuiItem> guiItems = new ArrayList<>(Collections.nCopies(highestUsedSlot + 1, emptyGuiItem));

        for (Map.Entry<File, PaginatedGui> entry : files.entrySet()) {
            int slot = getGuiItemSlot(entry.getKey()).slot();
            if (slot == -1) continue;
            guiItems.set(slot, new GuiItem(getGuiItemSlot(entry.getKey()).itemStack, e -> entry.getValue().open(e.getWhoClicked())));
        }

        // Add all items without a specified slot to the earliest available slot
        for (Map.Entry<File, PaginatedGui> entry : files.entrySet()) {
            GuiItemSlot guiItemSlot = getGuiItemSlot(entry.getKey());
            if (guiItemSlot.slot != -1) continue;
            guiItems.set(guiItems.indexOf(emptyGuiItem), new GuiItem(guiItemSlot.itemStack(), e -> entry.getValue().open(e.getWhoClicked())));
        }

        mainGui = Gui.paginated().rows(rows).pageSize((int) Settings.ORAXEN_INV_SIZE.getValue()).title(Settings.ORAXEN_INV_TITLE.toComponent()).create();
        mainGui.addItem(guiItems.toArray(new GuiItem[]{}));

        ItemStack previousPage = (Settings.ORAXEN_INV_PREVIOUS_ICON.getValue() == null
                ? new ItemBuilder(Material.ARROW).setDisplayName("Previous Page")
                : OraxenItems.getItemById(Settings.ORAXEN_INV_PREVIOUS_ICON.toString())
        ).build();
        mainGui.setItem(6, 2, new GuiItem(previousPage, event -> {
            mainGui.previous();
            event.setCancelled(true);
        }));

        ItemStack nextPage = (Settings.ORAXEN_INV_NEXT_ICON.getValue() == null
                ? new ItemBuilder(Material.ARROW).setDisplayName("Next Page")
                : OraxenItems.getItemById(Settings.ORAXEN_INV_NEXT_ICON.toString())
        ).build();
        mainGui.setItem(6, 8, new GuiItem(nextPage, event -> {
            mainGui.next();
            event.setCancelled(true);
        }));

        ItemStack exitIcon = (Settings.ORAXEN_INV_EXIT.getValue() == null
                ? new ItemBuilder(Material.BARRIER).setDisplayName("Exit") :
                OraxenItems.getItemById(Settings.ORAXEN_INV_EXIT.toString())
        ).build();
        mainGui.setItem(6, 5, new GuiItem(exitIcon, event -> event.getWhoClicked().closeInventory()));

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
            if (ItemUtils.isEmpty(itemStack)) continue;

            gui.addItem(new GuiItem(itemStack, e -> e.getWhoClicked().getInventory().addItem(ItemUpdater.updateItem(e.getCurrentItem().clone()))));
        }

        ItemStack nextPage = (Settings.ORAXEN_INV_NEXT_ICON.getValue() == null
                ? new ItemBuilder(Material.ARROW) : OraxenItems.getItemById(Settings.ORAXEN_INV_NEXT_ICON.toString()))
                .setDisplayName("Next Page").build();
        ItemStack previousPage = (Settings.ORAXEN_INV_PREVIOUS_ICON.getValue() == null
                ? new ItemBuilder(Material.ARROW) : OraxenItems.getItemById(Settings.ORAXEN_INV_PREVIOUS_ICON.toString()))
                .setDisplayName("Previous Page").build();
        ItemStack exitIcon = (Settings.ORAXEN_INV_EXIT.getValue() == null
                ? new ItemBuilder(Material.BARRIER) : OraxenItems.getItemById(Settings.ORAXEN_INV_EXIT.toString()))
                .setDisplayName("Exit").build();

        if (gui.getPagesNum() > 1) {
            gui.setItem(6, 2, new GuiItem(previousPage, event -> gui.previous()));
            gui.setItem(6, 8, new GuiItem(nextPage, event -> gui.next()));
        }

        gui.setItem(6, 5, new GuiItem(exitIcon, event -> mainGui.open(event.getWhoClicked())));

        return gui;
    }

    private record GuiItemSlot(ItemStack itemStack, Integer slot) {

    }

    private GuiItemSlot getGuiItemSlot(final File file) {
        ItemStack itemStack;
        String fileName = Utils.removeExtension(file.getName());
        //Material of category itemstack. if no material is set, set it to the first item of the category
        Optional<String> icon = Optional.ofNullable(settings.getString(String.format("oraxen_inventory.menu_layout.%s.icon", fileName)));
        String displayName = settings.getString(String.format("oraxen_inventory.menu_layout.%s.displayname", fileName), "<green>" + file.getName());

        itemStack = icon.map(OraxenItems::getItemById).map(ItemBuilder::clone)
                .orElse(OraxenItems.getMap().get(file).values().stream().findFirst().orElse(new ItemBuilder(Material.PAPER)))
                .clone().addItemFlags(ItemFlag.HIDE_ATTRIBUTES).setItemName(displayName).setDisplayName(displayName).setLore(new ArrayList<>()).build();

        // avoid possible bug if isOraxenItems is available but can't be an itemstack
        if (itemStack == null) itemStack = new ItemBuilder(Material.PAPER).setDisplayName(displayName).build();
        int slot = settings.getInt(String.format("oraxen_inventory.menu_layout.%s.slot", Utils.removeExtension(file.getName())), 0) - 1;
        return new GuiItemSlot(itemStack, slot);
    }
}
