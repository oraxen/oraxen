package io.th0rgal.oraxen.mechanics.provided.combat.spell.energyblast;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class EnergyBlastMechanicFactory extends MechanicFactory {
    public EnergyBlastMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new EnergyBlastMechanicManager(this));
    }

    @Override
    public EnergyBlastMechanic parse(ConfigurationSection section) {
        EnergyBlastMechanic mechanic = new EnergyBlastMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public EnergyBlastMechanic getMechanic(String itemID) {
        return (EnergyBlastMechanic) super.getMechanic(itemID);
    }

    @Override
    public EnergyBlastMechanic getMechanic(ItemStack itemStack) {
        return (EnergyBlastMechanic) super.getMechanic(itemStack);
    }
}
