package io.th0rgal.oraxen.mechanics.provided.gameplay.durability;

import io.th0rgal.oraxen.api.OraxenItems;
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
        changeDurability(event.getItem(), -event.getDamage());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemMend(PlayerItemMendEvent event) {
        changeDurability(event.getItem(), event.getRepairAmount());
    }

    public void changeDurability(ItemStack item, int amount) {
        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID)) return;

        DurabilityMechanic durabilityMechanic = (DurabilityMechanic) factory.getMechanic(itemID);
        if (durabilityMechanic == null) return;
        AtomicBoolean check = new AtomicBoolean(false);

        Utils.editItemMeta(item, (itemMeta) -> {
            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            check.set(pdc.has(DurabilityMechanic.DURAB_KEY, PersistentDataType.INTEGER));

            if (check.get()) {
                if(!(itemMeta instanceof Damageable damageable)) {
                    check.set(true);
                    return;
                }

                int baseMaxDurab = item.getType().getMaxDurability();
                int realMaxDurab = durabilityMechanic.getItemMaxDurability(); // because int rounded values suck
                int realDurabRemain = pdc.getOrDefault(DurabilityMechanic.DURAB_KEY, PersistentDataType.INTEGER, realMaxDurab);

                // If item was max durab before damage, set the fake one
                if (damageable.getDamage() != 0 && realDurabRemain == realMaxDurab) {
                    pdc.set(DurabilityMechanic.DURAB_KEY, PersistentDataType.INTEGER,
                            (int) (realMaxDurab - (((double) damageable.getDamage() / (double) baseMaxDurab) * realMaxDurab)));
                    item.setItemMeta(itemMeta);
                    // Call function again to have itemmeta stick and run below part aswell
                    changeDurability(item, amount);
                } else {
                    realDurabRemain = realDurabRemain + amount;
                    if (realDurabRemain > 0) {
                        pdc.set(DurabilityMechanic.DURAB_KEY, PersistentDataType.INTEGER, realDurabRemain);
                        //TODO Figure out why when mending this sets the durability abit too high in the actual Damagable meta
                        (damageable).setDamage((int) ((double) baseMaxDurab - (((double) realDurabRemain / realMaxDurab) * (double) baseMaxDurab)));
                    } else item.setAmount(0);
                }
            }
        });
        check.get();
    }
}
