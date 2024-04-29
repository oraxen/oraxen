package io.th0rgal.oraxen.utils;

import org.bukkit.enchantments.Enchantment;

import static io.th0rgal.oraxen.utils.ItemUtils.*;

public class EnchantmentWrapper {
    public static Enchantment efficiency() {
        return VersionUtil.atOrAbove("1.20.5") ? enchantment("efficiency") : enchantment("dig_speed");
    }
    public static Enchantment waterWalker() {
        return VersionUtil.atOrAbove("1.20.5") ? enchantment("frost_walker") : enchantment("water_walker");
    }
    public static Enchantment fortune() {
        return VersionUtil.atOrAbove("1.20.5") ? enchantment("fortune") : enchantment("loot_bonus_blocks");
    }
}
