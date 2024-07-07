package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.PotionUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.Indyuce.mmoitems.MMOItems;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Deprecated(forRemoval = true, since = "1.20.6")
public class FoodMechanic extends Mechanic {

    private final Set<PotionEffect> effects = new HashSet<>();
    private final double effectProbability;
    private final int hunger;
    private final int saturation;
    private ItemStack replacementItem;

    public FoodMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        hunger = section.getInt("hunger");
        saturation = section.getInt("saturation");
        effectProbability = Math.min(section.getDouble("effect_probability", 1.0), 1.0);

        // This must be initialized in OraxenItemsLoadedEvent due to OraxenItems not being loaded yet
        replacementItem = section.isConfigurationSection("replacement") ? new ItemStack(Material.AIR) : null;

        ConfigurationSection effectsSection = section.getConfigurationSection("effects");
        if (effectsSection != null) for (String effect : effectsSection.getKeys(false)) {
            ConfigurationSection effectSection = effectsSection.getConfigurationSection(effect);
            if (effectSection != null) registerEffects(effectSection);
        }
    }

    private void registerEffects(ConfigurationSection section) {
        String type = section.getName().toLowerCase(Locale.ROOT);
        PotionEffectType effectType = PotionUtils.getEffectType(type);
        if (effectType == null) {
            Logs.logError("Invalid potion effect: " + section.getName() + ", in " + StringUtils.substringBefore(section.getCurrentPath(), ".") + "!");
            return;
        }

        effects.add(new PotionEffect(effectType,
                section.getInt("duration", 1) * 20,
                section.getInt("amplifier", 0),
                section.getBoolean("is_ambient", true),
                section.getBoolean("has_particles", true),
                section.getBoolean("has_icon", true)));
    }

    public void registerReplacement(ConfigurationSection section) {
        if (section.isString("minecraft_type")) {
            Material material = Material.getMaterial(Objects.requireNonNull(section.getString("minecraft_type")));
            if (material == null) {
                Logs.logError("Invalid material: " + section.getString("minecraft_type"));
                replacementItem = null;
            } else replacementItem = new ItemStack(material);
        } else if (section.isString("oraxen_item"))
            replacementItem = OraxenItems.getItemById(section.getString("oraxen_item")).build();
        else if (section.isString("crucible_item"))
            replacementItem = new WrappedCrucibleItem(section.getString("crucible_item")).build();
        else if (section.isString("mmoitems_id") && section.isString("mmoitems_type"))
            replacementItem = MMOItems.plugin.getItem(section.getString("mmoitems_type"), section.getString("mmoitems_id"));
        else if (section.isString("ecoitem_id"))
            replacementItem = new WrappedEcoItem(section.getString("ecoitem_id")).build();
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

    public boolean hasEffects() {
        return !effects.isEmpty();
    }

    public Set<PotionEffect> getEffects() {
        return effects;
    }

    public double getEffectProbability() {
        return effectProbability;
    }
}
