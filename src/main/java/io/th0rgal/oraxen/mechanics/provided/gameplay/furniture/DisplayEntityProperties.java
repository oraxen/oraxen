package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;

import java.util.Arrays;

public class DisplayEntityProperties {
    private final Color glowColor;
    private Integer viewRange;
    private final Display.Brightness brightness;
    private ItemDisplay.ItemDisplayTransform transform;
    private Display.Billboard trackingRotation;
    private Float shadowStrength;
    private Float shadowRadius;
    private Integer interpolationDuration;
    private Integer interpolationDelay;
    private Float width;
    private Float height;
    private final boolean isInteractable;


    public DisplayEntityProperties(ConfigurationSection configSection) {
        isInteractable = configSection.getBoolean("interactable", true);
        glowColor = configSection.getColor("glow_color");
        if (glowColor == null && !configSection.getString("glow_color", "").isEmpty())
            Utils.toColor(configSection.getString("glow_color", ""));
        viewRange = configSection.getInt("view_range");
        interpolationDuration = configSection.getInt("interpolation_duration");
        interpolationDelay = configSection.getInt("interpolation_delay");
        shadowStrength = (float) configSection.getDouble("shadow_strength");
        shadowRadius = (float) configSection.getDouble("shadow_radius");
        width = (float) configSection.getDouble("width");
        height = (float) configSection.getDouble("height");

        if (viewRange == 0) viewRange = null;
        if (interpolationDuration == 0) interpolationDuration = null;
        if (interpolationDelay == 0) interpolationDelay = null;
        if (shadowStrength == 0f) shadowStrength = null;
        if (shadowRadius == 0f) shadowRadius = null;
        if (width == 0f) width = null;
        if (height == 0f) height = null;

        try {
            transform = ItemDisplay.ItemDisplayTransform.valueOf(configSection.getString("display_transform", ItemDisplay.ItemDisplayTransform.NONE.name()));
        } catch (IllegalArgumentException e) {
            Logs.logError("Use of illegal ItemDisplayTransform in " + configSection.getRoot().getName() + " furniture.");
            Logs.logError("Allowed ones are: " + Arrays.stream(ItemDisplay.ItemDisplayTransform.values()).toList().stream().map(Enum::name));
            Logs.logWarning("Set transform to NONE for " + configSection.getRoot().getName());
            transform = ItemDisplay.ItemDisplayTransform.NONE;
        }

        try {
            trackingRotation = Display.Billboard.valueOf(configSection.getString("tracking_rotation", Display.Billboard.FIXED.name()));
        } catch (IllegalArgumentException e) {
            Logs.logError("Use of illegal tracking-rotation in " + configSection.getRoot().getName() + " furniture.");
            Logs.logError("Allowed ones are: " + Arrays.stream(ItemDisplay.ItemDisplayTransform.values()).toList().stream().map(Enum::name));
            Logs.logWarning("Set tracking-rotation to FIXED for " + configSection.getRoot().getName());
            trackingRotation = Display.Billboard.FIXED;
        }

        ConfigurationSection brightnessSection = configSection.getConfigurationSection("brightness");
        if (brightnessSection != null)
            brightness = new Display.Brightness(brightnessSection.getInt("block_light", 0), brightnessSection.getInt("sky_light", 0));
        else brightness = null;

    }

    public boolean hasGlowColor() { return glowColor != null; }
    public Color getGlowColor() { return glowColor; }
    public boolean hasSpecifiedViewRange() { return viewRange != null; }
    public int getViewRange() { return viewRange; }
    public boolean hasInterpolationDuration() { return interpolationDuration != null; }
    public int getInterpolationDuration() { return interpolationDuration; }
    public boolean hasInterpolationDelay() { return interpolationDelay != null; }
    public int getInterpolationDelay() { return interpolationDelay; }
    public boolean hasBrightness() { return brightness != null; }
    public Display.Brightness getBrightness() { return brightness; }
    public ItemDisplay.ItemDisplayTransform getTransform() { return transform; }
    public boolean hasTrackingRotation() { return trackingRotation != null; }
    public Display.Billboard getTrackingRotation() { return trackingRotation; }
    public boolean hasShadowStrength() { return shadowStrength != null; }
    public float getShadowStrength() { return shadowStrength; }
    public boolean hasShadowRadius() { return shadowRadius != null; }
    public float getShadowRadius() { return shadowRadius; }
    public boolean hasWidth() { return width != null; }
    public float getWidth() { return width; }
    public boolean hasHeight() { return height != null; }
    public float getHeight() { return height; }
    public boolean isInteractable() { return isInteractable; }
}
