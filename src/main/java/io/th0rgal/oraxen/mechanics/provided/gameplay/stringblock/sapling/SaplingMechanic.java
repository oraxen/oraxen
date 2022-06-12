package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;

public class SaplingMechanic {

    public static final NamespacedKey SAPLING_KEY = new NamespacedKey(OraxenPlugin.get(), "sapling");
    private final String id;
    private final boolean canGrowNaturally;
    private final int naturalGrowthTime;
    private final boolean canGrowFromBoneMeal;
    private final int boneMealGrowChance;
    private final String growSound;
    private final int minLightLevel;
    private final boolean requiresWaterSource;
    private final String schematicName;

    public SaplingMechanic(String itemId, ConfigurationSection section) {
        id = itemId;
        canGrowNaturally = section.getBoolean("canGrowNaturally", true);
        naturalGrowthTime = section.getInt("", 6000); // Default of 5 mins
        canGrowFromBoneMeal = section.getBoolean("canGrowFromBoneMeal", true);
        boneMealGrowChance = Math.min(section.getInt("boneMealGrowChance", 50), 100);
        growSound = section.getString("growSound", null);
        minLightLevel = section.getInt("minLightLevel", 0);
        requiresWaterSource = section.getBoolean("requiresWaterSource", false);
        schematicName = section.getString("schematicName", null);
    }

    public boolean requiresWaterSource() {
        return requiresWaterSource;
    }

    public boolean requiresLight() {
        return minLightLevel != 0;
    }

    public int getMinLightLevel() {
        return minLightLevel;
    }

    public boolean canGrowNaturally() {
        return canGrowNaturally;
    }

    public int getNaturalGrowthTime() {
        return naturalGrowthTime;
    }

    public boolean canGrowFromBoneMeal() {
        return canGrowFromBoneMeal;
    }

    public int getBoneMealGrowChance() {
        return boneMealGrowChance;
    }

    public boolean hasGrowSound() {
        return growSound != null;
    }

    public String getGrowSound() {
        return growSound;
    }

    public boolean hasSchematic() {
        return (schematicName != null && getSchematic() != null);
    }

    public String getSchematicName() {
        if (schematicName.endsWith(".schem")) return schematicName;
        else return schematicName + ".schem";
    }

    public File getSchematic() {
        File schem = new File(OraxenPlugin.get().getConfigsManager().getSchematicsFolder().getAbsolutePath() + "/" + getSchematicName());
        if (!schem.exists()) return null;
        else return schem;
    }

    public boolean isInWater(Block block) {
        return block.getRelative(BlockFace.DOWN).getType() == Material.WATER;
    }
}
