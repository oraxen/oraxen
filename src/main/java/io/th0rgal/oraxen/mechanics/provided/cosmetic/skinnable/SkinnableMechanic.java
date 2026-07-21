package io.th0rgal.oraxen.mechanics.provided.cosmetic.skinnable;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class SkinnableMechanic extends Mechanic {
    public SkinnableMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
    }
}
