package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

public class EvolvingFurniture {

    private final String currentStage;
    private final int delay;
    private final boolean isLightBoost;
    private int minimumLightLevel;
    private int lightBoostTick;
    private final boolean isRainBoost;
    private int rainBoostTick;
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
            lightBoostTick = section.getInt("boost_tick");
        } else isLightBoost = false;

        if (plantSection.isConfigurationSection("rain_boost")) {
            ConfigurationSection section = plantSection.getConfigurationSection("rain_boost");
            isRainBoost = true;
            rainBoostTick = section.getInt("boost_tick");
        } else isRainBoost = false;

        if (plantSection.isConfigurationSection("bone_meal")) {
            ConfigurationSection section = plantSection.getConfigurationSection("bone_meal");
            isBoneMeal = true;
            if (section.getInt("chance") > 100)
                boneMealChance = 100;
            else boneMealChance = Math.max(section.getInt("chance"), 0);
        } else isBoneMeal = false;

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

    public int getLightBoostTick() {
        return lightBoostTick;
    }
    public boolean isRainBoosted() {
        return isRainBoost;
    }

    public int getRainBoostTick() {
        return rainBoostTick;
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
