package io.th0rgal.oraxen.mechanics.provided.cosmetic.hat;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class HatMechanicFactory extends MechanicFactory {

    private static HatMechanicFactory instance;

    public HatMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new HatMechanicListener(this));
        instance = this;
    }

    @Override
    public HatMechanic parse(ConfigurationSection section) {
        HatMechanic mechanic = new HatMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public HatMechanic getMechanic(String itemID) {
        return (HatMechanic) super.getMechanic(itemID);
    }

    @Override
    public HatMechanic getMechanic(ItemStack itemStack) {
        return (HatMechanic) super.getMechanic(itemStack);
    }

    public static HatMechanicFactory get() {
        return instance;
    }

}
