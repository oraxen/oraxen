package io.th0rgal.oraxen.nms;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class NMSHandlers {

    private static NMSHandler handler;
    private static String version;
    private static boolean packDispatchListenerRegistered;

    @NotNull
    public static NMSHandler getHandler() {
        if (handler != null)
            return handler;
        else {
            setup();
            if (handler instanceof NMSHandler.EmptyNMSHandler && Settings.DEBUG.toBool()) {
                Logs.logError("Failed to setup NMS handler for server version: " + Bukkit.getVersion());
                Logs.logError("Supported versions: " + VersionUtil.supportedVersions());
                Logs.logError("Current server implements Paper: " + VersionUtil.isPaperServer());
                Logs.logError("NMS features will be disabled or limited.");
            }
        }
        return handler;
    }

    public static String getVersion() {
        return version;
    }

    public static void setup() {
        if (handler != null)
            return;

        version = Bukkit.getMinecraftVersion();
        if (!VersionUtil.supportsSingleNmsHandler()) {
            Logs.logWarning("Oraxen does not have a Paper-compatible NMS handler for this server version (" + version + ").");
            Logs.logWarning("NMS features will be disabled or limited...");
            handler = new NMSHandler.EmptyNMSHandler();
            return;
        }

        try {
            String handlerClass = VersionUtil.isModernVersionNamespace()
                    ? "io.th0rgal.oraxen.nms.handler.java25.NMSHandler"
                    : "io.th0rgal.oraxen.nms.handler.java21.NMSHandler";
            handler = (NMSHandler) Class.forName(handlerClass)
                    .getConstructor().newInstance();
            if (Settings.DEBUG.toBool()) {
                Logs.logSuccess("Version " + version + " has been detected.");
                Logs.logInfo("Oraxen will use " + handlerClass + ".");
            }
            Bukkit.getPluginManager().registerEvents(new NMSListeners(), OraxenPlugin.get());
            Listener packDispatchListener = handler.packDispatchListener();
            if (packDispatchListener != null) {
                Bukkit.getPluginManager().registerEvents(packDispatchListener, OraxenPlugin.get());
                packDispatchListenerRegistered = true;
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            Logs.logWarning("Failed to load guarded NMS handler; NMS features will be disabled...");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            handler = new NMSHandler.EmptyNMSHandler();
        }
    }

    public static boolean hasPackDispatchListener() {
        return packDispatchListenerRegistered;
    }

    public static boolean isTripwireUpdatesDisabled() {
        return handler != null && handler.tripwireUpdatesDisabled();
    }

    public static boolean isNoteblockUpdatesDisabled() {
        return handler != null && handler.noteblockUpdatesDisabled();
    }

    public static boolean isChorusPlantUpdatesDisabled() {
        return handler != null && handler.chorusPlantUpdatesDisabled();
    }
}
