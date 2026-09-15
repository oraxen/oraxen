package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;

public enum CustomArmorType {
    NONE, TRIMS, COMPONENT;

    public static CustomArmorType getSetting() {
        return fromString(Settings.CUSTOM_ARMOR_TYPE.toString());
    }

    public static CustomArmorType fromString(String type) {
        try {
            CustomArmorType customArmorType = CustomArmorType.valueOf(type.toUpperCase());
            if (!Settings.CUSTOM_ARMOR_AUTO_SELECT_TYPE.toBool())
                return customArmorType;

            if (!VersionUtil.atOrAbove("1.21.2") && customArmorType == COMPONENT) {
                Logs.logError("Component based custom armor is only supported in 1.21.2 and above.");
                throw new IllegalArgumentException();
            }
            return customArmorType;
        } catch (IllegalArgumentException e) {
            CustomArmorType defaultType = getBestForCurrentVersion();
            Logs.logError("Invalid or incompatible custom armor type - " + type);
            Logs.logError("Defaulting to %s based on your Minecraft version.".formatted(defaultType));

            // Persist the corrected value back to settings.yml so future loads use the compatible type
            Settings.CUSTOM_ARMOR_TYPE.setValue(defaultType.name());
            return defaultType;
        }
    }

    /**
     * Determines the most suitable CustomArmorType for the running server version.
     *
     * Components: 1.21.2+
     * Trims:      1.20 - 1.21.1
     * None:       when custom armor is not supported
     */
    private static CustomArmorType getBestForCurrentVersion() {
        if (VersionUtil.atOrAbove("1.21.2")) {
            return COMPONENT;
        } else {
            return TRIMS;
        }
    }
}
