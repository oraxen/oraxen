package io.th0rgal.oraxen.utils.wrappers;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

public class EnchantmentWrapper {

    public static final Enchantment FORTUNE = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("fortune"));
    public static final Enchantment EFFICIENCY = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("efficiency"));
    public static final Enchantment SILK_TOUCH = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("silktouch"));
}
