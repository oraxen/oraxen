package io.th0rgal.oraxen.utils;

import com.google.common.collect.Iterables;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.settings.Message;
import io.th0rgal.oraxen.utils.fastinv.FastInv;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class ItemsInventory extends FastInv {

    public ItemsInventory(int page) {
        super(6 * 9, "Oraxen items visualizer");

        // Just add a random item
        Collection<ItemBuilder> collection = OraxenItems.getItems();
        for (int i = page * 5 * 9; i < (page + 1) * 5 * 9; i++) {
            if (i >= collection.size())
                break;
            ItemStack item = Iterables.get(collection, i).build();
            setItem(i, item, e -> giveItem(e.getWhoClicked(), item));
        }

        //close item
        setItem(getInventory().getSize() - 5,
                new ItemBuilder(Material.BARRIER)
                        .setDisplayName(ChatColor.RED + "close")
                        .build(),
                e -> e.getWhoClicked().closeInventory());

        setItem(getInventory().getSize() - 6,
                new ItemBuilder(Material.ARROW)
                        .setAmount(page - 1)
                        .setDisplayName(ChatColor.YELLOW + "previous page")
                        .build(),
                e -> new ItemsInventory(page - 1).open((Player) e.getWhoClicked()));

        setItem(getInventory().getSize() - 4,
                new ItemBuilder(Material.ARROW)
                        .setAmount(page + 1)
                        .setDisplayName(ChatColor.RED + "next page")
                        .build(),
                e -> new ItemsInventory(page + 1).open((Player) e.getWhoClicked()));

    }

    private void giveItem(HumanEntity humanEntity, ItemStack item) {
        if (humanEntity.hasPermission("oraxen.command.inv.give")) {
            if (humanEntity.getInventory().firstEmpty() != -1)
                humanEntity.getInventory().addItem(item);
            else {
                humanEntity.sendMessage("Your inventory was full, the item has been looted on the ground");
                humanEntity.getWorld().dropItem(humanEntity.getLocation(), item);
            }
        } else {
            Message.DONT_HAVE_PERMISSION.send(humanEntity, "oraxen.command.inv.give");
        }
    }

}
