package io.th0rgal.oraxen.nms;


import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

public class NMSHandlers {

    private static final VersionUtil.NMSVersion[] SUPPORTED_VERSION = VersionUtil.NMSVersion.values();
    private static NMSHandler handler;
    private static String version;

    @Nullable
    public static NMSHandler getHandler() {
        if (handler != null) return handler;
        else setup();
        return handler;
    }

    public static String getVersion() {
        return version;
    }

    public static void setup() {
        if (handler != null) return;

        for (VersionUtil.NMSVersion selectedVersion : SUPPORTED_VERSION) {
            if (!VersionUtil.NMSVersion.matchesServer(selectedVersion)) continue;

            version = selectedVersion.name();
            try {
                handler = (NMSHandler) Class.forName("io.th0rgal.oraxen.nms." + version + ".NMSHandler").getConstructor().newInstance();
                Logs.logSuccess("Version " + version + " has been detected.");
                Logs.logInfo("Oraxen will use the NMSHandler for this version.");
                Bukkit.getPluginManager().registerEvents(new NMSListeners(), OraxenPlugin.get());
                return;
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException e) {
                Logs.logWarning("Oraxen does not support this version of Minecraft (" + version + ") yet.");
                Logs.logWarning("NMS features will be disabled...");
                if (Settings.DEBUG.toBool()) e.printStackTrace();
            }
        }
    }
    public static boolean isTripwireUpdatesDisabled() {
        return handler != null && handler.tripwireUpdatesDisabled();
    }

    public static boolean isNoteblockUpdatesDisabled() {
        return handler != null && handler.noteblockUpdatesDisabled();
    }
}
