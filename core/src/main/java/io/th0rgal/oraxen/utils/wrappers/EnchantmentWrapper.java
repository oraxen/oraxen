package io.th0rgal.oraxen.utils.wrappers;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static io.th0rgal.oraxen.utils.MinecraftVersion.getCurrentVersion;
import static io.th0rgal.oraxen.utils.MinecraftVersion.v1_20_5;

public class EnchantmentWrapper {

    public static final @NotNull Enchantment FORTUNE = Objects.requireNonNull(Registry.ENCHANTMENT.get(NamespacedKey.minecraft("fortune")));
    public static final @NotNull Enchantment EFFICIENCY = Objects.requireNonNull(Registry.ENCHANTMENT.get(NamespacedKey.minecraft("efficiency")));
    public static final @NotNull Enchantment SILK_TOUCH = Objects.requireNonNull(Registry.ENCHANTMENT.get(getCurrentVersion().isAtLeast(v1_20_5) ? NamespacedKey.minecraft("silk_touch") : NamespacedKey.minecraft("silktouch")));
}
