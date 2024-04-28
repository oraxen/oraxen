package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.util.Arrays;

public enum CustomArmorType {
    NONE, SHADER, TRIMS;

    public static CustomArmorType getSetting() {
        return Arrays.stream(CustomArmorType.values())
                .filter(e -> e.name().equals(Settings.CUSTOM_ARMOR_TYPE.toString())).findFirst().orElseGet(() -> {
                    Logs.logError("Invalid custom armor type: " + Settings.CUSTOM_ARMOR_TYPE);
                    Logs.logError("Defaulting to NONE.");
                    return NONE;
                });
    }
}
