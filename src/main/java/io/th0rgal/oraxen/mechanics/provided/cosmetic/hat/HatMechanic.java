package io.th0rgal.oraxen.mechanics.provided.cosmetic.hat;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

@Deprecated(since = "1.21.2")
public class HatMechanic extends Mechanic {

    public HatMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
    }

}
