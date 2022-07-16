package io.th0rgal.oraxen.mechanics.provided.misc.replaceblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.misc.soulbound.SoulBoundMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.soulbound.SoulBoundMechanicListener;
import org.bukkit.configuration.ConfigurationSection;

public class ReplaceBlockMechanicFactory extends MechanicFactory {
    public ReplaceBlockMechanicFactory(ConfigurationSection section) {
        super(section);
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new SoulBoundMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
