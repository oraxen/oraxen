package io.th0rgal.oraxen.mechanics.provided.farming.bottledexp;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class BottledExpMechanicFactory extends MechanicFactory {

    private final int durabilityCost;

    public BottledExpMechanicFactory(ConfigurationSection section) {
        super(section);
        durabilityCost = section.getInt("durability_cost");
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new BottledExpMechanicListener(this));
    }

    @Override
    public BottledExpMechanic parse(ConfigurationSection section) {
        BottledExpMechanic mechanic = new BottledExpMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public BottledExpMechanic getMechanic(String itemID) {
        return (BottledExpMechanic) super.getMechanic(itemID);
    }

    @Override
    public BottledExpMechanic getMechanic(ItemStack itemStack) {
        return (BottledExpMechanic) super.getMechanic(itemStack);
    }

    public int getDurabilityCost() {
        return durabilityCost;
    }

}
