package io.th0rgal.oraxen.mechanics.provided.bottledexp;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.durability.DurabilityMechanic;
import org.bukkit.configuration.ConfigurationSection;

public class BottledExpMechanicFactory extends MechanicFactory {

    public BottledExpMechanicFactory(ConfigurationSection section) {
        super(section);
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BottledExpMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

}
