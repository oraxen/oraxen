package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Objects;

public class DisplayEntityProperties {
    //private final Color glowColor;
    private Integer viewRange;
    private final Display.Brightness brightness;
    private ItemDisplay.ItemDisplayTransform displayTransform;
    private Display.Billboard trackingRotation;
    private Float shadowStrength;
    private Float shadowRadius;
    private Integer interpolationDuration;
    private Integer interpolationDelay;
    private final float displayWidth;
    private final float displayHeight;
    private final Vector3f scale;

    public DisplayEntityProperties() {
        this.displayWidth = 0f;
        this.displayHeight = 0f;
        this.displayTransform = ItemDisplay.ItemDisplayTransform.NONE;
        this.scale = null;
        this.shadowRadius = null;
        this.shadowStrength = null;
        this.brightness = null;
        this.trackingRotation = null;
        this.viewRange = null;
    }

    public DisplayEntityProperties(ConfigurationSection configSection) {
        String itemID = configSection.getParent().getParent().getParent().getName();
        //glowColor = Utils.toColor(configSection.getString("glow_color", ""));
        viewRange = configSection.getInt("view_range");
        interpolationDuration = configSection.getInt("interpolation_duration");
        interpolationDelay = configSection.getInt("interpolation_delay");
        shadowStrength = (float) configSection.getDouble("shadow_strength");
        shadowRadius = (float) configSection.getDouble("shadow_radius");
        displayWidth = (float) configSection.getDouble("displayWidth", 0);
        displayHeight = (float) configSection.getDouble("displayHeight", 0);
        if (configSection.isConfigurationSection("scale"))
            scale = new Vector3f((float) configSection.getDouble("scale.x", 1.0),
                    (float) configSection.getDouble("scale.y", 1.0),
                    (float) configSection.getDouble("scale.z", 1.0));
        else scale = null;

        if (viewRange == 0) viewRange = null;
        if (interpolationDuration == 0) interpolationDuration = null;
        if (interpolationDelay == 0) interpolationDelay = null;
        if (shadowStrength == 0f) shadowStrength = null;
        if (shadowRadius == 0f) shadowRadius = null;

        try {
            displayTransform = ItemDisplay.ItemDisplayTransform.valueOf(configSection.getString("display_transform", ItemDisplay.ItemDisplayTransform.NONE.name()));
        } catch (IllegalArgumentException e) {
            Logs.logError("Use of illegal ItemDisplayTransform in furniture: <gold>" + itemID);
            Logs.logWarning("Allowed ones are: <gold>" + Arrays.stream(ItemDisplay.ItemDisplayTransform.values()).map(Enum::name).toList());
            Logs.logWarning("Setting transform to NONE for furniture: <gold>" + itemID);
            displayTransform = ItemDisplay.ItemDisplayTransform.NONE;
        }

        try {
            trackingRotation = Display.Billboard.valueOf(configSection.getString("tracking_rotation", Display.Billboard.FIXED.name()));
        } catch (IllegalArgumentException e) {
            Logs.logError("Use of illegal tracking-rotation in " + itemID + " furniture.");
            Logs.logError("Allowed ones are: " + Arrays.stream(ItemDisplay.ItemDisplayTransform.values()).toList().stream().map(Enum::name));
            Logs.logWarning("Set tracking-rotation to FIXED for " + itemID);
            trackingRotation = Display.Billboard.FIXED;
        }

        ConfigurationSection brightnessSection = configSection.getConfigurationSection("brightness");
        if (brightnessSection != null)
            brightness = new Display.Brightness(brightnessSection.getInt("block_light", 0), brightnessSection.getInt("sky_light", 0));
        else brightness = null;

    }

    //public boolean hasGlowColor() { return glowColor != null; }
    //public Color getGlowColor() { return glowColor; }
    public boolean hasSpecifiedViewRange() {
        return viewRange != null;
    }

    public int getViewRange() {
        return viewRange;
    }

    public boolean hasInterpolationDuration() {
        return interpolationDuration != null;
    }

    public int getInterpolationDuration() {
        return interpolationDuration;
    }

    public boolean hasInterpolationDelay() {
        return interpolationDelay != null;
    }

    public int getInterpolationDelay() {
        return interpolationDelay;
    }

    public boolean hasBrightness() {
        return brightness != null;
    }

    public Display.Brightness getBrightness() {
        return brightness;
    }

    public ItemDisplay.ItemDisplayTransform getDisplayTransform() {
        return displayTransform;
    }

    public boolean hasTrackingRotation() {
        return trackingRotation != null;
    }

    public Display.Billboard getTrackingRotation() {
        return trackingRotation;
    }

    public boolean hasShadowStrength() {
        return shadowStrength != null;
    }

    public float getShadowStrength() {
        return shadowStrength;
    }

    public boolean hasShadowRadius() {
        return shadowRadius != null;
    }

    public float getShadowRadius() {
        return shadowRadius;
    }

    public float getDisplayWidth() {
        return displayWidth;
    }

    public float getDisplayHeight() {
        return displayHeight;
    }

    public boolean hasScale() {
        return scale != null;
    }

    public Vector3f getScale() {
        return scale;
    }

    public boolean ensureSameDisplayProperties(@NotNull Entity entity) {
        if (!(entity instanceof ItemDisplay itemDisplay)) return false;
        itemDisplay.setItemDisplayTransform(displayTransform);
        itemDisplay.setBillboard(Objects.requireNonNullElse(trackingRotation, Display.Billboard.FIXED));
        itemDisplay.setBrightness(Objects.requireNonNullElse(brightness, new Display.Brightness(0,0)));
        itemDisplay.setShadowRadius(Objects.requireNonNullElse(shadowRadius, 0f));
        itemDisplay.setShadowStrength(Objects.requireNonNullElse(shadowStrength, 0f));
        itemDisplay.setViewRange(Objects.requireNonNullElse(viewRange, 0));
        itemDisplay.setInterpolationDuration(Objects.requireNonNullElse(interpolationDuration, 0));
        itemDisplay.setInterpolationDelay(Objects.requireNonNullElse(interpolationDelay, 0));
        itemDisplay.getTransformation().getScale().set(Objects.requireNonNullElse(scale, new Vector3f(1,1,1)));

        return true;
    }
}
