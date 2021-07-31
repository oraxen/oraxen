package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

public class EvolvingFurniture {

    private final String currentStage;
    private final int delay;
    private final boolean lightBoost;
    private final String nextStage;
    private final int probability;
    private final Random random = new Random();

    public EvolvingFurniture(String itemID, ConfigurationSection plantSection) {
        currentStage = itemID;
        delay = plantSection.getInt("delay");
        lightBoost = plantSection.isBoolean("light_boost") && plantSection.getBoolean("light_boost");
        nextStage = plantSection.getString("next_stage");
        probability = (int) (1D / (double) plantSection.get("probability", 1));
    }

    public int getDelay() {
        return delay;
    }

    public boolean isLightBoosted() {
        return lightBoost;
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
