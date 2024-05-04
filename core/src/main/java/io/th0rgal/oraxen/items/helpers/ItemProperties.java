package io.th0rgal.oraxen.items.helpers;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

@SuppressWarnings("CallToPrintStackTrace")
public class ItemProperties {


    @Nullable
    public static Component itemName(ItemMeta itemMeta) {
        if (!VersionUtil.atOrAbove("1.20.5")) return null;

        return (Component) ItemPropertiesWrapper.getProperty(itemMeta, "itemName");
    }

    public static void itemName(ItemMeta itemMeta, @Nullable Component itemName) {
        if (!VersionUtil.atOrAbove("1.20.5")) return;
        try {
            Method method = itemMeta.getClass().getMethod("itemName", Component.class);
            method.setAccessible(true);
            method.invoke(itemMeta, itemName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static String getItemName(ItemMeta itemMeta) {
        if (!VersionUtil.atOrAbove("1.20.5")) return null;

        return (String) ItemPropertiesWrapper.getProperty(itemMeta, "getItemName");
    }

    public static void setItemName(ItemMeta itemMeta, @Nullable String itemName) {
        if (!VersionUtil.atOrAbove("1.20.5")) return;

        ItemPropertiesWrapper.setProperty(itemMeta, "setItemName", String.class, itemName);
    }

    public static boolean isFireResistant(ItemMeta itemMeta) {
        if (!VersionUtil.atOrAbove("1.20.5")) return false;

        return (boolean) ItemPropertiesWrapper.getProperty(itemMeta, "isFireResistant");
    }

    public static void setFireResistant(ItemMeta itemMeta, boolean fireResistant) {
        if (!VersionUtil.atOrAbove("1.20.5")) return;

        ItemPropertiesWrapper.setProperty(itemMeta, "setFireResistant", fireResistant);
    }

    @Nullable
    public static ItemRarityWrapper getRarity(ItemMeta itemMeta) {
        if (!VersionUtil.atOrAbove("1.20.5")) return null;
        if (!((boolean) ItemPropertiesWrapper.getProperty(itemMeta, "hasRarity"))) return null;
        return ItemRarityWrapper.valueOf(((Enum) ItemPropertiesWrapper.getProperty(itemMeta, "getRarity")).name());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setRarity(ItemMeta itemMeta, @Nullable ItemRarityWrapper rarity) {
        if (!VersionUtil.atOrAbove("1.20.5")) return;

        try {
            Class<?> itemRarityClass = Class.forName("org.bukkit.inventory.ItemRarity");
            Enum<?> itemRarity = rarity != null ? Enum.valueOf((Class<Enum>) itemRarityClass, rarity.name()) : null;

            ItemPropertiesWrapper.setProperty(itemMeta, "setRarity", itemRarityClass, itemRarity);
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
        }
    }


    @Nullable
    public static Integer getDurability(ItemMeta itemMeta) {
        if (!VersionUtil.atOrAbove("1.20.5")) return null;
        if (!(itemMeta instanceof Damageable damageable)) return null;
        if (!((boolean) ItemPropertiesWrapper.getProperty(damageable, "hasMaxDamage"))) return null;
        return (Integer) ItemPropertiesWrapper.getProperty(damageable, "getMaxDamage");
    }

    public static void setDurability(ItemMeta itemMeta, @Nullable Integer durability) {
        if (!VersionUtil.atOrAbove("1.20.5")) return;
        if (!(itemMeta instanceof Damageable damageable)) return;

        ItemPropertiesWrapper.setProperty(damageable, "setMaxDamage", Integer.class, durability);
    }

    public static boolean isHideTooltip(ItemMeta itemMeta) {
        if (!VersionUtil.atOrAbove("1.20.5")) return false;

        return (boolean) ItemPropertiesWrapper.getProperty(itemMeta, "isHideTooltip");
    }

    public static void setHideTooltip(ItemMeta itemMeta, boolean hideTooltip) {
        if (!VersionUtil.atOrAbove("1.20.5")) return;

        ItemPropertiesWrapper.setProperty(itemMeta, "setHideTooltip", hideTooltip);
    }

    @Nullable
    public static Boolean getEnchantmentGlintOverride(ItemMeta itemMeta) {
        if (!VersionUtil.atOrAbove("1.20.5")) return null;
        if (!((boolean) ItemPropertiesWrapper.getProperty(itemMeta, "hasEnchantmentGlintOverride"))) return null;
        return (Boolean) ItemPropertiesWrapper.getProperty(itemMeta, "getEnchantmentGlintOverride");
    }

    public static void setEnchantmentGlintOverride(ItemMeta itemMeta, @Nullable Boolean enchantmentGlintOverride) {
        if (!VersionUtil.atOrAbove("1.20.5")) return;

        ItemPropertiesWrapper.setProperty(itemMeta, "setEnchantmentGlintOverride", Boolean.class, enchantmentGlintOverride);
    }

    @Nullable
    public static FoodComponentWrapper getFood(ItemMeta itemMeta) {
        if (!VersionUtil.atOrAbove("1.20.5")) return null;
        if (!((boolean) ItemPropertiesWrapper.getProperty(itemMeta, "hasFood"))) return null;
        return FoodComponentWrapper.fromVanillaFoodComponent(ItemPropertiesWrapper.getProperty(itemMeta, "getFood"));
    }

    public static void setFood(ItemMeta itemMeta, @Nullable FoodComponentWrapper foodWrapper) {
        if (!VersionUtil.atOrAbove("1.20.5")) return;
        try {
            Object foodComponent = foodWrapper != null ? foodWrapper.toVanilla() : null;
            ItemPropertiesWrapper.setProperty(itemMeta, "setFood", "org.bukkit.inventory.meta.components.FoodComponent", foodComponent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Integer getMaxStackSize(ItemMeta itemMeta) {
        if (!VersionUtil.atOrAbove("1.20.5")) return null;
        if (!((boolean) ItemPropertiesWrapper.getProperty(itemMeta, "hasMaxStackSize"))) return null;
        return (Integer) ItemPropertiesWrapper.getProperty(itemMeta, "getMacStackSize");
    }

    public static void setMaxStackSize(ItemMeta itemMeta, @Nullable Integer maxStackSize) {
        if (!VersionUtil.atOrAbove("1.20.5")) return;
        ItemPropertiesWrapper.setProperty(itemMeta, "setMaxStackSize", Integer.class, maxStackSize);
    }
}

