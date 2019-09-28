package io.th0rgal.oraxen.items.mechanics.provided.durability;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.Item;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.items.mechanics.Mechanic;
import io.th0rgal.oraxen.items.mechanics.MechanicFactory;
import io.th0rgal.oraxen.items.modifiers.ItemModifier;
import io.th0rgal.oraxen.listeners.EventsManager;
import io.th0rgal.oraxen.utils.Logs;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class DurabilityMechanicFactory extends MechanicFactory implements Listener {

    public DurabilityMechanicFactory(ConfigurationSection section) {
        super(section);
        new EventsManager(OraxenPlugin.get()).addEvents(this);
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new DurabilityMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamaged(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        if (!this.isImplementedIn(itemID))
            return;

        DurabilityMechanic durabilityMechanic = (DurabilityMechanic) getMechanic(itemID);

        ItemMeta itemMeta = item.getItemMeta();
        PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
        int realDurabilityLeft = persistentDataContainer.get(DurabilityModifier.NAMESPACED_KEY, PersistentDataType.INTEGER) - event.getDamage();

        if (realDurabilityLeft > 0) {
            double realMaxDurability = durabilityMechanic.getItemMaxDurability(); //because int rounded values suck
            Logs.logWarning("realdura:" + realDurabilityLeft + " realmax:" + realMaxDurability);
            persistentDataContainer.set(DurabilityModifier.NAMESPACED_KEY, PersistentDataType.INTEGER, realDurabilityLeft);
            ((Damageable) itemMeta).setDamage((int) (item.getType().getMaxDurability() - realDurabilityLeft / realMaxDurability * item.getType().getMaxDurability()));
            item.setItemMeta(itemMeta);
        } else {
            item.setAmount(0);
        }

    }

}

class DurabilityMechanic extends Mechanic {

    private int itemDurability;

    public DurabilityMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /* We give:
        - an instance of the Factory which created the mechanic
        - the section used to configure the mechanic
        - the item modifier(s)
         */
        super(mechanicFactory, section, new DurabilityModifier(section.getInt("value")));
        this.itemDurability = section.getInt("value");
    }

    public int getItemMaxDurability() {
        return itemDurability;
    }
}

class DurabilityModifier extends ItemModifier {

    public static final NamespacedKey NAMESPACED_KEY = new NamespacedKey(OraxenPlugin.get(), "durability");
    private int durability;

    public DurabilityModifier(int durability) {
        this.durability = durability;
    }

    @Override
    public Item getItem(Item item) {
        return item.setCustomTag(NAMESPACED_KEY, PersistentDataType.INTEGER, durability);
    }
}