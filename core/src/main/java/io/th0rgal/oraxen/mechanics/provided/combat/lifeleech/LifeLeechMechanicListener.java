package io.th0rgal.oraxen.mechanics.provided.combat.lifeleech;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class LifeLeechMechanicListener implements Listener {

    private final LifeLeechMechanicFactory factory;

    public LifeLeechMechanicListener(LifeLeechMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCall(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (!ProtectionLib.canInteract(damager, event.getEntity().getLocation())) return;

        String itemID = OraxenItems.getIdByItem(damager.getInventory().getItemInMainHand());
        if (!OraxenItems.exists(itemID)) return;
        LifeLeechMechanic mechanic = factory.getMechanic(itemID);
        if (mechanic == null) return;

        double maxHealth = damager.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        damager.setHealth(Math.min(damager.getHealth() + mechanic.getAmount(), maxHealth));
        livingEntity.setHealth(Math.max(livingEntity.getHealth() - mechanic.getAmount(), 0));
    }
}
