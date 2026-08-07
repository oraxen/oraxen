package io.th0rgal.oraxen.mechanics.provided.combat.spell.energyblast;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.combat.spell.SpellMechanic;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public class EnergyBlastMechanic extends SpellMechanic {

    private final Particle particle;
    private Particle.DustOptions particleColor = null;
    private final double damage;
    private final int length;

    public EnergyBlastMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        ConfigurationSection particleSection = section.getConfigurationSection("particle");
        assert particleSection != null;

        this.particle = Particle.valueOf(particleSection.getString("type"));
        if (particleSection.isConfigurationSection("color")) {
            ConfigurationSection colorSection = particleSection.getConfigurationSection("color");
            this.particleColor = new Particle.DustOptions(Color
                    .fromRGB(Objects.requireNonNull(colorSection).getInt("red"), colorSection.getInt("green"),
                            colorSection.getInt("blue")),
                    particleSection.getInt("size"));
        }
        this.damage = section.getDouble("damage");
        this.length = section.getInt("length");
    }

    public Particle.DustOptions getParticleColor() {
        return particleColor;
    }

    public double getDamage() {
        return damage;
    }

    public int getLength() {
        return length;
    }

    public Particle getParticle() {
        return particle;
    }
}
