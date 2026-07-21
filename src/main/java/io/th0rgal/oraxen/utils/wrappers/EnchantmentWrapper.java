package io.th0rgal.oraxen.utils.wrappers;

import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EnchantmentWrapper {
    public static final @NotNull Enchantment FORTUNE = Objects.requireNonNull(getByKey(NamespacedKey.minecraft("fortune")));
    public static final @NotNull Enchantment EFFICIENCY = Objects.requireNonNull(getByKey(NamespacedKey.minecraft("efficiency")));
    public static final @NotNull Enchantment SILK_TOUCH = Objects.requireNonNull(getByKey(NamespacedKey.minecraft("silk_touch")));

    public static @Nullable Enchantment getByKey(@NotNull NamespacedKey key) {
        return VersionUtil.atOrAbove("1.21") ? Registry.ENCHANTMENT.get(key) : Enchantment.getByKey(key);
    }
}
