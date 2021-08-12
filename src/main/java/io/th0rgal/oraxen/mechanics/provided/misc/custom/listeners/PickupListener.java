package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomAction;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomCondition;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PickupListener extends CustomListener {


    public PickupListener(String itemID, CustomEvent event,
                          List<CustomCondition> conditions, List<CustomAction> actions) {
        super(itemID, event, conditions, actions);
    }

    @EventHandler
    public void onPickedUp(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack item = event.getItem().getItemStack();
            if (!itemID.equals(OraxenItems.getIdByItem(item)))
                return;
            perform(player, item);
        }
    }

}
