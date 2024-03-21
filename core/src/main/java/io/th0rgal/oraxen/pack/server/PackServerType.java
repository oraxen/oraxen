package io.th0rgal.oraxen.pack.server;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum PackServerType {
    CREATIVE, POLYMATH, NONE;

    public static PackServerType fromSetting() {
        String typeString = Settings.PACK_SERVER_TYPE.toString();
        try {
            PackServerType type = PackServerType.valueOf(typeString);
            Logs.logInfo("PackServer set to " + typeString);
            return type;
        } catch (IllegalArgumentException e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            else Logs.logWarning(e.getMessage());

            Logs.logError("Invalid PackServer-type specified: " + typeString);
            Logs.logError("Valid types are: " + Arrays.stream(PackServerType.values()).map(Enum::name).collect(Collectors.joining()));
            Logs.logError("Server is temporarily set to NONE, please fix the setting!");
            return NONE;
        }
    }
}
