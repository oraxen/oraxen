package io.th0rgal.oraxen.mechanics.provided.potioneffects;

import io.th0rgal.oraxen.Oraxen;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class PotionEffectsMechanicFactory extends MechanicFactory {

    public PotionEffectsMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(Oraxen.get(), new PotionEffectsMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new PotionEffectsMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
