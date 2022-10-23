package io.th0rgal.oraxen.utils.blocksounds;

import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.configuration.ConfigurationSection;

public class BlockSounds {

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
        placeSound = section.getString("place");
        breakSound = section.getString("break");
        stepSound = section.getString("step");
        hitSound = getSound(section, "hit");
        fallSound = getSound(section, "fall");

        placeVolume = getFloat(section, "place_volume", "place", 1.0f);
        breakVolume = getFloat(section, "break_volume", "break", 1.0f);
        stepVolume = getFloat(section, "step_volume", "step", 0.15f);
        hitVolume = getFloat(section, "hit_volume", "hit", 0.25f);
        fallVolume = getFloat(section, "fall_volume", "fall", 0.5f);

        placePitch = getFloat(section, "place_pitch", "place", 0.8f);
        breakPitch = getFloat(section, "break_pitch", "break", 0.8f);
        stepPitch = getFloat(section, "step_pitch", "step", 1.0f);
        hitPitch = getFloat(section, "hit_pitch", "hit", 0.5f);
        fallPitch = getFloat(section, "fall_sound", "fall", 0.75f);
    }

    private String getSound(ConfigurationSection section, String key) {
        ConfigurationSection soundSection = section.getConfigurationSection(key);
        return section.isString(key + "_sound")
                ? section.getString(key + "_sound")
                : soundSection != null
                ? soundSection.getString("sound")
                : null;
    }

    private float getFloat(ConfigurationSection section, String key, String type, float defaultValue) {
        ConfigurationSection soundSection = section.getConfigurationSection(key);
        if (soundSection != null) {
            return (float) soundSection.getDouble(type, defaultValue);
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
