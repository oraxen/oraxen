package io.th0rgal.oraxen.mechanics.provided.misc.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class CommandsMechanicFactory extends MechanicFactory {

    public CommandsMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new CommandsMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        Mechanic mechanic = new CommandsMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public CommandsMechanic getMechanic(String itemID) {
        return (CommandsMechanic) super.getMechanic(itemID);
    }

    @Override
    public CommandsMechanic getMechanic(ItemStack itemStack) {
        return (CommandsMechanic) super.getMechanic(itemStack);
    }
}
