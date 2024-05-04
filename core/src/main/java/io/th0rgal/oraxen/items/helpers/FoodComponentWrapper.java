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
        nutrition = 1;
        saturation = 0.1F;
        canAlwaysEat = false;
        eatSeconds = DEFAULT_EAT_SECONDS;
        effects = new ArrayList<>();
    }

    public static FoodComponentWrapper fromVanillaFoodComponent(@Nullable Object foodComponent) {
        FoodComponentWrapper foodWrapper = new FoodComponentWrapper();
        if (foodComponent != null) try {
            foodWrapper = foodWrapper
                    .setNutrition((Integer) ItemPropertiesWrapper.getProperty(foodComponent, "getNutrition"))
                    .setSaturation((Float) ItemPropertiesWrapper.getProperty(foodComponent, "getSaturation"))
                    .setCanAlwaysEat((Boolean) ItemPropertiesWrapper.getProperty(foodComponent, "canAlwaysEat"))
                    .setEatSeconds((Float) ItemPropertiesWrapper.getProperty(foodComponent, "getEatSeconds"))
                    .setEffects(new ArrayList<>())
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
            foodComponent = ItemPropertiesWrapper.getProperty(tempMeta, "getFood");
            if (foodComponent == null) return null;
            ItemPropertiesWrapper.setProperty(foodComponent, "setNutrition", Integer.class, nutrition);
            ItemPropertiesWrapper.setProperty(foodComponent, "setSaturation", Integer.class, saturation);
            ItemPropertiesWrapper.setProperty(foodComponent, "setCanAlwaysEat", Integer.class, canAlwaysEat);
            ItemPropertiesWrapper.setProperty(foodComponent, "setEatSeconds", Integer.class, eatSeconds);
            ItemPropertiesWrapper.setProperty(foodComponent, "setEffects", Integer.class, effects);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return foodComponent;
    }

    public int getNutrition() {
        return this.nutrition;
    }

    public FoodComponentWrapper setNutrition(int nutrition) {
        this.nutrition = Math.max(nutrition, 0);
        return this;
    }

    public float getSaturation() {
        return this.saturation;
    }

    public FoodComponentWrapper setSaturation(float saturation) {
        this.saturation = Math.max(saturation, 0.0f);
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
        this.eatSeconds = Math.min(eatSeconds, 0.0F);
        return this;
    }

    public void addEffect(PotionEffect potionEffect, float probability) {
    }

    public FoodComponentWrapper setEffects(List<FoodEffectWrapper> effects) {
        this.effects = effects;
        return this;
    }
}

