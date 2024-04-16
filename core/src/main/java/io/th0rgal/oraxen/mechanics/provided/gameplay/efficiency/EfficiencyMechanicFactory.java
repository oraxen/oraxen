package io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class EfficiencyMechanicFactory extends MechanicFactory {

    private static EfficiencyMechanicFactory instance;

    public EfficiencyMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new EfficiencyMechanicListener());
    }

    @Override
    public EfficiencyMechanic parse(ConfigurationSection section) {
        EfficiencyMechanic mechanic = new EfficiencyMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public EfficiencyMechanic getMechanic(String itemID) {
        return (EfficiencyMechanic) super.getMechanic(itemID);
    }

    @Override
    public EfficiencyMechanic getMechanic(ItemStack itemStack) {
        return (EfficiencyMechanic) super.getMechanic(itemStack);
    }

    public static EfficiencyMechanicFactory get() {
        return instance;
    }

}
