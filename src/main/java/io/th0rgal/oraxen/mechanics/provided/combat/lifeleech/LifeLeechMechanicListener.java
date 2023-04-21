package io.th0rgal.oraxen.mechanics.provided.combat.lifeleech;

import io.th0rgal.oraxen.api.OraxenItems;
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
        if (!(event.getDamager() instanceof Player damager))
            return;

        ItemStack item = damager.getInventory().getItemInMainHand();
        String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID))
            return;

        LifeLeechMechanic mechanic = (LifeLeechMechanic) factory.getMechanic(itemID);
        double health = damager.getHealth() + mechanic.getAmount();
        double maxHealth = damager.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

        if (event.getEntity() instanceof LivingEntity damaged) {
            if(damaged.hasMetadata("NPC")){
                // Its a Citizens NPC so just ignore
                return;
            }

            // If its not a Citizens NPC then only give the health to damager
            damager.setHealth(Math.min(health, maxHealth));

            
            damaged
                    .setHealth(
                            damaged.getHealth() - mechanic.getAmount() <= 0
                                    ? 0.0 : damaged.getHealth() - mechanic.getAmount());
        }
    }
}
