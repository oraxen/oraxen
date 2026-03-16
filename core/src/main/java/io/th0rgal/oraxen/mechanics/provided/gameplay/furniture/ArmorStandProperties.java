package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
/**
 * Extra properties for ARMOR_STAND based furniture.
 *
 * Note: ArmorStands do not support arbitrary per-entity scaling like Display entities.
 * The optional scale here is only used as a hint to toggle the ArmorStand's "small" flag.
 */
public class ArmorStandProperties {
    private final Vector translation;
    private final Float scaleY;
    public ArmorStandProperties() {
        this.translation = null;
        this.scaleY = null;
    }
    public ArmorStandProperties(@Nullable ConfigurationSection configSection) {
        if (configSection == null) {
            this.translation = null;
            this.scaleY = null;
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
        // Accept either scalar scale (scale: 0.5) or vector scale (scale.y).
        if (configSection.isDouble("scale") || configSection.isInt("scale")) {
            this.scaleY = (float) configSection.getDouble("scale");
        } else if (configSection.isConfigurationSection("scale")) {
            this.scaleY = (float) configSection.getDouble("scale.y", 1.0);
        } else {
            this.scaleY = null;
        }
    }
    public boolean hasTranslation() {
        return translation != null;
    }
    public Vector getTranslation() {
        return translation;
    }
    public boolean hasScale() {
        return scaleY != null;
    }
    public float getScaleY() {
        return scaleY;
    }
    /**
     * Best-effort mapping of a continuous scale hint to ArmorStand's discrete size.
     */
    public boolean hintSmallFromScale() {
        if (scaleY == null) return false;
        return scaleY < 0.75f;
    }
}
