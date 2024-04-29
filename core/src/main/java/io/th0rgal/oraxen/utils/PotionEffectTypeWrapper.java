package io.th0rgal.oraxen.utils;

import org.bukkit.potion.PotionEffectType;

import static io.th0rgal.oraxen.utils.PotionUtils.*;

public class PotionEffectTypeWrapper {
    public static PotionEffectType fastDigging() {
        return VersionUtil.atOrAbove("1.20.5") ? type("haste") : type("fast_digging");
    }
    public static PotionEffectType slowDigging() {
        return VersionUtil.atOrAbove("1.20.5") ? type("mining_fatigue") : type("slow_digging");
    }
}
