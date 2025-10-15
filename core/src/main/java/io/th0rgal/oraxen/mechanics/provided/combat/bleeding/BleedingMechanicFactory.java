package io.th0rgal.oraxen.mechanics.provided.combat.bleeding;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class BleedingMechanicFactory extends MechanicFactory {

    public BleedingMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new BleedingMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BleedingMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

}