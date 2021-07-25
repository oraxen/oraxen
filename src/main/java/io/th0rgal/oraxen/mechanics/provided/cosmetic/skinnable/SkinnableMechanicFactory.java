package io.th0rgal.oraxen.mechanics.provided.cosmetic.skinnable;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class SkinnableMechanicFactory extends MechanicFactory {

    private static SkinnableMechanicFactory instance;

    public SkinnableMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
    }

    public static SkinnableMechanicFactory get() {
        return instance;
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new SkinnableMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
