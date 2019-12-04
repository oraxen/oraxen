package io.th0rgal.oraxen.mechanics.provided.bottledexp;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class BottledExpMechanic extends Mechanic {

    final double ratio;

    public BottledExpMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.ratio = section.getDouble("ratio");
    }

    public double getRatio() {
        return  ratio;
    }

}