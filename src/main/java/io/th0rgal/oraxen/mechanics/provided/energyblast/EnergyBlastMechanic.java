package io.th0rgal.oraxen.mechanics.provided.energyblast;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;


public class EnergyBlastMechanic extends Mechanic{

    private Particle.DustOptions particleColor;
    private double damage;
    private int length;
    private TimersFactory timersFactory;

    public EnergyBlastMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        ConfigurationSection particleSection = section.getConfigurationSection("particle");
        ConfigurationSection colorSection = particleSection.getConfigurationSection("color");
        this.particleColor = new Particle.DustOptions(Color.fromRGB(colorSection.getInt("red"), colorSection.getInt("green"), colorSection.getInt("blue")), particleSection.getInt("size"));
        this.damage = section.getDouble("damage");
        this.length = section.getInt("length");
        this.timersFactory = new TimersFactory(section.getLong("delay"));
    }

    public Particle.DustOptions getParticleColor() {
        return particleColor;
    }

    public double getDamage() {
        return damage;
    }

    public Timer getTimer(Player player) {
        return timersFactory.getTimer(player);
    }

    public int getLength() {
        return length;
    }
}
