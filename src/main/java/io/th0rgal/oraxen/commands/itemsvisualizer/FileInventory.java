package io.th0rgal.oraxen.commands.itemsvisualizer;

import com.google.common.collect.Iterables;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FilenameUtils;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.utils.fastinv.FastInv;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class FileInventory extends FastInv {
    public FileInventory(int page) {
        super(3 * 9, "Oraxen item files visualizer");

        YamlConfiguration settings = new ResourcesManager(OraxenPlugin.get()).getSettings();

        boolean lastPage = false;
        Collection<File> collection = OraxenItems.getMap().keySet();
        for (int i = page * 2 * 9; i < (page + 1) * 2 * 9; i++) {
            if (i >= collection.size()) {
                lastPage = true;
                break;
            }
            File ymlFile = Iterables.get(collection, i);
            ItemStack itemStack = null;
            String material = settings
                    .getString(String.format("gui_inventory.%s", FilenameUtils.removeExtension(ymlFile.getName())),
                            "PAPER");
            if (material == null)
                material = "PAPER";

            try {
                itemStack = new ItemBuilder(OraxenItems.getItemById(material).build())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .setDisplayName(ChatColor.GREEN + ymlFile.getName())
                        .setLore(new ArrayList<>())
                        .build();
            } catch (Exception e) {
                try {
                    itemStack = new ItemBuilder(Material.getMaterial(material.toUpperCase()))
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .setDisplayName(ChatColor.GREEN + ymlFile.getName())
                            .build();
                } catch (Exception ignored) {
                    itemStack = new ItemBuilder(Material.PAPER)
                            .setDisplayName(ChatColor.GREEN + ymlFile.getName())
                            .build();
                }
            }

            if (itemStack == null)
                // avoid possible bug if isOraxenItems is available but can't be an itemstack
                itemStack = new ItemBuilder(Material.PAPER).setDisplayName(ChatColor.GREEN + ymlFile.getName()).build();
            setItem(i - page * 2 * 9, itemStack,
                    e -> new ItemsInventory(0, page, ymlFile).open((Player) e.getWhoClicked()));
        }

        setItem(getInventory().getSize() - 5,
                new ItemBuilder(Material.BARRIER).setDisplayName(ChatColor.RED + "close").build(),
                e -> e.getWhoClicked().closeInventory());

        if (page > 0)
            setItem(getInventory().getSize() - 6,
                    new ItemBuilder(Material.ARROW)
                            .setAmount(page)
                            .setDisplayName(ChatColor.YELLOW + "open page " + page)
                            .build(),
                    e -> new FileInventory(page - 1).open((Player) e.getWhoClicked()));

        if (!lastPage)
            setItem(getInventory().getSize() - 4,
                    new ItemBuilder(Material.ARROW)
                            .setAmount(page + 2)
                            .setDisplayName(ChatColor.RED + "open page " + (page + 2))
                            .build(),
                    e -> new FileInventory(page + 1).open((Player) e.getWhoClicked()));

    }
}
