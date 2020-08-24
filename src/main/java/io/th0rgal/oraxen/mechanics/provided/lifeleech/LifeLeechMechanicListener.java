package io.th0rgal.oraxen.mechanics.provided.lifeleech;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;

import org.bukkit.attribute.Attribute;
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCall(EntityDamageByEntityEvent event) {

        if (event.isCancelled())
            return;
        if (!(event.getDamager() instanceof Player))
            return;

        Player damager = (Player) event.getDamager();
        ItemStack item = damager.getInventory().getItemInMainHand();
        String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID))
            return;

        LifeLeechMechanic mechanic = (LifeLeechMechanic) factory.getMechanic(itemID);
        double health = damager.getHealth() + mechanic.getAmount();
        /**
         * double maxHealth =
         * damager.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue();
         *
         * Fixe d'un bug duquel le maxHealth n'est pas bien compté. Fixe d'un second byg
         * que le GENERIC_MAX_HEALTH avec getDefaultValue ne dépasse pas les 20
         */

        double maxHealth = damager.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

        damager.setHealth(Math.min(health, maxHealth));
        LivingEntity damaged = (LivingEntity) event.getEntity();
        /**
         * Could not pass event EntityDamageByEntityEvent to Oraxen v1.35.2
         * java.lang.IllegalArgumentException: Health must be between 0 and 20.0, but
         * was -1.8572006225585938. (attribute base value: 20.0) at
         * org.bukkit.craftbukkit.v1_15_R1.entity.CraftLivingEntity.setHealth(CraftLivingEntity.java:112)
         * ~[server.jar:git-Purpur-"6158389"]
         */
        damaged
            .setHealth(
                damaged.getHealth() - mechanic.getAmount() <= 0 ? 0.0 : damaged.getHealth() - mechanic.getAmount());
    }
}
