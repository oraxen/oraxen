package io.th0rgal.oraxen.mechanics.provided.misc.backpack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class BackpackMechanicFactory extends MechanicFactory {

        public BackpackMechanicFactory(ConfigurationSection section) {
            super(section);
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new BackpackListener(this));
        }

        @Override
        public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
            Mechanic mechanic = new BackpackMechanic(this, itemMechanicConfiguration);
            addToImplemented(mechanic);
            return mechanic;
        }

}
