package io.th0rgal.oraxen.items.helpers;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

public interface FoodEffectWrapper extends ConfigurationSerializable {

    /**
     * Gets the effect which may be applied.
     *
     * @return the effect
     */
    @NotNull
    PotionEffect getEffect();

    /**
     * Sets the effect which may be applied.
     *
     * @param effect the new effect
     */
    void setEffect(@NotNull PotionEffect effect);

    /**
     * Gets the probability of this effect being applied.
     *
     * @return probability
     */
    float getProbability();

    /**
     * Sets the probability of this effect being applied.
     *
     * @param probability between 0 and 1 inclusive.
     */
    void setProbability(float probability);
}
