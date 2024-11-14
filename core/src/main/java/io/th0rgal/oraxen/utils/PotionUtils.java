package io.th0rgal.oraxen.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import io.th0rgal.oraxen.utils.logs.Logs;

import javax.annotation.Nullable;

public class PotionUtils {

    public static PotionEffectType getEffectType(String effect) {
        return getEffectType(effect, null);
    }

    @SuppressWarnings({ "deprecation" })
    @Nullable
    public static PotionEffectType getEffectType(String effect, String legacyEffect) {
        if (effect == null || effect.isEmpty())
            return null;
        PotionEffectType effectType = null;
        try {
            effectType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.fromString(effect.toLowerCase()));
        } catch (NoSuchFieldError ignored) {
        }
        if (effectType == null) {
            effectType = PotionEffectType.getByName(effect);
        }
        if (effectType == null) {
            effectType = PotionEffectType.getByKey(
                    effect.contains(":") ? NamespacedKey.fromString(effect) : NamespacedKey.minecraft(effect));
        }

        if (effectType == null && legacyEffect != null && !legacyEffect.isEmpty()) {
            effectType = getEffectType(legacyEffect, null);
        }

        return effectType;
    }

    public static PotionType getPotionType(PotionMeta potionMeta) {
        if (VersionUtil.atOrAbove("1.20.2"))
            return potionMeta.getBasePotionType();
        else
            return potionMeta.getBasePotionData().getType();
    }

    public static void setPotionType(PotionMeta potionMeta, PotionType potionType) {
        if (VersionUtil.atOrAbove("1.20.2"))
            potionMeta.setBasePotionType(potionType);
        else
            potionMeta.setBasePotionData(new PotionData(potionType));
    }
}
