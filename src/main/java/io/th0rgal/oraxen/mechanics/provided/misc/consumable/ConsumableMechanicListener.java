package io.th0rgal.oraxen.mechanics.provided.misc.consumable;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ConsumableMechanicListener implements Listener {

    private final ConsumableMechanicFactory factory;

    public ConsumableMechanicListener(ConsumableMechanicFactory factory) {
        this.factory = factory;
    }

    // HIGH lets protection plugins on NORMAL or lower deny the click before the item is used up.
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemUsed(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.useItemInHand() == Event.Result.DENY) return;

        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand();
        String itemID = OraxenItems.getIdByItem(item);
        if (item == null || hand == null || factory.isNotImplementedIn(itemID)) return;

        int amount = item.getAmount() - 1;
        event.getPlayer().getInventory().setItem(hand, amount > 0 ? item.asQuantity(amount) : null);
    }

}
