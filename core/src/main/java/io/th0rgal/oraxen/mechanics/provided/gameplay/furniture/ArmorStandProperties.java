package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
/**
 * Extra properties for ARMOR_STAND based furniture.
 *
 * Note: ArmorStands do not support arbitrary per-entity scaling like Display entities.
 * The optional scale is used both as a small-stand hint and for generated model display.head transforms.
 */
public class ArmorStandProperties {
    private final Vector translation;
    private final Vector scale;

    public ArmorStandProperties() {
        this.translation = null;
        this.scale = null;
    }

    public ArmorStandProperties(@Nullable ConfigurationSection configSection) {
        if (configSection == null) {
            this.translation = null;
            this.scale = null;
            return;
        }
        // Accept both "translation" and "offset" as aliases.
        ConfigurationSection translationSection = configSection.getConfigurationSection("translation");
        if (translationSection == null) translationSection = configSection.getConfigurationSection("offset");
        if (translationSection != null) {
            this.translation = new Vector(
                    translationSection.getDouble("x", 0.0),
                    translationSection.getDouble("y", 0.0),
                    translationSection.getDouble("z", 0.0)
            );
        } else {
            this.translation = null;
        }
        // Accept either scalar scale (scale: 0.5) or vector scale ({x,y,z}).
        if (configSection.isDouble("scale") || configSection.isInt("scale")) {
            double uniformScale = configSection.getDouble("scale");
            this.scale = new Vector(uniformScale, uniformScale, uniformScale);
        } else if (configSection.isConfigurationSection("scale")) {
            this.scale = new Vector(
                    configSection.getDouble("scale.x", 1.0),
                    configSection.getDouble("scale.y", 1.0),
                    configSection.getDouble("scale.z", 1.0)
            );
        } else {
            this.scale = null;
        }
    }

    public boolean hasTranslation() {
        return translation != null;
    }

    @Nullable
    public Vector getTranslation() {
        return translation;
    }

    public boolean hasScale() {
        return scale != null;
    }

    @Nullable
    public Vector getScale() {
        return scale;
    }

    public float getScaleY() {
        return scale != null ? (float) scale.getY() : 1.0f;
    }

    /**
     * Best-effort mapping of a continuous scale hint to ArmorStand's discrete size.
     */
    public boolean hintSmallFromScale() {
        return hasScale() && getScaleY() < 0.75f;
    }
}
