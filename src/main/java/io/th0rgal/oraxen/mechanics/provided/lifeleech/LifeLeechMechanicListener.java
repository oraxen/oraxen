package io.th0rgal.oraxen.mechanics.provided.lifeleech;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class LifeLeechMechanicListener implements Listener {

    private final MechanicFactory factory;

    public LifeLeechMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCall(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;

        Player damager = (Player) event.getDamager();
        ItemStack item = damager.getInventory().getItemInMainHand();
        String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID))
            return;

        LifeLeechMechanic mechanic = (LifeLeechMechanic) factory.getMechanic(itemID);
        damager.setHealth(damager.getHealth() + mechanic.getAmount());
        LivingEntity damaged = (LivingEntity) event.getEntity();
        damaged.setHealth(damaged.getHealth() - mechanic.getAmount());
    }
}