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
        DurabilityMechanic mechanic = (DurabilityMechanic) factory.getMechanic(OraxenItems.getIdByItem(event.getItem()));
        if (mechanic == null) return;
        mechanic.changeDurability(event.getItem(), -event.getDamage());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemMend(PlayerItemMendEvent event) {
        DurabilityMechanic mechanic = (DurabilityMechanic) factory.getMechanic(OraxenItems.getIdByItem(event.getItem()));
        if (mechanic == null) return;
        mechanic.changeDurability(event.getItem(), event.getRepairAmount());
    }
}
