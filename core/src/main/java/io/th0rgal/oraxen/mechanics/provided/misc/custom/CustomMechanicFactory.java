package io.th0rgal.oraxen.mechanics.provided.misc.custom;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class CustomMechanicFactory extends MechanicFactory {

    public CustomMechanicFactory(ConfigurationSection section) {
        super(section);
    }

    @Override
    public CustomMechanic parse(ConfigurationSection section) {
        CustomMechanic mechanic = new CustomMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public CustomMechanic getMechanic(String itemID) {
        return (CustomMechanic) super.getMechanic(itemID);
    }

    @Override
    public CustomMechanic getMechanic(ItemStack itemStack) {
        return (CustomMechanic) super.getMechanic(itemStack);
    }
}
