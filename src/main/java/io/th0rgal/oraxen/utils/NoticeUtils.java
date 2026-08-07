package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;

public class NoticeUtils {

    public static void compileNotice() {
        Logs.logError("This is a compiled version of Oraxen.");
        Logs.logWarning("Compiled versions come without Default assets and support is not provided.");
        Logs.logWarning("Consider purchasing Oraxen on SpigotMC or Polymart for access to the full version.");
    }
}
