package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;

public enum CustomArmorType {
    NONE, SHADER, TRIMS;

    public static CustomArmorType getSetting() {
        return fromString(Settings.CUSTOM_ARMOR_TYPE.toString());
    }

    public static CustomArmorType fromString(String type) {
        try {
            CustomArmorType customArmorType = CustomArmorType.valueOf(type.toUpperCase());
            if (!VersionUtil.atOrAbove("1.20")) {
                Logs.logError("Trim based custom armor is only supported in 1.20 and above.");
                throw new IllegalArgumentException();
            }
            return customArmorType;
        } catch (IllegalArgumentException e) {
            Logs.logError("Invalid custom armor type: " + type);
            Logs.logError("Defaulting to NONE.");
            return NONE;
        }
    }
}
