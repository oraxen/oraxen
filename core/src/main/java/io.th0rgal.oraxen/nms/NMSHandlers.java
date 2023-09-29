package io.th0rgal.oraxen.nms;


import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

public class NMSHandlers {

    private static final VersionUtil[] SUPPORTED_VERSION = VersionUtil.values();
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
        final String packageName = OraxenPlugin.get().getServer().getClass().getPackage().getName();
        String packageVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

        for (VersionUtil selectedVersion : SUPPORTED_VERSION) {
            if (!selectedVersion.toString().contains(packageVersion)) continue;

            version = packageVersion;
            try {
                handler = (NMSHandler) Class.forName("io.th0rgal.oraxen.nms." + packageVersion + ".NMSHandler").getConstructor().newInstance();
                Logs.logSuccess("Version " + packageVersion + " has been detected.");
                Logs.logInfo("Oraxen will use the NMSHandler for this version.");
                Bukkit.getPluginManager().registerEvents(new NMSListeners(), OraxenPlugin.get());
                return;
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException e) {
                Logs.logWarning("Oraxen does not support this version of Minecraft (" + packageVersion + ") yet.");
                Logs.logWarning("NMS features will be disabled...");
            }
        }
    }

    public static String formatJsonString(JsonObject obj) {
        return (obj.has("args") || obj.has("text") || obj.has("extra") || obj.has("translate"))
                ? Glyph.parsePlaceholders(obj.toString()) : obj.toString();
    }

    public static String verifyFor(Player player, String message) {
        if (message != null && player != null) for (Glyph glyph : OraxenPlugin.get().getFontManager().getGlyphs()) {
            String glyphTag = glyph.getGlyphTag();
            // Escape all glyphs the player does not have permission for
            if (!glyph.hasPermission(player)) {
                message = message.replace(glyphTag, "g" + glyphTag);
            }
        }
        return message;
    }
}
