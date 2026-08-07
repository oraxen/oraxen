package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class PickupListener extends CustomListener {


    public PickupListener(String itemID, long cooldown, CustomEvent event, ClickAction clickAction) {
        super(itemID, cooldown, event, clickAction);
    }

    @EventHandler
    public void onPickedUp(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (!itemID.equals(OraxenItems.getIdByItem(item)))
            return;
        perform(player, item);
    }

}
