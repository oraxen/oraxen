package io.th0rgal.oraxen.utils.wrappers;

import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EnchantmentWrapper {
    private static Enchantment FORTUNE_VALUE;
    private static Enchantment EFFICIENCY_VALUE;
    private static Enchantment SILK_TOUCH_VALUE;

    static {
        try {
            if (VersionUtil.isPaperServer()) {
                FORTUNE_VALUE = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("fortune"));
                EFFICIENCY_VALUE = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("efficiency"));
                SILK_TOUCH_VALUE = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("silk_touch"));
            } else {
                // Fallback for non-Paper servers using standard Bukkit API
                FORTUNE_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("fortune"));
                EFFICIENCY_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("efficiency"));
                SILK_TOUCH_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("silk_touch"));
            }
        } catch (NoSuchMethodError e) {
            // Fallback if Registry.ENCHANTMENT is not available
            FORTUNE_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("fortune"));
            EFFICIENCY_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("efficiency"));
            SILK_TOUCH_VALUE = Enchantment.getByKey(NamespacedKey.minecraft("silk_touch"));
        }
    }

    public static final @NotNull Enchantment FORTUNE = Objects.requireNonNull(FORTUNE_VALUE);
    public static final @NotNull Enchantment EFFICIENCY = Objects.requireNonNull(EFFICIENCY_VALUE);
    public static final @NotNull Enchantment SILK_TOUCH = Objects.requireNonNull(SILK_TOUCH_VALUE);
}
