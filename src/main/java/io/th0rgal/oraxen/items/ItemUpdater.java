package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;

public class ItemUpdater implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        if(!Settings.AUTO_UPDATE_ITEMS.toBool())
            return;
        PlayerInventory inventory = event.getPlayer().getInventory();
        for (int i = 0;i < inventory.getSize();i++) {
            ItemStack oldItem = inventory.getItem(i);
            ItemStack newItem = ItemUpdater.updateItem(oldItem);
            if(oldItem == null || oldItem.equals(newItem))
                continue;
            inventory.setItem(i, newItem);
        }
    }


    public static ItemStack updateItem(ItemStack item){
        String id = OraxenItems.getIdByItem(item);
        if(id == null)
            return item;
        Optional<ItemBuilder> newItemBuilder = OraxenItems.getOptionalItemById(id);

        if(newItemBuilder.isEmpty() || newItemBuilder.get().getOraxenMeta().isNoUpdate())
            return item;

        ItemStack newItem = newItemBuilder.get().build();
        newItem.setAmount(item.getAmount());
        return newItem;
    }

}
