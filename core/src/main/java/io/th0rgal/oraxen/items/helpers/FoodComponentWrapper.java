package io.th0rgal.oraxen.items.helpers;

import org.bukkit.potion.PotionEffect;

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
        this.eatSeconds = Math.max(eatSeconds, 0.0F);
        return this;
    }

    public List<FoodEffectWrapper> getEffects() {
        return effects;
    }

    public void addEffect(PotionEffect potionEffect, float probability) {
    }

    public FoodComponentWrapper setEffects(List<FoodEffectWrapper> effects) {
        this.effects = effects;
        return this;
    }
}

