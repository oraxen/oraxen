package io.th0rgal.oraxen.mechanics.provided.farming.harvesting;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class HarvestingMechanicFactory extends MechanicFactory {

    private static HarvestingMechanicFactory instance;

    public HarvestingMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new HarvestingMechanicListener());
    }

    public static HarvestingMechanicFactory get() {
        return instance;
    }

    @Override
    public HarvestingMechanic parse(ConfigurationSection section) {
        HarvestingMechanic mechanic = new HarvestingMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public HarvestingMechanic getMechanic(String itemID) {
        return (HarvestingMechanic) super.getMechanic(itemID);
    }

    @Override
    public HarvestingMechanic getMechanic(ItemStack itemStack) {
        return (HarvestingMechanic) super.getMechanic(itemStack);
    }
}
