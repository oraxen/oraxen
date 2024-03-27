package io.th0rgal.oraxen.mechanics.provided.misc.consumable;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class ConsumableMechanicFactory extends MechanicFactory {

    private static ConsumableMechanicFactory instance;

    public ConsumableMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new ConsumableMechanicListener(this));
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        Mechanic mechanic = new ConsumableMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static ConsumableMechanicFactory get() {
        return instance;
    }
}
