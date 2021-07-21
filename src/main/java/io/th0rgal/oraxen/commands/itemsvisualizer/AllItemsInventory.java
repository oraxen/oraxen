package io.th0rgal.oraxen.commands.itemsvisualizer;

import com.google.common.collect.Iterables;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.fastinv.FastInv;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class AllItemsInventory extends FastInv {

    public AllItemsInventory(int page) {
        super(6 * 9, "Oraxen items visualizer");

        boolean lastPage = false;
        Collection<ItemBuilder> collection = OraxenItems.getUnexcludedItems();
        for (int i = page * 5 * 9; i < (page + 1) * 5 * 9; i++) {
            if (i >= collection.size()) {
                lastPage = true;
                break;
            }
            ItemStack item = Iterables.get(collection, i).build();
            setItem(i - page * 5 * 9, item, e -> giveItem(e.getWhoClicked(), item));
        }

        // close item
        setItem(getInventory().getSize() - 5,
                new ItemBuilder(Material.BARRIER).setDisplayName(ChatColor.RED + "close").build(),
                e -> e.getWhoClicked().closeInventory());

        if (page > 0)
            setItem(getInventory().getSize() - 6,
                    new ItemBuilder(Material.ARROW)
                            .setAmount(page)
                            .setDisplayName(ChatColor.YELLOW + "open page " + page)
                            .build(),
                    e -> new AllItemsInventory(page - 1).open((Player) e.getWhoClicked()));

        if (!lastPage)
            setItem(getInventory().getSize() - 4,
                    new ItemBuilder(Material.ARROW)
                            .setAmount(page + 2)
                            .setDisplayName(ChatColor.RED + "open page " + (page + 2))
                            .build(),
                    e -> new AllItemsInventory(page + 1).open((Player) e.getWhoClicked()));

    }

    private void giveItem(HumanEntity humanEntity, ItemStack item) {
        sharedGive(humanEntity, item);
    }

    static void sharedGive(HumanEntity humanEntity, ItemStack item) {
        if (humanEntity.hasPermission("oraxen.command.inventory.give")) {
            if (humanEntity.getInventory().firstEmpty() != -1)
                humanEntity.getInventory().addItem(item);
            else {
                humanEntity.sendMessage("Your inventory was full, the item has been looted on the ground");
                humanEntity.getWorld().dropItem(humanEntity.getLocation(), item);
            }
        } else {
            Message.NO_PERMISSION.send(humanEntity, "permission", "oraxen.command.inventory.give");
        }
    }

}
