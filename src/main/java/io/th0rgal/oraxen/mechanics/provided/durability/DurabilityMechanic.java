package io.th0rgal.oraxen.mechanics.provided.durability;

import io.th0rgal.oraxen.Oraxen;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataType;

public class DurabilityMechanic extends Mechanic {

    private final int itemDurability;
    public static final NamespacedKey NAMESPACED_KEY = new NamespacedKey(Oraxen.get(), "durability");

    public DurabilityMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        /* We give:
        - an instance of the Factory which created the mechanic
        - the section used to configure the mechanic
        - the item modifier(s)
         */
        super(mechanicFactory, section, item ->
                item.setCustomTag(NAMESPACED_KEY,
                        PersistentDataType.INTEGER, section.getInt("value")));
        this.itemDurability = section.getInt("value");
    }

    public int getItemMaxDurability() {
        return itemDurability;
    }
}
