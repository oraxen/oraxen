package io.th0rgal.oraxen.commands.itemsvisualizer;

import com.google.common.collect.Iterables;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.fastinv.FastInv;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Collection;

public class ItemsInventory extends FastInv {

    public ItemsInventory(int page, int fileInvPage, File file) {
        super(6 * 9, "Oraxen items in " + file.getName());

        boolean lastPage = false;
        Collection<ItemBuilder> collection = OraxenItems.getUnexcludedItems(file);
        for (int i = page * 5 * 9; i < (page + 1) * 5 * 9; i++) {
            if (i >= collection.size()) {
                lastPage = true;
                break;
            }
            ItemStack item = Iterables.get(collection, i).build();
            setItem(i - page * 5 * 9, item, e -> giveItem(e.getWhoClicked(), item));
        }

        // back item
        setItem(getInventory().getSize() - 5,
                new ItemBuilder(Material.BARRIER).setDisplayName(ChatColor.RED + "back").build(),
                e -> new FileInventory(fileInvPage).open((Player) e.getWhoClicked()));

        if (page > 0)
            setItem(getInventory().getSize() - 6,
                    new ItemBuilder(Material.ARROW)
                            .setAmount(page)
                            .setDisplayName(ChatColor.YELLOW + "open page " + page)
                            .build(),
                    e -> new ItemsInventory(page - 1, fileInvPage, file).open((Player) e.getWhoClicked()));

        if (!lastPage)
            setItem(getInventory().getSize() - 4,
                    new ItemBuilder(Material.ARROW)
                            .setAmount(page + 2)
                            .setDisplayName(ChatColor.RED + "open page " + (page + 2))
                            .build(),
                    e -> new ItemsInventory(page + 1, fileInvPage, file).open((Player) e.getWhoClicked()));

    }

    private void giveItem(HumanEntity humanEntity, ItemStack item) {
        AllItemsInventory.sharedGive(humanEntity, item);
    }

}
