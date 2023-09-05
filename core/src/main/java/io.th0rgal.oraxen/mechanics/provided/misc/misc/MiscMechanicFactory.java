package io.th0rgal.oraxen.mechanics.provided.misc.misc;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class MiscMechanicFactory extends MechanicFactory {
    public MiscMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new MiscListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        Mechanic mechanic = new MiscMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }
}
