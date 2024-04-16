package io.th0rgal.oraxen.mechanics.provided.misc.soulbound;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class SoulBoundMechanicFactory extends MechanicFactory {
    public SoulBoundMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new SoulBoundMechanicListener(this));
    }

    @Override
    public SoulBoundMechanic parse(ConfigurationSection section) {
        SoulBoundMechanic mechanic = new SoulBoundMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public SoulBoundMechanic getMechanic(String itemID) {
        return (SoulBoundMechanic) super.getMechanic(itemID);
    }

    @Override
    public SoulBoundMechanic getMechanic(ItemStack itemStack) {
        return (SoulBoundMechanic) super.getMechanic(itemStack);
    }
}
