package io.th0rgal.oraxen.mechanics.provided.combat.spell.witherskull;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class WitherSkullMechanicFactory extends MechanicFactory {
    public WitherSkullMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new WitherSkullMechanicListener(this));
    }

    @Override
    public WitherSkullMechanic parse(ConfigurationSection section) {
        WitherSkullMechanic mechanic = new WitherSkullMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public WitherSkullMechanic getMechanic(String itemID) {
        return (WitherSkullMechanic) super.getMechanic(itemID);
    }

    @Override
    public WitherSkullMechanic getMechanic(ItemStack itemStack) {
        return (WitherSkullMechanic) super.getMechanic(itemStack);
    }

}
