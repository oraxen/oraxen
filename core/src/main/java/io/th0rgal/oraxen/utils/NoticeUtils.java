package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;

public class NoticeUtils {

    public static void compileNotice() {
        Logs.logError("This is a compiled version of Oraxen.");
        Logs.logWarning("Compiled versions come without Default assets and support is not provided.");
        Logs.logWarning("Consider purchasing Oraxen on SpigotMC or Polymart for access to the full version.");
    }

    public static void leakNotice() {
        Logs.logError("This is a leaked version of Oraxen");
        Logs.logError("Piracy is not supported, shutting down plugin.");
        Logs.logError("Consider purchasing Oraxen on SpigotMC or Polymart if you want a working version.");
        Bukkit.getPluginManager().disablePlugin(OraxenPlugin.get());
    }

    public static void foliaNotice() {
        Logs.logError("Oraxen has detected that you are using Folia.");
        Logs.logWarning("Whilst Oraxen mostly supports it, there is bound to be bugs and other unforseen issues");
        Logs.logWarning("It is not recommended to run this on a production server");
        Logs.logInfo("Gestures & Evolution-Mechanic are entirely disabled on Folia Servers for the time being");
    }
}
