package io.th0rgal.oraxen.mechanics.provided.misc.consumablepotioneffects;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.PotionUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ConsumablePotionEffectsMechanic extends Mechanic {

    private final Set<PotionEffect> effects = new HashSet<>();

    public ConsumablePotionEffectsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        for (String effectKey : section.getKeys(false)) {
            ConfigurationSection effectSection = section.getConfigurationSection(effectKey);
            if (effectSection != null) registersEffectFromSection(effectSection);
        }
    }

    public void registersEffectFromSection(ConfigurationSection section) {
        String type = section.getName().toLowerCase(Locale.ROOT);
        PotionEffectType effectType = PotionUtils.getEffectType(type);
        if (effectType == null) {
            Logs.logError("Invalid potion effect: " + section.getName() + ", in " + StringUtils.substringBefore(section.getCurrentPath(), ".") + "!");
            return;
        }
        int amplifier = section.getInt("amplifier", 0);
        int duration = section.getInt("duration", 20 * 30);
        boolean ambient = section.getBoolean("ambient", false);
        boolean particles = section.getBoolean("particles", true);
        boolean icon = section.getBoolean("icon", true);
        PotionEffect potionEffect = new PotionEffect(effectType, duration, amplifier, ambient, particles, icon);
        effects.add(potionEffect);
    }

    public void onItemPlaced(Player player) {
        player.addPotionEffects(effects);
    }

}
