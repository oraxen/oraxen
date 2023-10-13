package io.th0rgal.oraxen.mechanics.provided.farming.smelting;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class SmeltingMechanicFactory extends MechanicFactory {

    private static SmeltingMechanicFactory instance;

    public SmeltingMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new SmeltingMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new SmeltingMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static SmeltingMechanicFactory getInstance() {
        return instance;
    }

}
