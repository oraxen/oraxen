package io.th0rgal.oraxen.mechanics.provided.spell.fireball;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.spell.SpellMechanic;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public class FireballMechanic extends SpellMechanic {

    private final double yield;
    private final double speed;

    public FireballMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        this.yield = section.getDouble("yield");
        this.speed = section.getDouble("speed");
    }

    public double getYield() {
        return yield;
    }

    public double getSpeed() {
        return speed;
    }
}
