package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

public class EvolvingFurniture {

    private final String currentStage;
    private final int delay;
    private final boolean isLightBoost;
    private int minimumLightLevel;
    private final boolean isRainBoost;
    private final boolean isBoneMeal;
    private int boneMealChance;
    private final String nextStage;
    private final int probability;
    private final Random random = new Random();

    public EvolvingFurniture(String itemID, ConfigurationSection plantSection) {
        currentStage = itemID;
        delay = plantSection.getInt("delay");
        if (plantSection.isConfigurationSection("light_boost")) {
            ConfigurationSection section = plantSection.getConfigurationSection("light_boost");
            isLightBoost = true;
            minimumLightLevel = section.getInt("minimum_light_level");
        }
        else {
            isLightBoost = false;
        }
        isRainBoost = plantSection.getBoolean("rain_boost");
        if (plantSection.isConfigurationSection("bone_meal")) {
            ConfigurationSection section = plantSection.getConfigurationSection("bone_meal");
            isBoneMeal = true;
            if (section.getInt("chance") > 100)
                boneMealChance = 100;
            else if (section.getInt("chance") < 0)
                boneMealChance = 0;
            else
                boneMealChance = section.getInt("chance");
        }
        else {
            isBoneMeal = false;
        }
        nextStage = plantSection.getString("next_stage");
        probability = (int) (1D / (double) plantSection.get("probability", 1));
    }

    public int getDelay() {
        return delay;
    }
    public boolean isLightBoosted() {
        return isLightBoost;
    }
    public int getMinimumLightLevel() {
        return minimumLightLevel;
    }
    public boolean isRainBoosted() {
        return isRainBoost;
    }
    public boolean isBoneMeal() {
        return isBoneMeal;
    }
    public int getBoneMealChance() {
        return boneMealChance;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public String getNextStage() {
        return nextStage;
    }

    public boolean bernoulliTest() {
        return random.nextInt(probability) == 0;
    }
}
