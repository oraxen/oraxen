package io.th0rgal.oraxen.mechanics.provided.gameplay.durability;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.atomic.AtomicBoolean;

public class DurabilityMechanicManager implements Listener {

    private final DurabilityMechanicFactory factory;

    public DurabilityMechanicManager(DurabilityMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamaged(PlayerItemDamageEvent event) {
        if (changeDurability(event.getItem(), -event.getDamage())) {
            event.setDamage(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemMend(PlayerItemMendEvent event) {
        if (changeDurability(event.getItem(), event.getRepairAmount())) {
            event.setRepairAmount(0);
        }
    }

    public boolean changeDurability(ItemStack item, int amount) {
        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID)) return false;

        DurabilityMechanic durabilityMechanic = (DurabilityMechanic) factory.getMechanic(itemID);
        if (durabilityMechanic == null) return false;
        AtomicBoolean check = new AtomicBoolean(false);

        Utils.editItemMeta(item, (itemMeta) -> {
            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            check.set(pdc.has(DurabilityMechanic.DURAB_KEY, PersistentDataType.INTEGER));

            if (check.get()) {
                int realDurabRemain = pdc.get(DurabilityMechanic.DURAB_KEY, PersistentDataType.INTEGER) + amount;
                if (realDurabRemain > 0) {
                    pdc.set(DurabilityMechanic.DURAB_KEY, PersistentDataType.INTEGER, realDurabRemain);

                    if(!(itemMeta instanceof Damageable damageable)) {
                        check.set(true);
                        return item;
                    }
                    double realMaxDurab = durabilityMechanic.getItemMaxDurability(); // because int rounded values suck

                    (damageable).setDamage((int) (item.getType().getMaxDurability() - realDurabRemain / realMaxDurab * item.getType().getMaxDurability()));
                } else item.setAmount(0);
            }
            return item;
        });
        return check.get();
    }
}
