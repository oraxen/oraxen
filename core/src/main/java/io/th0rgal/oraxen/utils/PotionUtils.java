package io.th0rgal.oraxen.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;

public class PotionUtils {

    @SuppressWarnings({"deprecation"})
    @Nullable
    public static PotionEffectType getEffectType(String effect) {
        if (effect == null || effect.isEmpty()) return null;
        PotionEffectType effectType = null;
        try {
            effectType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.fromString(effect));
        } catch (NoSuchFieldError ignored) {
        }
        if (effectType == null)
            effectType = PotionEffectType.getByName(effect);
        if (effectType == null)
            effectType = PotionEffectType.getByKey(effect.contains(":") ? NamespacedKey.fromString(effect) : NamespacedKey.minecraft(effect));

        return effectType;
    }
}
