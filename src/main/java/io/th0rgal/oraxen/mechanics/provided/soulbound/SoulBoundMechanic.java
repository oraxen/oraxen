package io.th0rgal.oraxen.mechanics.provided.soulbound;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class SoulBoundMechanic extends Mechanic {
    private double loseChance;

    public SoulBoundMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.loseChance = section.getDouble("lose_chance");
    }

    public double getLoseChance() {
        return loseChance;
    }
}
