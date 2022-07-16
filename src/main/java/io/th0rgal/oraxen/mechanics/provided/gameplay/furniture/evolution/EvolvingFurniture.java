package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

public class EvolvingFurniture {

    private final String currentStage;
    private final boolean lightBoost;
    private final boolean rainBoost;
    private final boolean allowBoneMeal;
    private final int boneMealChance;
    private final String nextStage;
    private final double growChance;
    private final Random random = new Random();

    public EvolvingFurniture(String itemID, ConfigurationSection plantSection) {
        currentStage = itemID;
        lightBoost = plantSection.isBoolean("light_boost") && plantSection.getBoolean("light_boost");
        rainBoost = plantSection.isBoolean("rain_boost") && plantSection.getBoolean("rain_boost");
        allowBoneMeal = plantSection.isBoolean("allow_bone_meal") && plantSection.getBoolean("allow_bone_meal");
        boneMealChance = plantSection.getInt("bone_meal_chance");
        nextStage = plantSection.getString("next_stage");
        growChance = plantSection.getDouble("grow_chance");
    }
    public double getGrowChance() {
        return growChance;
    }

    public boolean isLightBoosted() {
        return lightBoost;
    }

    public boolean isRainBoosted() { return rainBoost; }

    public String getCurrentStage() {
        return currentStage;
    }

    public boolean getAllowBoneMeal() { return allowBoneMeal; }

    public int getBoneMealChance() { return boneMealChance; }

    public String getNextStage() {
        return nextStage;
    }
}
