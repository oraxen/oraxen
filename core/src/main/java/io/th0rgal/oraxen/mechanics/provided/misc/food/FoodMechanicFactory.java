package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class FoodMechanicFactory extends MechanicFactory {
    public FoodMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FoodMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new FoodMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
