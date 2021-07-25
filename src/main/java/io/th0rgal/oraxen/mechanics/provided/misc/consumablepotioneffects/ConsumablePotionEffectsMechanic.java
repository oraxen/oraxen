package io.th0rgal.oraxen.mechanics.provided.misc.consumablepotioneffects;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ConsumablePotionEffectsMechanic extends Mechanic {

    private final Set<PotionEffect> effects = new HashSet<>();

    public ConsumablePotionEffectsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        for (String effectSection : section.getKeys(false))
            if (section.isConfigurationSection(effectSection))
                registersEffectFromSection(section.getConfigurationSection(effectSection));
    }

    public void registersEffectFromSection(ConfigurationSection section) {
        PotionEffectType effectType = PotionEffectType.getByName(section.getName());
        int amplifier = 0;
        int duration = 20 * 30;
        boolean ambient = false;
        boolean particles = true;
        boolean icon = true;
        if (section.isInt("amplifier"))
            amplifier = section.getInt("amplifier");
        if (section.isBoolean("ambient"))
            ambient = section.getBoolean("ambient");
        if (section.isBoolean("particles"))
            particles = section.getBoolean("particles");
        if (section.isBoolean("icon"))
            icon = section.getBoolean("icon");
        if (section.isInt("duration"))
            duration = section.getInt("duration");
        PotionEffect potionEffect = new PotionEffect(effectType, duration, amplifier, ambient, particles,
                icon);
        effects.add(potionEffect);
    }

    public void onItemPlaced(Player player) {
        player.addPotionEffects(effects);
    }

}
