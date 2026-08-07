package io.th0rgal.oraxen.mechanics.provided.combat.bleeding;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class BleedingMechanic extends Mechanic {

    private final double chance;
    private final int duration;
    private final double damagePerTick;
    private final int tickInterval;

    public BleedingMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        this.chance = section.getDouble("chance", 0.3);
        this.duration = section.getInt("duration", 100);
        this.damagePerTick = section.getDouble("damage_per_interval", 0.5);
        this.tickInterval = section.getInt("interval", 20);
    }

    public double getChance() {
        return chance;
    }

    public int getDuration() {
        return duration;
    }

    public double getDamagePerTick() {
        return damagePerTick;
    }

    public int getTickInterval() {
        return tickInterval;
    }

}