package io.th0rgal.oraxen.mechanics.provided.misc.consumable;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class ConsumableMechanic extends Mechanic {

    public ConsumableMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
    }
}
