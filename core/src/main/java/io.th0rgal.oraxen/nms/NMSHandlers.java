package io.th0rgal.oraxen.nms;


import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.lang.reflect.InvocationTargetException;

public class NMSHandlers {

    private static final VersionUtil[] SUPPORTED_VERSION = VersionUtil.values();
    private static NMSHandler handler;
    private static String version;

    public static NMSHandler getHandler() {
        if (handler != null) {
            return handler;
        } else {
            setup();
        }
        return handler;
    }

    public static String getVersion() {
        return version;
    }

    public static void setup() {
        if (handler != null) return;
        final String packageName = OraxenPlugin.get().getServer().getClass().getPackage().getName();
        String packageVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

        for (VersionUtil selectedVersion : SUPPORTED_VERSION) {
            if (!selectedVersion.toString().contains(packageVersion)) {
                continue;
            }
            Logs.debug(packageVersion + " has been detected.");
            version = packageVersion;
            try {
                handler = (NMSHandler) Class.forName("io.th0rgal.oraxen.nms." + packageVersion + ".NMSHandler").getConstructor().newInstance();
                return;
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
