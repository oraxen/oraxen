package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FoodMechanic extends Mechanic {

    private final Set<PotionEffect> effects = new HashSet<>();
    public final int hunger;
    public final int saturation;
    public final boolean hasReplacement;
    public boolean isVanillaItem;

    public String item;

    public final boolean hasEffect;

    public FoodMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        hunger = section.getInt("hunger");
        saturation = section.getInt("saturation");
        if (section.isConfigurationSection("replacement")) {
            ConfigurationSection replacementSection = section.getConfigurationSection("replacement");
            hasReplacement = true;
            isVanillaItem = replacementSection.getBoolean("isVanillaItem");
            item = replacementSection.getString("item");
        }
        else {
            hasReplacement = false;
        }
        if (section.isConfigurationSection("effects")) {
            ConfigurationSection effectSection = section.getConfigurationSection("replacement");
            hasEffect = true;
            for (String sect : effectSection.getKeys(false)) {
                registerEffect(effectSection);
            }
        } else hasEffect = false;
    }

    public void registerEffect(ConfigurationSection section) {
        PotionEffectType effectType = PotionEffectType.getByName(section.getName());
        int duration = 0;
        int amplifier = 0;
        if (section.isInt("duration"))
            duration = section.getInt("duration");
        if (section.isInt("amplifier"))
            amplifier = section.getInt("amplifier");
        PotionEffect potionEffect = new PotionEffect(effectType, duration, amplifier);
        effects.add(potionEffect);
    }

    public int getHunger() {
        return hunger;
    }

    public int getSaturation() {
        return saturation;
    }

    public boolean hasReplacement() {
        return hasReplacement;
    }

    public boolean isVanillaItem() {
        return isVanillaItem;
    }

    public String getReplacementItem() {
        return item;
    }

    public boolean isHasEffect() {
        return hasEffect;
    }

    public Set<PotionEffect> getEffects() {
        return effects;
    }
}
