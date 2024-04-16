package io.th0rgal.oraxen.mechanics.provided.misc.consumablepotioneffects;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class ConsumablePotionEffectsFactory extends MechanicFactory {

    public ConsumablePotionEffectsFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new ConsumablePotionEffectsListener(this));
    }

    @Override
    public ConsumablePotionEffectsMechanic parse(ConfigurationSection section) {
        ConsumablePotionEffectsMechanic mechanic = new ConsumablePotionEffectsMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public ConsumablePotionEffectsMechanic getMechanic(String itemID) {
        return (ConsumablePotionEffectsMechanic) super.getMechanic(itemID);
    }

    @Override
    public ConsumablePotionEffectsMechanic getMechanic(ItemStack itemStack) {
        return (ConsumablePotionEffectsMechanic) super.getMechanic(itemStack);
    }
}
