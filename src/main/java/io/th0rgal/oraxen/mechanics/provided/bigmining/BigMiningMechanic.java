package io.th0rgal.oraxen.mechanics.provided.bigmining;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class BigMiningMechanic extends Mechanic {

    private int radius;

    public BigMiningMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        radius = section.getInt("radius");
    }

    public int getRadius() {
        return this.radius;
    }

}