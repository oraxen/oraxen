package io.th0rgal.oraxen.mechanics.provided.misc.misc;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class MiscMechanicFactory extends MechanicFactory {

    private static MiscMechanicFactory instance;
    public MiscMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new MiscListener(this));
        instance = this;
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        Mechanic mechanic = new MiscMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static MiscMechanicFactory get() {
        return instance;
    }

    @Override
    public MiscMechanic getMechanic(String itemID) {
        return (MiscMechanic) super.getMechanic(itemID);
    }

    @Override
    public MiscMechanic getMechanic(ItemStack itemStack) {
        return (MiscMechanic) super.getMechanic(itemStack);
    }
}
