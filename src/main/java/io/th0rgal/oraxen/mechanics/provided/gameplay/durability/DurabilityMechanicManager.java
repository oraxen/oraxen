package io.th0rgal.oraxen.mechanics.provided.gameplay.durability;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class DurabilityMechanicManager implements Listener {

    private final DurabilityMechanicFactory factory;

    public DurabilityMechanicManager(DurabilityMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamaged(PlayerItemDamageEvent event) {
        changeDurability(event.getItem(), -event.getDamage());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemMend(PlayerItemMendEvent event) {
        changeDurability(event.getItem(), event.getRepairAmount());
    }

    public void changeDurability(ItemStack item, int amount) {
        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        DurabilityMechanic durabilityMechanic = (DurabilityMechanic) factory.getMechanic(itemID);

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return;
        PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
        if (persistentDataContainer.has(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER)) {
            int realDurabilityLeft = persistentDataContainer
                    .get(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER) + amount;
            if (realDurabilityLeft > 0) {
                persistentDataContainer
                        .set(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER, realDurabilityLeft);

                if(!(itemMeta instanceof Damageable)) return;
                double realMaxDurability = durabilityMechanic.getItemMaxDurability(); // because int rounded values suck

                ((Damageable) itemMeta)
                        .setDamage((int) (item.getType().getMaxDurability()
                                - realDurabilityLeft / realMaxDurability * item.getType().getMaxDurability()));
                item.setItemMeta(itemMeta);
            } else {
                item.setAmount(0);
            }

        }
        
    }

}
