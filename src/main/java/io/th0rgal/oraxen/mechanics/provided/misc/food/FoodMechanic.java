package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.lumine.mythiccrucible.MythicCrucible;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class FoodMechanic extends Mechanic {

    private PotionEffect effect = null;
    private double effectProbability = 1.0;
    private final int hunger;
    private final int saturation;
    private ItemStack replacementItem;

    public FoodMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        hunger = section.getInt("hunger");
        saturation = section.getInt("saturation");

        if (section.isConfigurationSection("replacement")) {
            registerReplacement(section.getConfigurationSection("replacement"));
        } else replacementItem = null;

        if (section.isConfigurationSection("effect")) {
            ConfigurationSection effectSection = section.getConfigurationSection("effect");
            assert effectSection != null;
            registerEffect(effectSection);
        } else effect = null;
    }

    private void registerEffect(ConfigurationSection section) {
        PotionEffectType effectType = PotionEffectType.getByName(section.getString("type", null));
        effectProbability = Math.min(section.getDouble("probability", 1.0), 1.0);
        if (effectType == null) Logs.logError("Invalid effect type: " + section.getName());
        else effect = new PotionEffect(effectType, section.getInt("duration", 1) * 20,
                    section.getInt("amplifier", 0), section.getBoolean("is_ambient", true),
                    section.getBoolean("has_particles", true), section.getBoolean("has_icon", true));
    }

    private void registerReplacement(ConfigurationSection section) {
        if (section.isString("minecraft_type")) {
            Material material = Material.getMaterial(Objects.requireNonNull(section.getString("minecraft_type")));
            if (material == null) {
                Logs.logError("Invalid material: " + section.getString("minecraft_type"));
                replacementItem = null;
            }
            else replacementItem = new ItemStack(material);
        } else if (section.isString("oraxen_item"))
            replacementItem = OraxenItems.getItemById(section.getString("oraxen_item")).build();
        else if (section.isString("crucible_item"))
            replacementItem = MythicCrucible.core().getItemManager().getItemStack(section.getString("crucible_item"));
        else replacementItem = null;
    }

    public int getHunger() {
        return hunger;
    }

    public int getSaturation() {
        return saturation;
    }

    public boolean hasReplacement() {
        return replacementItem != null;
    }

    public ItemStack getReplacement() {
        return replacementItem;
    }

    public boolean hasEffect() {
        return effect != null;
    }

    public PotionEffect getEffect() {
        return effect;
    }

    public double getEffectProbability() {
        return effectProbability;
    }
}
