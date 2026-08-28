package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PotionUtils {

    public static PotionEffectType getEffectType(String effect) {
        return getEffectType(effect, null);
    }

    @SuppressWarnings("deprecation")
    @Nullable
    public static PotionEffectType getEffectType(String effect, String legacyEffect) {
        if (effect == null || effect.isBlank())
            return null;

        String normalized = effect.trim().toLowerCase(Locale.ROOT);
        List<String> candidates = new ArrayList<>(List.of(normalized));
        String alternate = renamedEffect(normalized);
        if (alternate != null)
            candidates.add(alternate);

        for (String candidate : candidates) {
            PotionEffectType effectType = resolveEffectType(candidate);
            if (effectType != null)
                return effectType;
        }

        if (legacyEffect != null && !legacyEffect.isBlank()) {
            PotionEffectType effectType = getEffectType(legacyEffect, null);
            if (effectType != null)
                return effectType;
        }

        Logs.logWarning("Invalid PotionEffectType: " + effect);
        return null;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private static PotionEffectType resolveEffectType(String effect) {
        NamespacedKey key = effect.contains(":")
                ? NamespacedKey.fromString(effect)
                : NamespacedKey.minecraft(effect);
        PotionEffectType effectType = null;
        try {
            if (key != null)
                effectType = Registry.POTION_EFFECT_TYPE.get(key);
        } catch (NoSuchFieldError | NoSuchMethodError ignored) {
            // Ignore errors on older versions
        }
        if (effectType == null)
            effectType = PotionEffectType.getByName(effect);
        if (effectType == null && key != null)
            effectType = PotionEffectType.getByKey(key);
        return effectType;
    }

    @Nullable
    private static String renamedEffect(String effect) {
        int separator = effect.indexOf(':');
        String namespace = separator >= 0 ? effect.substring(0, separator + 1) : "";
        String name = separator >= 0 ? effect.substring(separator + 1) : effect;
        return switch (name) {
            case "confusion" -> namespace + "nausea";
            case "nausea" -> namespace + "confusion";
            default -> null;
        };
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
