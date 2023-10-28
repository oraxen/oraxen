package io.th0rgal.oraxen.mechanics.provided.misc.consumablepotioneffects;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class ConsumablePotionEffectsFactory extends MechanicFactory {

    public ConsumablePotionEffectsFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new ConsumablePotionEffectsListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new ConsumablePotionEffectsMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
