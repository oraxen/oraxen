package io.th0rgal.oraxen.mechanics.provided.combat.lifeleech;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class LifeLeechMechanicFactory extends MechanicFactory {

    public LifeLeechMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new LifeLeechMechanicListener(this));
    }

    @Override
    public LifeLeechMechanic parse(ConfigurationSection section) {
        LifeLeechMechanic mechanic = new LifeLeechMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public LifeLeechMechanic getMechanic(String itemID) {
        return (LifeLeechMechanic) super.getMechanic(itemID);
    }

    @Override
    public LifeLeechMechanic getMechanic(ItemStack itemStack) {
        return (LifeLeechMechanic) super.getMechanic(itemStack);
    }

}
