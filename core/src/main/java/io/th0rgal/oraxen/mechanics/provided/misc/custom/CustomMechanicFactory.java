package io.th0rgal.oraxen.mechanics.provided.misc.custom;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class CustomMechanicFactory extends MechanicFactory {

    public CustomMechanicFactory(ConfigurationSection section) {
        super(section);
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        Mechanic mechanic = new CustomMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }
}
