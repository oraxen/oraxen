package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.utils.ParseUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Objects;

public class DisplayEntityProperties {
    private Color glowColor;
    private Integer viewRange;
    private Display.Brightness brightness;
    private ItemDisplay.ItemDisplayTransform displayTransform;
    private Display.Billboard trackingRotation;
    private Float shadowStrength;
    private Float shadowRadius;
    private float displayWidth;
    private float displayHeight;
    private Vector3f scale;

    public DisplayEntityProperties(@Nullable ConfigurationSection configSection) {
        this();
        if (configSection == null) return;
        String itemID = configSection.getParent().getParent().getParent().getName();
        glowColor = Utils.toColor(configSection.getString("glow_color", ""));
        viewRange = configSection.getInt("view_range");
        shadowStrength = (float) configSection.getDouble("shadow_strength");
        shadowRadius = (float) configSection.getDouble("shadow_radius");
        displayWidth = (float) configSection.getDouble("displayWidth", 0);
        displayHeight = (float) configSection.getDouble("displayHeight", 0);

        if (viewRange == 0) viewRange = null;
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

        boolean isFixed = displayTransform == ItemDisplay.ItemDisplayTransform.FIXED;
        if (configSection.isConfigurationSection("scale")) {
            scale = new Vector3f((float) configSection.getDouble("scale.x", isFixed ? 0.5 : 1.0),
                    (float) configSection.getDouble("scale.y", isFixed ? 0.5 : 1.0),
                    (float) configSection.getDouble("scale.z", isFixed ? 0.5 : 1.0));
        } else if (configSection.isString("scale")) {
            float[] vector = new float[3];
            String[] parts = configSection.getString("scale", "").replace(" ", "").split(",");
            while (parts.length < 3) parts[parts.length - 1] = isFixed ? "0.5": "1";

            for (int i = 0; i < parts.length && i < 3; i++)
                vector[i] = ParseUtils.parseFloat(parts[i].trim(), isFixed ? 0.5f : 1);

            scale = new Vector3f(vector[0], vector[1], vector[2]);
        } else scale = isFixed ? new Vector3f(0.5f,0.5f,0.5f) : null;

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
        this.glowColor = null;
    }

    public boolean hasGlowColor() { return glowColor != null; }
    public Color glowColor() { return glowColor; }
    public boolean hasSpecifiedViewRange() {
        return viewRange != null;
    }

    public Integer viewRange() {
        return viewRange;
    }

    public boolean hasBrightness() {
        return brightness != null;
    }

    public Display.Brightness brightness() {
        return brightness;
    }

    public ItemDisplay.ItemDisplayTransform displayTransform() {
        return displayTransform;
    }

    public boolean isFixedTransform() {
        return displayTransform == ItemDisplay.ItemDisplayTransform.FIXED;
    }

    public boolean isNoneTransform() {
        return displayTransform == ItemDisplay.ItemDisplayTransform.NONE;
    }

    public boolean hasTrackingRotation() {
        return trackingRotation != null;
    }

    public Display.Billboard trackingRotation() {
        return trackingRotation;
    }

    public Float shadowStrength() {
        return shadowStrength;
    }

    public Float shadowRadius() {
        return shadowRadius;
    }

    public Float displayWidth() {
        return displayWidth;
    }

    public Float displayHeight() {
        return displayHeight;
    }

    public boolean hasScale() {
        return scale != null;
    }

    public Vector3f scale() {
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
        itemDisplay.getTransformation().getScale().set(Objects.requireNonNullElse(scale, new Vector3f(1,1,1)));

        return true;
    }
}
