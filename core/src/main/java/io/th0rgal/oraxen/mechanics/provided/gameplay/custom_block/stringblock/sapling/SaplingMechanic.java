package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.sapling;

import io.th0rgal.oraxen.OraxenPlugin;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;

public class SaplingMechanic {

    public static final NamespacedKey SAPLING_KEY = new NamespacedKey(OraxenPlugin.get(), "sapling");
    private final boolean canGrowNaturally;
    private final int naturalGrowthTime;
    private final boolean canGrowFromBoneMeal;
    private final int boneMealGrowthSpeedup;
    private final String growSound;
    private final int minLightLevel;
    private final boolean requiresWaterSource;
    private final String schematicName;
    private final boolean shouldReplaceBlocks;
    private final boolean shouldCopyBiomes;
    private final boolean shouldCopyEntities;

    public SaplingMechanic(String itemId, ConfigurationSection section) {
        canGrowNaturally = section.getBoolean("grows_naturally", true);
        naturalGrowthTime = section.getInt("natural_growth_time", 6000); // Default of 5 mins
        canGrowFromBoneMeal = section.getBoolean("grows_from_bonemeal", true);
        boneMealGrowthSpeedup = Math.min(section.getInt("bonemeal_growth_speedup", naturalGrowthTime/5), naturalGrowthTime);
        growSound = section.getString("grow_sound", null);
        minLightLevel = section.getInt("min_light_level", 0);
        requiresWaterSource = section.getBoolean("requires_water_source", false);
        schematicName = section.getString("schematic", null);
        shouldReplaceBlocks = section.getBoolean("replace_blocks", false);
        shouldCopyBiomes = section.getBoolean("copy_biomes", false);
        shouldCopyEntities = section.getBoolean("copy_entities", false);
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

    public int getBoneMealGrowthSpeedup() {
        return boneMealGrowthSpeedup;
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
        return StringUtils.appendIfMissing(schematicName, ".schem");
    }

    public File getSchematic() {
        File schem = new File(OraxenPlugin.get().configsManager().getSchematicsFolder().getAbsolutePath() + "/" + getSchematicName());
        if (!schem.exists()) return null;
        else return schem;
    }

    public boolean replaceBlocks() { return shouldReplaceBlocks; }
    public boolean copyBiomes() { return shouldCopyBiomes; }
    public boolean copyEntities() { return shouldCopyEntities; }

    public boolean isUnderWater(Block block) {
        return block.getRelative(BlockFace.DOWN).getType() == Material.WATER;
    }
}
