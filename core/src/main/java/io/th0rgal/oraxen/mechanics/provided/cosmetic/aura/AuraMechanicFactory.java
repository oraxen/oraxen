package io.th0rgal.oraxen.mechanics.provided.cosmetic.aura;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class AuraMechanicFactory extends MechanicFactory {

    public AuraMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new AuraMechanicListener(this));
    }

    @Override
    public AuraMechanic parse(ConfigurationSection section) {
        AuraMechanic mechanic = new AuraMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public AuraMechanic getMechanic(String itemID) {
        return (AuraMechanic) super.getMechanic(itemID);
    }

    @Override
    public AuraMechanic getMechanic(ItemStack itemStack) {
        return (AuraMechanic) super.getMechanic(itemStack);
    }

}
