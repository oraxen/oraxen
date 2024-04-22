package io.th0rgal.oraxen.mechanics.provided.farming.smelting;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class SmeltingMechanicFactory extends MechanicFactory {

    private static SmeltingMechanicFactory instance;

    public SmeltingMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new SmeltingMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        Mechanic mechanic = new SmeltingMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public SmeltingMechanic getMechanic(String itemID) {
        return (SmeltingMechanic) super.getMechanic(itemID);
    }

    @Override
    public SmeltingMechanic getMechanic(ItemStack itemStack) {
        return (SmeltingMechanic) super.getMechanic(itemStack);
    }

    public static SmeltingMechanicFactory get() {
        return instance;
    }

}
