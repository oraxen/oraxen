package io.th0rgal.oraxen.mechanics.provided.combat.lifeleech;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class LifeLeechMechanic extends Mechanic {

    private final int amount;

    public LifeLeechMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.amount = section.getInt("amount");
    }

    public int getAmount() {
        return amount;
    }

}
