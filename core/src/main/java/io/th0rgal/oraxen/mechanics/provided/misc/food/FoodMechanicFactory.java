package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class FoodMechanicFactory extends MechanicFactory {
    public FoodMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FoodMechanicListener(this));
    }

    @Override
    public FoodMechanic parse(ConfigurationSection section) {
        FoodMechanic mechanic = new FoodMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public FoodMechanic getMechanic(String itemID) {
        return (FoodMechanic) super.getMechanic(itemID);
    }

    @Override
    public FoodMechanic getMechanic(ItemStack itemStack) {
        return (FoodMechanic) super.getMechanic(itemStack);
    }
}
