package io.th0rgal.oraxen.mechanics.provided.misc.replaceblock;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

public class ReplaceBlockMechanic extends Mechanic {
    public final String baseBlock;
    public final String switchedBlock;
    public final String action;
    public final String effect;
    public final int effectNumber;
    public final String sound;

    public ReplaceBlockMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        baseBlock = section.getString("base_block");
        switchedBlock = section.getString("switched_block");
        action = section.getString("action");
        effect = section.getString("effect");
        effectNumber = section.getInt("effect_number");
        sound = section.getString("sound");
    }

    public String getBaseBlock() {
        return baseBlock;
    }
    public String getSwitchedBlock() {
        return switchedBlock;
    }

    public String getAction() {
        return action;
    }

    public String getEffect() {
        return effect;
    }

    public int getEffectNumber() {
        return effectNumber;
    }

    public String getSound() {
        return sound;
    }
}
