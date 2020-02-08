package io.th0rgal.oraxen.mechanics.provided.potioneffects;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

public class PotionEffectsMechanic extends Mechanic {

    private Set<ItemPotionEffect> effects;

    public PotionEffectsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        for (String effectSection : section.getKeys(false))
            if (section.isConfigurationSection(effectSection))
                effects.add(parseEffectSection(section.getConfigurationSection(effectSection)));

    }

    public ItemPotionEffect parseEffectSection(ConfigurationSection section) {
        PotionEffectType effectType = PotionEffectType.getByName(section.getName());
        int amplifier = 1;
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
        PotionEffect potionEffect = new PotionEffect(effectType, Integer.MAX_VALUE, amplifier, ambient, particles, icon);
        ItemPotionEffect.Position position = ItemPotionEffect.Position.valueOf(section.getString("position").toUpperCase());
        return new ItemPotionEffect(potionEffect, position);
    }

    public Set<ItemPotionEffect> getPotionEffects() {
        return effects;
    }
}