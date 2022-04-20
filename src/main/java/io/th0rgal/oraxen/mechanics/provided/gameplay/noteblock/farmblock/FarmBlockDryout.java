package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

public class FarmBlockDryout {

    private final boolean lightBoost;
    private final int probability;
    private final int delay;
    private final Random random = new Random();
    private boolean isMoist;

    public FarmBlockDryout(String itemId, ConfigurationSection farmblockSection) {
        delay = farmblockSection.getInt("delay");
        probability = (int) (1D / (double) farmblockSection.get("probability", 1));
        lightBoost = farmblockSection.isBoolean("light_boost") && farmblockSection.getBoolean("light_boost");
    }

    public int getDelay() {
        return delay;
    }

    public boolean isLightBoosted() {
        return lightBoost;
    }

    public boolean bernoulliTest() {
        return random.nextInt(probability) == 0;
    }
}


