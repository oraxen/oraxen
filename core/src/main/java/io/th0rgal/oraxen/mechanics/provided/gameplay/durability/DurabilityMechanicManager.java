package io.th0rgal.oraxen.mechanics.provided.gameplay.durability;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;

public class DurabilityMechanicManager implements Listener {

    private final DurabilityMechanicFactory factory;

    public DurabilityMechanicManager(DurabilityMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamaged(PlayerItemDamageEvent event) {
        DurabilityMechanic mechanic = factory.getMechanic(event.getItem());
        if (mechanic == null || !mechanic.changeDurability(event.getItem(), -event.getDamage())) return;
        event.setDamage(0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemMend(PlayerItemMendEvent event) {
        DurabilityMechanic mechanic = factory.getMechanic(OraxenItems.getIdByItem(event.getItem()));
        if (mechanic == null || !mechanic.changeDurability(event.getItem(), event.getRepairAmount())) return;
        event.setRepairAmount(0);
    }
}
