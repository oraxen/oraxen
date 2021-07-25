package io.th0rgal.oraxen.mechanics.provided.misc.custom;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class CustomMechanicFactory extends MechanicFactory {

    public CustomMechanicFactory(ConfigurationSection section) {
        super(section);
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new CustomMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
