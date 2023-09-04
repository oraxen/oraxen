package io.th0rgal.oraxen.nms;


import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Player;

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
            Logs.logSuccess("Version " + packageVersion + " has been detected.");
            Logs.logSuccess("Oraxen will use the Glyph-NMSHandler for this version.");
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

    public static String returnFormattedString(JsonObject obj, Player player) {
        return (obj.has("args") || obj.has("text") || obj.has("extra") || obj.has("translate")) ? Glyph.parseGlyphPlaceholders(player, AdventureUtils.parseJsonThroughMiniMessage(obj.toString())) : obj.toString();
    }
}