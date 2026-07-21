package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class DropListener extends CustomListener {


    public DropListener(String itemID, long cooldown, CustomEvent event, ClickAction clickAction) {
        super(itemID, cooldown, event, clickAction);
    }

    @EventHandler
    public void onDropped(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (!itemID.equals(OraxenItems.getIdByItem(item))) return;
        perform(event.getPlayer(), item);
    }

}
