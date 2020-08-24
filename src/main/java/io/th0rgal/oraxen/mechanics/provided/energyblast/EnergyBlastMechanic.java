package io.th0rgal.oraxen.mechanics.provided.energyblast;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Objects;

public class EnergyBlastMechanic extends Mechanic {

    private final Particle particle;
    private final Particle.DustOptions particleColor;
    private final double damage;
    private final int length;
    private final TimersFactory timersFactory;

    public EnergyBlastMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        ConfigurationSection particleSection = section.getConfigurationSection("particle");
        assert particleSection != null;
        ConfigurationSection colorSection = particleSection.getConfigurationSection("color");
        this.particle = Particle.valueOf(particleSection.getString("type"));
        this.particleColor = new Particle.DustOptions(Color
            .fromRGB(Objects.requireNonNull(colorSection).getInt("red"), colorSection.getInt("green"),
                colorSection.getInt("blue")),
            particleSection.getInt("size"));
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

    public Particle getParticle() {
        return particle;
    }
}
