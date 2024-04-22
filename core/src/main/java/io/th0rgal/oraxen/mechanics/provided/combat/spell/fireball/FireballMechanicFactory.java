package io.th0rgal.oraxen.mechanics.provided.combat.spell.fireball;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class FireballMechanicFactory extends MechanicFactory {
    public FireballMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new FireballMechanicManager(this));
    }

    @Override
    public FireballMechanic parse(ConfigurationSection section) {
        FireballMechanic mechanic = new FireballMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public FireballMechanic getMechanic(String itemID) {
        return (FireballMechanic) super.getMechanic(itemID);
    }

    @Override
    public FireballMechanic getMechanic(ItemStack itemStack) {
        return (FireballMechanic) super.getMechanic(itemStack);
    }
}
