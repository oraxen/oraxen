package io.th0rgal.oraxen.nms.v1_20_R4;

import io.th0rgal.oraxen.items.helpers.FoodComponentWrapper;
import io.th0rgal.oraxen.items.helpers.FoodEffectWrapper;
import io.th0rgal.oraxen.items.helpers.ItemPropertyHandler;
import io.th0rgal.oraxen.items.helpers.ItemRarityWrapper;
import net.kyori.adventure.text.Component;
import net.minecraft.world.food.FoodProperties;
import org.bukkit.craftbukkit.inventory.components.CraftFoodComponent;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

@SuppressWarnings({"CallToPrintStackTrace", "UnstableApiUsage"})
public class ItemProperties implements ItemPropertyHandler {


    @Nullable
    @Override
    public Component itemName(ItemMeta itemMeta) {
        return itemMeta.hasItemName() ? itemMeta.itemName() : null;
    }

    @Override
    public void itemName(ItemMeta itemMeta, @Nullable Component itemName) {
        itemMeta.itemName(itemName);
    }

    @Nullable
    @Override
    public String getItemName(ItemMeta itemMeta) {
        return itemMeta.hasItemName() ? itemMeta.getItemName() : null;
    }

    @Override
    public void setItemName(ItemMeta itemMeta, @Nullable String itemName) {
        itemMeta.setItemName(itemName);
    }

    @Override
    public boolean isFireResistant(ItemMeta itemMeta) {
        return itemMeta.isFireResistant();
    }

    @Override
    public void setFireResistant(ItemMeta itemMeta, boolean fireResistant) {
        itemMeta.setFireResistant(fireResistant);
    }

    @Nullable
    @Override
    public ItemRarityWrapper getRarity(ItemMeta itemMeta) {
        return itemMeta.hasRarity() ? ItemRarityWrapper.valueOf(itemMeta.getRarity().name()) : null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void setRarity(ItemMeta itemMeta, @Nullable ItemRarityWrapper rarity) {
        itemMeta.setRarity(rarity != null ? ItemRarity.valueOf(rarity.name()) : null);
    }


    @Nullable
    @Override
    public Integer getDurability(ItemMeta itemMeta) {
        return !(itemMeta instanceof Damageable damageable) ? null : damageable.hasMaxDamage() ? damageable.getMaxDamage() : null;
    }

    @Override
    public void setDurability(ItemMeta itemMeta, @Nullable Integer durability) {
        if (itemMeta instanceof Damageable damageable)
            damageable.setMaxDamage(durability);
    }

    @Override
    public boolean isHideTooltip(ItemMeta itemMeta) {
        return itemMeta.isHideTooltip();
    }

    @Override
    public void setHideTooltip(ItemMeta itemMeta, boolean hideTooltip) {
        itemMeta.setHideTooltip(hideTooltip);
    }

    @Nullable
    @Override
    public Boolean getEnchantmentGlintOverride(ItemMeta itemMeta) {
        return itemMeta.hasEnchantmentGlintOverride() ? itemMeta.getEnchantmentGlintOverride() : null;
    }

    @Override
    public void setEnchantmentGlintOverride(ItemMeta itemMeta, @Nullable Boolean enchantmentGlintOverride) {
        itemMeta.setEnchantmentGlintOverride(enchantmentGlintOverride);
    }

    @Nullable
    @Override
    public FoodComponentWrapper getFood(ItemMeta itemMeta) {
        FoodComponent food = itemMeta.hasFood() ? itemMeta.getFood() : null;
        return food == null ? null : new FoodComponentWrapper()
                .setEatSeconds(food.getEatSeconds())
                .setCanAlwaysEat(food.canAlwaysEat())
                .setSaturation(food.getSaturation())
                .setNutrition(food.getNutrition())
                .setEffects(food.getEffects().stream().map(f -> new FoodEffectWrapper().setEffect(f.getEffect()).setProbability(f.getProbability())).toList());
    }

    @Override
    public void setFood(ItemMeta itemMeta, @Nullable FoodComponentWrapper foodWrapper) {
        if (foodWrapper == null) itemMeta.setFood(null);
        else {
            FoodComponent foodComponent = new CraftFoodComponent(new FoodProperties(0, 0, false, 0, Collections.emptyList()));
            foodComponent.setEatSeconds(foodWrapper.getEatSeconds());
            foodComponent.setNutrition(foodWrapper.getNutrition());
            foodComponent.setCanAlwaysEat(foodWrapper.getCanAlwaysEat());
            foodComponent.setSaturation(foodWrapper.getSaturation());
            foodComponent.setEffects(foodWrapper.getEffects().stream().map(f -> {
                        FoodProperties.PossibleEffect foodEffect = new FoodProperties.PossibleEffect(CraftPotionUtil.fromBukkit(f.getEffect()), f.getProbability());
                        return (FoodComponent.FoodEffect) new CraftFoodComponent.CraftFoodEffect(foodEffect);
                    }
            ).toList());
            itemMeta.setFood(foodComponent);
        }

    }

    @Override
    public Integer getMaxStackSize(ItemMeta itemMeta) {
        return itemMeta.hasMaxStackSize() ? itemMeta.getMaxStackSize() : null;
    }

    @Override
    public void setMaxStackSize(ItemMeta itemMeta, @Nullable Integer maxStackSize) {
        itemMeta.setMaxStackSize(maxStackSize);
    }
}

