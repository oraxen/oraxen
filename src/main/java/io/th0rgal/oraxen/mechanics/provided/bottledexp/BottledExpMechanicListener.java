package io.th0rgal.oraxen.mechanics.provided.bottledexp;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BottledExpMechanicListener implements Listener {

    private final MechanicFactory factory;

    public BottledExpMechanicListener(BottledExpMechanicFactory factory) {
        this.factory = factory;
    }

    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR
                && action != Action.RIGHT_CLICK_AIR
                && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        if (item == null)
            return;

        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        BottledExpMechanic mechanic = (BottledExpMechanic) factory.getMechanic(itemID);
    }

}
