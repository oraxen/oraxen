package io.th0rgal.oraxen.mechanics.provided.combat.spell.thor;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class ThorMechanicFactory extends MechanicFactory {

    public ThorMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new ThorMechanicListener(this));
    }

    @Override
    public ThorMechanic parse(ConfigurationSection section) {
        ThorMechanic mechanic = new ThorMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public ThorMechanic getMechanic(String itemID) {
        return (ThorMechanic) super.getMechanic(itemID);
    }

    @Override
    public ThorMechanic getMechanic(ItemStack itemStack) {
        return (ThorMechanic) super.getMechanic(itemStack);
    }

}
