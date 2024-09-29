package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.EnumUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VectorUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class FurnitureProperties {
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
    private Vector3f translation;
    private Quaternionf leftRotation;
    private Quaternionf rightRotation;

    public FurnitureProperties(@Nullable ConfigurationSection configSection) {
        this();
        if (configSection == null) return;
        String itemID = configSection.getParent().getParent().getParent().getName();
        glowColor = Utils.toColor(configSection.getString("glow_color", ""));
        viewRange = configSection.getInt("view_range");
        shadowStrength = (float) configSection.getDouble("shadow_strength");
        shadowRadius = (float) configSection.getDouble("shadow_radius");
        displayWidth = (float) configSection.getDouble("display_width", 0);
        displayHeight = (float) configSection.getDouble("display_height", 0);

        if (viewRange == 0) viewRange = null;
        if (shadowStrength == 0f) shadowStrength = null;
        if (shadowRadius == 0f) shadowRadius = null;

        displayTransform = EnumUtils.getEnumOrElse(ItemDisplay.ItemDisplayTransform.class, configSection.getString("display_transform"), (transform) -> {
            if (transform != null) {
                Logs.logError("Use of illegal ItemDisplayTransform " + transform + " in furniture: <gold>" + itemID);
                Logs.logWarning("Allowed ones are: <gold>" + Arrays.stream(ItemDisplay.ItemDisplayTransform.values()).map(Enum::name).toList());
                Logs.logWarning("Setting transform to NONE for furniture: <gold>" + itemID);
            }
            return ItemDisplay.ItemDisplayTransform.NONE;
        });

        boolean isFixed = displayTransform == ItemDisplay.ItemDisplayTransform.FIXED;
        Optional.ofNullable(configSection.getString("scale")).ifPresentOrElse(
                (string) -> scale = VectorUtils.getVector3fFromString(string, isFixed ? 0.5f : 1),
                () -> scale = isFixed ? new Vector3f(0.5f, 0.5f, 0.5f) : new Vector3f()
        );

        Optional.ofNullable(configSection.getString("translation")).ifPresentOrElse(
                (string) -> translation = VectorUtils.getVector3fFromString(string, 0),
                () -> translation = new Vector3f()
        );

        Optional.ofNullable(configSection.getString("left_rotation")).ifPresentOrElse(
                (string) -> leftRotation = VectorUtils.getQuaternionfFromString(string, 0),
                () -> leftRotation = new Quaternionf()
        );

        Optional.ofNullable(configSection.getString("right_rotation")).ifPresentOrElse(
                (string) -> rightRotation = VectorUtils.getQuaternionfFromString(string, 0),
                () -> rightRotation = new Quaternionf()
        );

        trackingRotation = EnumUtils.getEnumOrElse(Display.Billboard.class, configSection.getString("tracking_rotation"), (billboard) -> {
            if (configSection.getString("tracking_rotation") != null) {
                Logs.logError("Use of illegal tracking-rotation " + billboard + " in " + itemID + " furniture.");
                Logs.logError("Allowed ones are: " + Arrays.stream(ItemDisplay.ItemDisplayTransform.values()).map(Enum::name).toList());
                Logs.logWarning("Set tracking-rotation to FIXED for " + itemID);
            }
            return Display.Billboard.FIXED;
        });

        ConfigurationSection brightnessSection = configSection.getConfigurationSection("brightness");
        if (brightnessSection != null)
            brightness = new Display.Brightness(brightnessSection.getInt("block_light", 0), brightnessSection.getInt("sky_light", 0));
        else brightness = null;

    }

    public FurnitureProperties() {
        this.displayWidth = 0f;
        this.displayHeight = 0f;
        this.displayTransform = ItemDisplay.ItemDisplayTransform.NONE;
        this.scale = new Vector3f();
        this.translation = new Vector3f();
        this.leftRotation = new Quaternionf();
        this.rightRotation = new Quaternionf();
        this.shadowRadius = null;
        this.shadowStrength = null;
        this.brightness = null;
        this.trackingRotation = null;
        this.viewRange = null;
        this.glowColor = null;
    }

    public Optional<Color> glowColor() { return Optional.ofNullable(glowColor); }

    public Optional<Integer> viewRange() {
        return Optional.ofNullable(viewRange);
    }


    public Optional<Display.Brightness> brightness() {
        return Optional.ofNullable(brightness);
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


    public Optional<Display.Billboard> trackingRotation() {
        return Optional.ofNullable(trackingRotation);
    }

    public Optional<Float> shadowStrength() {
        return Optional.ofNullable(shadowStrength);
    }

    public Optional<Float> shadowRadius() {
        return Optional.ofNullable(shadowRadius);
    }

    public Float displayWidth() {
        return displayWidth;
    }

    public Float displayHeight() {
        return displayHeight;
    }

    public Vector3f scale() {
        return scale;
    }

    public Vector3f translation() {
        return translation;
    }

    public Quaternionf leftRotation() {
        return leftRotation;
    }

    public Quaternionf rightRotation() {
        return rightRotation;
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
        itemDisplay.getTransformation().getTranslation().set(Objects.requireNonNullElse(translation, new Vector3f()));

        return true;
    }
}
