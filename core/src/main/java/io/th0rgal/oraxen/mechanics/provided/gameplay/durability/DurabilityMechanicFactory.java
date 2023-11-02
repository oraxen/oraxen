package io.th0rgal.oraxen.mechanics.provided.gameplay.durability;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class DurabilityMechanicFactory extends MechanicFactory {

    private static DurabilityMechanicFactory instance;

    public DurabilityMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new DurabilityMechanicManager(this));
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new DurabilityMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static DurabilityMechanicFactory get() {
        return instance;
    }

    @Override
    public DurabilityMechanic getMechanic(String itemId) {
        return (DurabilityMechanic) super.getMechanic(itemId);
    }

    @Override
    public DurabilityMechanic getMechanic(ItemStack itemStack) {
        return (DurabilityMechanic) super.getMechanic(itemStack);
    }
}
