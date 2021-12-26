package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomAction;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomCondition;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DropListener extends CustomListener {


    public DropListener(String itemID, long cooldown, CustomEvent event,
                        List<CustomCondition> conditions, List<CustomAction> actions) {
        super(itemID, cooldown, event, conditions, actions);
    }

    @EventHandler
    public void onDropped(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!itemID.equals(OraxenItems.getIdByItem(item)))
            return;
        perform(event.getPlayer(), item);
    }

}
