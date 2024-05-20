package io.th0rgal.oraxen.items.helpers;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public interface ItemPropertyHandler {

    @Nullable
    default Component itemName(ItemMeta itemMeta) {
        return null;
    }

    default void itemName(ItemMeta itemMeta, @Nullable Component itemName) {

    }

    @Nullable
    default String getItemName(ItemMeta itemMeta) {
        return null;
    }

    default void setItemName(ItemMeta itemMeta, @Nullable String itemName) {

    }

    default boolean isFireResistant(ItemMeta itemMeta) {
        return false;
    }

    default void setFireResistant(ItemMeta itemMeta, boolean fireResistant) {

    }

    @Nullable
    default ItemRarityWrapper getRarity(ItemMeta itemMeta) {
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default void setRarity(ItemMeta itemMeta, @Nullable ItemRarityWrapper rarity) {

    }


    @Nullable
    default Integer getDurability(ItemMeta itemMeta) {
        return null;
    }

    default void setDurability(ItemMeta itemMeta, @Nullable Integer durability) {

    }

    default boolean isHideTooltip(ItemMeta itemMeta) {
        return false;
    }
    default void setHideTooltip(ItemMeta itemMeta, boolean hideTooltip) {

    }

    @Nullable default Boolean getEnchantmentGlintOverride(ItemMeta itemMeta) {
        return null;
    }
    default void setEnchantmentGlintOverride(ItemMeta itemMeta, @Nullable Boolean enchantmentGlintOverride) {

    }

    @Nullable
    default FoodComponentWrapper getFood(ItemMeta itemMeta) {
        return null;
    }

     default void setFood(ItemMeta itemMeta, @Nullable FoodComponentWrapper foodWrapper) {

     }

    @Nullable
     default Integer getMaxStackSize(ItemMeta itemMeta) {
        return null;
    }

    default void setMaxStackSize(ItemMeta itemMeta, @Nullable Integer maxStackSize) {

    }
}
