package io.th0rgal.oraxen.mechanics.provided.gameplay.durability;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.atomic.AtomicBoolean;

public class DurabilityMechanic extends Mechanic {

    private final int itemDurability;
    public static final NamespacedKey DURABILITY_KEY = new NamespacedKey(OraxenPlugin.get(), "durability");

    public DurabilityMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /*
         * We give: - an instance of the Factory which created the mechanic - the
         * section used to configure the mechanic - the item modifier(s)
         */
        super(mechanicFactory, section,
            item -> item.setCustomTag(DURABILITY_KEY, PersistentDataType.INTEGER, section.getInt("value")));
        this.itemDurability = section.getInt("value");
    }

    public int getItemMaxDurability() {
        return itemDurability;
    }

    public void changeDurability(ItemStack item, int amount) {
        DurabilityMechanic durabilityMechanic = this;
        AtomicBoolean check = new AtomicBoolean(false);

        Utils.editItemMeta(item, (itemMeta) -> {
            PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
            check.set(pdc.has(DurabilityMechanic.DURABILITY_KEY, PersistentDataType.INTEGER));

            if (check.get()) {
                if(!(itemMeta instanceof Damageable damageable)) {
                    check.set(true);
                    return;
                }

                int baseMaxDurab = item.getType().getMaxDurability();
                int realMaxDurab = durabilityMechanic.getItemMaxDurability(); // because int rounded values suck
                int realDurabRemain = pdc.getOrDefault(DURABILITY_KEY, PersistentDataType.INTEGER, realMaxDurab);

                // If item was max durab before damage, set the fake one
                if (damageable.getDamage() != 0 && realDurabRemain == realMaxDurab) {
                    pdc.set(DURABILITY_KEY, PersistentDataType.INTEGER,
                            (int) (realMaxDurab - (((double) damageable.getDamage() / (double) baseMaxDurab) * realMaxDurab)));
                    item.setItemMeta(itemMeta);
                    // Call function again to have itemmeta stick and run below part aswell
                    changeDurability(item, amount);
                } else {
                    realDurabRemain = realDurabRemain + amount;
                    if (realDurabRemain > 0) {
                        pdc.set(DURABILITY_KEY, PersistentDataType.INTEGER, realDurabRemain);
                        //TODO Figure out why when mending this sets the durability abit too high in the actual Damagable meta
                        (damageable).setDamage((int) ((double) baseMaxDurab - (((double) realDurabRemain / realMaxDurab) * (double) baseMaxDurab)));
                    } else item.setAmount(0);
                }
            }
        });
        check.get();
    }
}
