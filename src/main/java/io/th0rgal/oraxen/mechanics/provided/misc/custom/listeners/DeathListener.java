package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class DeathListener extends CustomListener {

    public DeathListener(String itemID, long cooldown, CustomEvent event, ClickAction clickAction) {
        super(itemID, cooldown, event, clickAction);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        for (ItemStack drop : event.getDrops()) {
            if (!itemID.equals(OraxenItems.getIdByItem(drop))) continue;
            perform(event.getEntity().getPlayer(), drop);
        }
    }
}
