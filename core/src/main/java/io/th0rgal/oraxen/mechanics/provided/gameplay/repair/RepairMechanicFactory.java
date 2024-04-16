package io.th0rgal.oraxen.mechanics.provided.gameplay.repair;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class RepairMechanicFactory extends MechanicFactory {

    private final boolean oraxenDurabilityOnly;

    public RepairMechanicFactory(ConfigurationSection section) {
        super(section);
        oraxenDurabilityOnly = section.getBoolean("oraxen_durability_only");
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new RepairMechanicListener(this));
    }

    @Override
    public RepairMechanic parse(ConfigurationSection section) {
        RepairMechanic mechanic = new RepairMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public RepairMechanic getMechanic(String itemID) {
        return (RepairMechanic) super.getMechanic(itemID);
    }

    @Override
    public RepairMechanic getMechanic(ItemStack itemStack) {
        return (RepairMechanic) super.getMechanic(itemStack);
    }

    public boolean isOraxenDurabilityOnly() {
        return oraxenDurabilityOnly;
    }
}
