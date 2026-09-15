package io.th0rgal.oraxen.mechanics.provided.combat.lifeleech;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.wrappers.AttributeWrapper;
import io.th0rgal.oraxen.protection.AntiGriefLib;

import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class LifeLeechMechanicListener implements Listener {

    private final MechanicFactory factory;

    public LifeLeechMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCall(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager))
            return;
        if (!(event.getEntity() instanceof LivingEntity livingEntity))
            return;
        if (!AntiGriefLib.canInteract(damager, event.getEntity().getLocation()))
            return;

        String itemID = OraxenItems.getIdByItem(damager.getInventory().getItemInMainHand());
        if (!OraxenItems.exists(itemID))
            return;
        LifeLeechMechanic mechanic = (LifeLeechMechanic) factory.getMechanic(itemID);
        if (mechanic == null)
            return;

        AttributeInstance damagerMaxHealth = damager.getAttribute(AttributeWrapper.MAX_HEALTH);
        AttributeInstance victimMaxHealth = livingEntity.getAttribute(AttributeWrapper.MAX_HEALTH);
        if (damagerMaxHealth == null || victimMaxHealth == null)
            return;

        int amount = mechanic.getAmount();
        damager.setHealth(clamp(damager.getHealth() + amount, damagerMaxHealth.getValue()));
        livingEntity.setHealth(clamp(livingEntity.getHealth() - amount, victimMaxHealth.getValue()));
    }

    private static double clamp(double health, double maxHealth) {
        return Math.max(0, Math.min(health, maxHealth));
    }
}
