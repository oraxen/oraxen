package io.th0rgal.oraxen.items.helpers;

import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FoodComponentWrapper {

    private static final float DEFAULT_EAT_SECONDS = 1.6F;

    private int nutrition;
    private float saturation;
    private boolean canAlwaysEat;
    private float eatSeconds;
    private List<FoodEffectWrapper> effects;

    public FoodComponentWrapper() {
        nutrition = 0;
        saturation = 0.0F;
        canAlwaysEat = false;
        eatSeconds = DEFAULT_EAT_SECONDS;
        effects = new ArrayList<>();
    }

    public static FoodComponentWrapper fromVanillaFoodComponent(@Nullable Object foodComponent) {
        FoodComponentWrapper foodWrapper = new FoodComponentWrapper();
        if (foodComponent != null) try {
            Class<?> foodClass = foodComponent.getClass();
            foodWrapper = foodWrapper
                    .setNutrition((Integer) foodClass.getDeclaredMethod("getNutrition").invoke(foodComponent))
                    .setSaturation((Float) foodClass.getDeclaredMethod("getSaturation").invoke(foodComponent))
                    .setCanAlwaysEat((Boolean) foodClass.getDeclaredMethod("getCanAlwaysEat").invoke(foodComponent))
                    .setEatSeconds((Float) foodClass.getDeclaredMethod("getEatSeconds").invoke(foodComponent))
            //TODO .setEffects(foodClass.getDeclaredMethod("getEffects"))
            ;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return foodWrapper;
    }

    @Nullable
    public Object toVanilla() {
        if (!VersionUtil.atOrAbove("1.20.5")) return null;

        Object foodComponent = new Object();
        try {
            ItemMeta tempMeta = new ItemStack(Material.PAPER).getItemMeta();
            foodComponent = tempMeta.getClass().getDeclaredMethod("getFood").invoke(tempMeta);
            foodComponent.getClass().getDeclaredMethod("setNutrition", Integer.class).invoke(foodComponent, nutrition);
            foodComponent.getClass().getDeclaredMethod("setSaturation", Float.class).invoke(foodComponent, saturation);
            foodComponent.getClass().getDeclaredMethod("setCanAlwaysEat", Boolean.class).invoke(foodComponent, canAlwaysEat);
            foodComponent.getClass().getDeclaredMethod("setEatSeconds", Float.class).invoke(foodComponent, eatSeconds);
            foodComponent.getClass().getDeclaredMethod("setEffects", List.class).invoke(foodComponent, effects);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return foodComponent;
    }

    public int getNutrition() {
        return this.nutrition;
    }

    public FoodComponentWrapper setNutrition(int nutrition) {
        this.nutrition = nutrition;
        return this;
    }

    public float getSaturation() {
        return this.saturation;
    }

    public FoodComponentWrapper setSaturation(float saturation) {
        this.saturation = saturation;
        return this;
    }

    public boolean getCanAlwaysEat() {
        return canAlwaysEat;
    }

    public FoodComponentWrapper setCanAlwaysEat(boolean canAlwaysEat) {
        this.canAlwaysEat = canAlwaysEat;
        return this;
    }

    public float getEatSeconds() {
        return eatSeconds;
    }

    public FoodComponentWrapper setEatSeconds(float eatSeconds) {
        this.eatSeconds = eatSeconds;
        return this;
    }

    public void addEffect(PotionEffect potionEffect, float probability) {
    }

    public FoodComponentWrapper setEffects(List<FoodEffectWrapper> effects) {
        this.effects = effects;
        return this;
    }
}

