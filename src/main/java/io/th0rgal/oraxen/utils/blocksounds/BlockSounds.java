package io.th0rgal.oraxen.utils.blocksounds;

import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.configuration.ConfigurationSection;

public class BlockSounds {

    public static String VANILLA_STONE_PLACE = "minecraft:required.stone.place";
    public static String VANILLA_STONE_BREAK = "minecraft:required.stone.break";
    public static String VANILLA_STONE_HIT = "minecraft:required.stone.hit";
    public static String VANILLA_STONE_STEP = "minecraft:required.stone.step";
    public static String VANILLA_STONE_FALL = "minecraft:required.stone.fall";

    public static String VANILLA_WOOD_PLACE = "minecraft:required.wood.place";
    public static String VANILLA_WOOD_BREAK = "minecraft:required.wood.break";
    public static String VANILLA_WOOD_HIT = "minecraft:required.wood.hit";
    public static String VANILLA_WOOD_STEP = "minecraft:required.wood.step";
    public static String VANILLA_WOOD_FALL = "minecraft:required.wood.fall";

    public static float VANILLA_PLACE_VOLUME = 1.0f;
    public static float VANILLA_PLACE_PITCH = 0.8f;
    public static float VANILLA_BREAK_VOLUME = 1.0f;
    public static float VANILLA_BREAK_PITCH = 0.8f;
    public static float VANILLA_HIT_VOLUME = 0.25f;
    public static float VANILLA_HIT_PITCH = 0.5f;
    public static float VANILLA_STEP_VOLUME = 0.15f;
    public static float VANILLA_STEP_PITCH = 1.0f;
    public static float VANILLA_FALL_VOLUME = 0.5f;
    public static float VANILLA_FALL_PITCH = 0.75f;

    private final String placeSound;
    private final String breakSound;
    private final String stepSound;
    private final String hitSound;
    private final String fallSound;
    private final float placeVolume;
    private final float breakVolume;
    private final float stepVolume;
    private final float hitVolume;
    private final float fallVolume;
    private final float placePitch;
    private final float breakPitch;
    private final float stepPitch;
    private final float hitPitch;
    private final float fallPitch;

    public BlockSounds(ConfigurationSection section) {
        placeSound = getSound(section, "place");
        breakSound = getSound(section, "break");
        stepSound = getSound(section, "step");
        hitSound = getSound(section, "hit");
        fallSound = getSound(section, "fall");

        placeVolume = getVolume(section, "place", VANILLA_PLACE_VOLUME);
        breakVolume = getVolume(section, "break", VANILLA_BREAK_VOLUME);
        stepVolume = getVolume(section, "step", VANILLA_STEP_VOLUME);
        hitVolume = getVolume(section, "hit", VANILLA_HIT_VOLUME);
        fallVolume = getVolume(section, "fall", VANILLA_FALL_VOLUME);

        placePitch = getPitch(section, "place", VANILLA_PLACE_PITCH);
        breakPitch = getPitch(section, "break", VANILLA_BREAK_PITCH);
        stepPitch = getPitch(section, "step", VANILLA_STEP_PITCH);
        hitPitch = getPitch(section, "hit", VANILLA_HIT_PITCH);
        fallPitch = getPitch(section, "fall", VANILLA_FALL_PITCH);
    }

    private String getSound(ConfigurationSection section, String key) {
        ConfigurationSection soundSection = section.getConfigurationSection(key);
        return section.isString(key + "_sound")
                ? section.getString(key + "_sound")
                : soundSection != null
                ? soundSection.getString("sound")
                : null;
    }

    private float getVolume(ConfigurationSection section, String type, float defaultValue) {
        ConfigurationSection soundSection = section.getConfigurationSection(type);
        if (soundSection == null) {
            return (float) section.getDouble("volume", defaultValue);
        } else return defaultValue;
    }

    private float getPitch(ConfigurationSection section, String type, float defaultValue) {
        ConfigurationSection soundSection = section.getConfigurationSection(type);
        if (soundSection != null) {
            return (float) soundSection.getDouble("pitch", defaultValue);
        } else return defaultValue;
    }

    public boolean hasPlaceSound() {
        return placeSound != null;
    }

    public String getPlaceSound() {
        return BlockHelpers.validateReplacedSounds(placeSound);
    }

    public boolean hasBreakSound() {
        return breakSound != null;
    }

    public String getBreakSound() {
        return BlockHelpers.validateReplacedSounds(breakSound);
    }

    public boolean hasStepSound() {
        return stepSound != null;
    }

    public String getStepSound() {
        return BlockHelpers.validateReplacedSounds(stepSound);
    }

    public boolean hasHitSound() {
        return hitSound != null;
    }

    public String getHitSound() {
        return BlockHelpers.validateReplacedSounds(hitSound);
    }

    public boolean hasFallSound() {
        return fallSound != null;
    }

    public String getFallSound() {
        return BlockHelpers.validateReplacedSounds(fallSound);
    }

    public float getPlaceVolume() {
        return placeVolume;
    }

    public float getBreakVolume() {
        return breakVolume;
    }

    public float getStepVolume() {
        return stepVolume;
    }

    public float getHitVolume() {
        return hitVolume;
    }

    public float getFallVolume() {
        return fallVolume;
    }

    public float getPlacePitch() {
        return placePitch;
    }

    public float getBreakPitch() {
        return breakPitch;
    }

    public float getStepPitch() {
        return stepPitch;
    }

    public float getHitPitch() {
        return hitPitch;
    }

    public float getFallPitch() {
        return fallPitch;
    }
}
