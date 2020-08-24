package io.th0rgal.oraxen.mechanics.provided.skin;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class SkinMechanic extends Mechanic {

    private boolean consume;

    public SkinMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.consume = section.getBoolean("consume");
    }

    public boolean doConsume() {
        return consume;
    }
}
