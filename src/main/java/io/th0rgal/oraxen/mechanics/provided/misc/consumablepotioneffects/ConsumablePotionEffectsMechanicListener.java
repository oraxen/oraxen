package io.th0rgal.oraxen.mechanics.provided.misc.consumablepotioneffects;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class ConsumablePotionEffectsMechanicListener implements Listener {

    private final ConsumablePotionEffectsMechanicFactory factory;

    public ConsumablePotionEffectsMechanicListener(ConsumablePotionEffectsMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemConsumed(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem()  ;
        String itemID = OraxenItems.getIdByItem(item);
        if (!factory.isNotImplementedIn(itemID)) {
            ConsumablePotionEffectsMechanic mechanic = (ConsumablePotionEffectsMechanic) factory.getMechanic(itemID);
            mechanic.onItemPlaced(event.getPlayer());
        }
    }


}
