package io.th0rgal.oraxen.mechanics.provided.invisibleitemframe;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class InvisibleItemFrameFactory extends MechanicFactory {

    public static InvisibleItemFrameFactory instance;

    public InvisibleItemFrameFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), new InvisibleItemFrameListener(this));
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new InvisibleItemFrameMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static InvisibleItemFrameFactory getInstance() {
        return instance;
    }
}
