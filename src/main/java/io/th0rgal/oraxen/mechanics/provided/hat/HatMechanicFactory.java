package io.th0rgal.oraxen.mechanics.provided.hat;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;

import org.bukkit.configuration.ConfigurationSection;

public class HatMechanicFactory extends MechanicFactory {

    public HatMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), new HatMechanicsListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new HatMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

}
