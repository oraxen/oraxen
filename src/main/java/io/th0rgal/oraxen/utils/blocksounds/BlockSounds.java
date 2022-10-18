package io.th0rgal.oraxen.utils.blocksounds;

import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.configuration.ConfigurationSection;

public class BlockSounds {

    private final String placeSound;
    private final String breakSound;
    private final String stepSound;
    private final String hitSound;
    private final String fallSound;
    private final float volume;
    private final float pitch;

    public BlockSounds(ConfigurationSection section) {
        placeSound = section.getString("place_sound", null);
        breakSound = section.getString("break_sound", null);
        stepSound = section.getString("step_sound", null);
        hitSound = section.getString("hit_sound", null);
        fallSound = section.getString("fall_sound", null);
        volume = (float) section.getDouble("volume", 1);
        pitch = (float) section.getDouble("pitch", 1);
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

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }
}
