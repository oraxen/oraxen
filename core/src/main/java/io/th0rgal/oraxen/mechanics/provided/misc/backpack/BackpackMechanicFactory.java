package io.th0rgal.oraxen.mechanics.provided.misc.backpack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class BackpackMechanicFactory extends MechanicFactory {

        public BackpackMechanicFactory(ConfigurationSection section) {
            super(section);
            MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new BackpackListener(this));
        }

        @Override
        public BackpackMechanic parse(ConfigurationSection section) {
            BackpackMechanic mechanic = new BackpackMechanic(this, section);
            addToImplemented(mechanic);
            return mechanic;
        }

    @Override
    public BackpackMechanic getMechanic(String itemID) {
        return (BackpackMechanic) super.getMechanic(itemID);
    }

    @Override
    public BackpackMechanic getMechanic(ItemStack itemStack) {
        return (BackpackMechanic) super.getMechanic(itemStack);
    }

}
