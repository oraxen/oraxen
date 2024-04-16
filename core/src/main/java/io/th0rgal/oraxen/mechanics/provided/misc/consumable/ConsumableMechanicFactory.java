package io.th0rgal.oraxen.mechanics.provided.misc.consumable;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class ConsumableMechanicFactory extends MechanicFactory {

    private static ConsumableMechanicFactory instance;

    public ConsumableMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new ConsumableMechanicListener(this));
        instance = this;
    }

    @Override
    public ConsumableMechanic parse(ConfigurationSection section) {
        ConsumableMechanic mechanic = new ConsumableMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public ConsumableMechanic getMechanic(String itemID) {
        return (ConsumableMechanic) super.getMechanic(itemID);
    }

    @Override
    public ConsumableMechanic getMechanic(ItemStack itemStack) {
        return (ConsumableMechanic) super.getMechanic(itemStack);
    }

    public static ConsumableMechanicFactory get() {
        return instance;
    }
}
