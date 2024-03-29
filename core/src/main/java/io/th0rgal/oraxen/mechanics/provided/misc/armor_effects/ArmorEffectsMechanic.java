package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.PotionUtils;
import io.th0rgal.oraxen.utils.customarmor.ShaderArmorTextures;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class ArmorEffectsMechanic extends Mechanic {

    public static final Set<Integer> ARMOR_SLOTS = Set.of(36,37,38,39);
    private final Set<ArmorEffect> armorEffects = new HashSet<>();

    public ArmorEffectsMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        for (String effect : section.getKeys(false)) {
            ConfigurationSection effectSection = section.getConfigurationSection(effect);
            if (effectSection != null) registersEffectFromSection(effectSection);
        }
    }

    public void registersEffectFromSection(ConfigurationSection section) {
        String type = section.getName().toLowerCase();
        PotionEffectType effectType = PotionUtils.getEffectType(type);
        if (effectType == null) {
            Logs.logError("Invalid potion effect: " + section.getName() + ", in " + StringUtils.substringBefore(section.getCurrentPath(), ".") + "!");
            return;
        }

        int duration = section.getInt("duration", ArmorEffectsFactory.getInstance().getDelay());
        int amplifier = section.getInt("amplifier", 0);
        boolean ambient = section.getBoolean("ambient", false);
        boolean particles = section.getBoolean("particles", true);
        boolean icon = section.getBoolean("icon", true);
        boolean requiresFullSet = section.getBoolean("requires_full_set", false);
        PotionEffect potionEffect = new PotionEffect(effectType, duration, amplifier, ambient, particles, icon);
        armorEffects.add(new ArmorEffect(potionEffect, requiresFullSet));
    }

    public Set<ArmorEffect> getArmorEffects() {
        return armorEffects;
    }

    public static void addEffects(Player player) {
        for (int armorSlot : ArmorEffectsMechanic.ARMOR_SLOTS) {
            ItemStack armorPiece = player.getInventory().getItem(armorSlot);
            ArmorEffectsMechanic mechanic = (ArmorEffectsMechanic) ArmorEffectsFactory.getInstance().getMechanic(armorPiece);
            if (mechanic == null) continue;

            Set<PotionEffect> finalArmorEffects = new HashSet<>();
            for (ArmorEffect armorEffect : mechanic.getArmorEffects()) {
                if (armorEffect.requiresFullSet()) {
                    boolean hasFullSet = ArmorEffectsMechanic.ARMOR_SLOTS.stream().filter(s -> s != armorSlot).allMatch(slot -> {
                        ItemStack armor = player.getInventory().getItem(slot);
                        return armor != null && ShaderArmorTextures.isSameArmorType(armorPiece, armor);
                    });

                    if (hasFullSet) finalArmorEffects.add(armorEffect.getEffect());
                } else finalArmorEffects.add(armorEffect.getEffect());
            }

            player.addPotionEffects(finalArmorEffects);
        }
    }
}
