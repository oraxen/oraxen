package io.th0rgal.oraxen.items.helpers;

import org.bukkit.potion.PotionEffect;

public class FoodEffectWrapper {

    private PotionEffect potionEffect;
    private float probability;

    public PotionEffect getEffect() {
        return potionEffect;
    }

    public FoodEffectWrapper setEffect(PotionEffect potionEffect) {
        this.potionEffect = potionEffect;
        return this;
    }

    public float getProbability() {
        return probability;
    }

    public FoodEffectWrapper setProbability(float probability) {
        this.probability = Math.max(0,Math.min(1, probability));
        return this;
    }
}
