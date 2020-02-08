package io.th0rgal.oraxen.mechanics.provided.potioneffects;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;
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
        return null;
    }

    public Set<ItemPotionEffect> getPotionEffects() {
        return effects;
    }
}