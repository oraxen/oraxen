package io.th0rgal.oraxen.items.mechanics.provided.durability;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.items.mechanics.Mechanic;
import io.th0rgal.oraxen.items.mechanics.MechanicFactory;
import io.th0rgal.oraxen.items.modifiers.ItemModifier;
import io.th0rgal.oraxen.listeners.EventsManager;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.NMS;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;

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
    public void onItemDamaged(PlayerItemDamageEvent event) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        if (!this.isImplementedIn(itemID))
            return;

        DurabilityMechanic durabilityMechanic = (DurabilityMechanic) getMechanic(itemID);

        Object nmsItem = ItemUtils.getNMSCopy(item);
        Object itemTag = ItemUtils.getNBTTagCompound(nmsItem);
        Object nbtBase = ItemUtils.getNBTBase(itemTag, "durability");

        int realDurability = (int) NMS.NBT_TAG_INT.toClass().getMethod("asInt").invoke(nbtBase) - event.getDamage();

        if (realDurability > 0) {
            event.setCancelled(true);
            ItemUtils.setIntNBTTag(itemTag, "durability", realDurability);
            ItemUtils.setNBTTagCompound(nmsItem, itemTag);
            Damageable damageableMeta = (Damageable) ItemUtils.fromNMS(nmsItem).getItemMeta();
            damageableMeta.setDamage((int) (item.getType().getMaxDurability() - ((float) item.getType().getMaxDurability() / (float) durabilityMechanic.getItemDurability()
                    * (float) realDurability)));
            item.setItemMeta((ItemMeta) damageableMeta);
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

    public int getItemDurability() {
        return itemDurability;
    }
}