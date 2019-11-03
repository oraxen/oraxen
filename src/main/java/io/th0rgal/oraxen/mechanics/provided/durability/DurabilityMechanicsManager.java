package io.th0rgal.oraxen.mechanics.provided.durability;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class DurabilityMechanicsManager implements Listener {

    private DurabilityMechanicFactory factory;

    public DurabilityMechanicsManager(DurabilityMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onItemDamaged(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        DurabilityMechanic durabilityMechanic = (DurabilityMechanic) factory.getMechanic(itemID);

        ItemMeta itemMeta = item.getItemMeta();
        PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
        int realDurabilityLeft = persistentDataContainer.get(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER) - event.getDamage();

        if (realDurabilityLeft > 0) {
            double realMaxDurability = durabilityMechanic.getItemMaxDurability(); //because int rounded values suck
            persistentDataContainer.set(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER, realDurabilityLeft);
            ((Damageable) itemMeta).setDamage((int) (item.getType().getMaxDurability() - realDurabilityLeft / realMaxDurability * item.getType().getMaxDurability()));
            item.setItemMeta(itemMeta);
        } else {
            item.setAmount(0);
        }

    }

}
