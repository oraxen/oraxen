package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Random;

/**
 * Represents a single growth stage in a multi-stage plant/furniture evolution.
 * Each stage has its own model, evolution settings, and drop configuration.
 */
public class GrowthStage {
    
    private final String modelKey;           // Reference to Pack.models key (e.g., "stage0")
    private final Drop drop;                 // Stage-specific drops
    private final int light;                 // Light level for this stage (-1 = inherit from mechanic)
    
    // Evolution settings (null = final stage, no further evolution)
    private final int delay;
    private final double probability;
    private final boolean lightBoost;
    private final int minimumLightLevel;
    private final int lightBoostTick;
    private final boolean rainBoost;
    private final int rainBoostTick;
    private final boolean boneMeal;
    private final int boneMealChance;
    
    private final Random random = new Random();
    
    /**
     * Creates a growth stage from a configuration section.
     * 
     * @param section The configuration section for this stage
     * @param defaultDrop The default drop from the parent mechanic (used if stage has no drop)
     * @param parentItemId The parent item ID (used as fallback source for drops)
     */
    public GrowthStage(ConfigurationSection section, @Nullable Drop defaultDrop, String parentItemId) {
        this.modelKey = section.getString("model", "");
        this.light = section.getInt("light", -1);
        
        // Parse stage-specific drop
        // Use parentItemId as sourceID for drop fallback (avoids NPE from empty string)
        ConfigurationSection dropSection = section.getConfigurationSection("drop");
        if (dropSection != null) {
            this.drop = Drop.createDrop(FurnitureFactory.getInstance().toolTypes, dropSection, parentItemId);
        } else {
            this.drop = defaultDrop;
        }
        
        // Parse evolution settings
        ConfigurationSection evoSection = section.getConfigurationSection("evolution");
        if (evoSection != null) {
            this.delay = evoSection.getInt("delay", 10000);
            this.probability = evoSection.getDouble("probability", 1.0);
            
            // Light boost
            if (evoSection.isConfigurationSection("light_boost")) {
                ConfigurationSection lightSection = evoSection.getConfigurationSection("light_boost");
                this.lightBoost = true;
                this.minimumLightLevel = lightSection.getInt("minimum_light_level", 9);
                this.lightBoostTick = lightSection.getInt("boost_tick", 500);
            } else if (evoSection.getBoolean("light_boost", false)) {
                // Shorthand: light_boost: true
                this.lightBoost = true;
                this.minimumLightLevel = 9;
                this.lightBoostTick = 500;
            } else {
                this.lightBoost = false;
                this.minimumLightLevel = 0;
                this.lightBoostTick = 0;
            }
            
            // Rain boost
            if (evoSection.isConfigurationSection("rain_boost")) {
                ConfigurationSection rainSection = evoSection.getConfigurationSection("rain_boost");
                this.rainBoost = true;
                this.rainBoostTick = rainSection.getInt("boost_tick", 500);
            } else if (evoSection.getBoolean("rain_boost", false)) {
                this.rainBoost = true;
                this.rainBoostTick = 500;
            } else {
                this.rainBoost = false;
                this.rainBoostTick = 0;
            }
            
            // Bone meal
            if (evoSection.isConfigurationSection("bone_meal")) {
                ConfigurationSection boneSection = evoSection.getConfigurationSection("bone_meal");
                this.boneMeal = true;
                this.boneMealChance = Math.min(100, Math.max(0, boneSection.getInt("chance", 50)));
            } else if (evoSection.getBoolean("bone_meal", false)) {
                this.boneMeal = true;
                this.boneMealChance = 50;
            } else {
                this.boneMeal = false;
                this.boneMealChance = 0;
            }
        } else {
            // No evolution = final stage
            this.delay = 0;
            this.probability = 0;
            this.lightBoost = false;
            this.minimumLightLevel = 0;
            this.lightBoostTick = 0;
            this.rainBoost = false;
            this.rainBoostTick = 0;
            this.boneMeal = false;
            this.boneMealChance = 0;
        }
    }
    
    /**
     * @return The model key (reference to Pack.models)
     */
    public String getModelKey() {
        return modelKey;
    }
    
    /**
     * @return The drop configuration for this stage
     */
    @Nullable
    public Drop getDrop() {
        return drop;
    }
    
    /**
     * @return The light level for this stage, or -1 to inherit from mechanic
     */
    public int getLight() {
        return light;
    }
    
    /**
     * @return Whether this stage has evolution settings (can evolve to next stage)
     */
    public boolean hasEvolution() {
        return delay > 0;
    }
    
    /**
     * @return The delay in ticks before evolution check
     */
    public int getDelay() {
        return delay;
    }
    
    /**
     * @return Whether light boost is enabled
     */
    public boolean isLightBoosted() {
        return lightBoost;
    }
    
    /**
     * @return The minimum light level for light boost
     */
    public int getMinimumLightLevel() {
        return minimumLightLevel;
    }
    
    /**
     * @return The extra ticks added per evolution check when light boosted
     */
    public int getLightBoostTick() {
        return lightBoostTick;
    }
    
    /**
     * @return Whether rain boost is enabled
     */
    public boolean isRainBoosted() {
        return rainBoost;
    }
    
    /**
     * @return The extra ticks added per evolution check when rain boosted
     */
    public int getRainBoostTick() {
        return rainBoostTick;
    }
    
    /**
     * @return Whether bone meal can be used on this stage
     */
    public boolean isBoneMeal() {
        return boneMeal;
    }
    
    /**
     * @return The chance (0-100) that bone meal advances this stage
     */
    public int getBoneMealChance() {
        return boneMealChance;
    }
    
    /**
     * Performs a Bernoulli test for evolution probability.
     * @return true if the evolution should proceed
     */
    public boolean bernoulliTest() {
        if (probability >= 1.0) return true;
        if (probability <= 0.0) return false;
        return random.nextDouble() < probability;
    }
}

