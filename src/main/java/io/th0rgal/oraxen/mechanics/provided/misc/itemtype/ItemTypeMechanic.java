package io.th0rgal.oraxen.mechanics.provided.misc.itemtype;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class ItemTypeMechanic extends Mechanic {

    public final String itemType;

    public ItemTypeMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.itemType = section.getString("value");
    }

}
