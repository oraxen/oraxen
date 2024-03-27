package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class CustomBlockFactory extends MechanicFactory {

    public CustomBlockFactory(ConfigurationSection section) {
        super(section);

        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new CustomBlockListener());
    }

    @Override
    public CustomBlockMechanic parse(ConfigurationSection section) {
        CustomBlockMechanic mechanic = new CustomBlockMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }
}
