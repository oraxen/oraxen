package io.th0rgal.oraxen.mechanics.provided.spell.fireball;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.spell.energyblast.EnergyBlastMechanic;
import io.th0rgal.oraxen.mechanics.provided.spell.energyblast.EnergyBlastMechanicManager;
import org.bukkit.configuration.ConfigurationSection;

public class FireballMechanicFactory extends MechanicFactory {
    public FireballMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), new FireballMechanicManager(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new FireballMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
