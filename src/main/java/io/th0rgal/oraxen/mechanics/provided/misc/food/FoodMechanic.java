package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class FoodMechanic extends Mechanic {

    private final Set<PotionEffect> effects = new HashSet<>();
    public final int hunger;
    public final int saturation;
    public final boolean hasReplacement;
    public boolean isVanillaItem;

    public String item;

    public FoodMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        hunger = section.getInt("hunger");
        saturation = section.getInt("saturation");

        if (section.isConfigurationSection("replacement")) {
            ConfigurationSection replacementSection = section.getConfigurationSection("replacement");
            assert replacementSection != null;
            hasReplacement = true;
            isVanillaItem = replacementSection.getBoolean("is_vanilla_item");
            item = replacementSection.getString("item");
        } else hasReplacement = false;

        if (section.isConfigurationSection("effects")) {
            ConfigurationSection effectSection = section.getConfigurationSection("effects");
            assert effectSection != null;
            registerEffect(effectSection);
        }
    }

    public void registerEffect(ConfigurationSection section) {
        PotionEffectType effectType = PotionEffectType.getByName(section.getName());
        if (effectType == null)
            throw new IllegalArgumentException("Invalid effect type: " + section.getName());
        PotionEffect potionEffect = new PotionEffect(effectType, section.getInt("duration", 1), section.getInt("amplifier", 0));
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

    public String getReplacement() {
        return item;
    }

    public boolean hasEffects() { return !effects.isEmpty(); }

    public Set<PotionEffect> getEffects() {
        return effects;
    }
}
