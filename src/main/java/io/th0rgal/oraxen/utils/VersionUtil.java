package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VersionUtil {
    private static final boolean isPaper;
    private static final boolean isFolia;
    private static final ConcurrentHashMap<String, Boolean> AT_OR_ABOVE_CACHE = new ConcurrentHashMap<>();

    static {
        isPaper = hasClass("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent");
        isFolia = hasClass("io.papermc.paper.threadedregions.RegionizedServer");
    }

    /**
     * Whether the server runs a release from Mojang's post-1.21.11 version namespace
     * (26.x and later). These versions are handled by the Java 25 NMS handler.
     */
    public static boolean isModernVersionNamespace() {
        return isModernVersionNamespace(MinecraftVersion.getCurrentVersion());
    }

    public static boolean isModernVersionNamespace(MinecraftVersion version) {
        return PlatformCapabilities.isModernVersionNamespace(version);
    }

    public static boolean supportsNmsHandler() {
        return supportsNmsHandler(MinecraftVersion.getCurrentVersion());
    }

    public static boolean supportsNmsHandler(MinecraftVersion version) {
        return PlatformCapabilities.supportsNmsHandler(version);
    }

    /**
     * 26.3 renamed packet accessors such as {@code tags()}, {@code hitResult()} and
     * {@code sequence()}. Pre-releases sort below the final version, but they already
     * expose those accessors, so the check uses the numeric version only.
     */
    public static boolean uses263PacketAccessors(MinecraftVersion version) {
        return PlatformCapabilities.uses263PacketAccessors(version);
    }

    public static boolean uses263PacketAccessors() {
        return uses263PacketAccessors(MinecraftVersion.getCurrentVersion());
    }

    public static String supportedVersions() {
        return "Paper and Paper forks 1.21.2+ / 26.x through the guarded NMS handler";
    }

    public static boolean atOrAbove(String versionString) {
        return AT_OR_ABOVE_CACHE.computeIfAbsent(versionString,
                version -> new MinecraftVersion(version).atOrAbove());
    }

    /**
     * Scoreboard/tablist background hiding tweaks are only supported from 1.21.6 through 1.21.7.
     * Scoreboard number hiding is unaffected: it is handled per version by
     * TextShaderGenerator#hideScoreboardNumbers.
     */
    public static boolean supportsScoreboardBackgroundHiding() {
        return supportsScoreboardBackgroundHiding(MinecraftVersion.getCurrentVersion());
    }

    public static boolean supportsScoreboardBackgroundHiding(MinecraftVersion version) {
        return PlatformCapabilities.supportsScoreboardBackgroundHiding(version);
    }

    /**
     * @return true if the server is Paper or false of not
     */
    public static boolean isPaperServer() {
        return isPaper;
    }

    /**
     * Checks whether the current server implementation is supported by Oraxen.
     * Paper forks (Purpur, DivineMC, etc.) expose Paper classes and are supported.
     * Unknown hybrid runtimes such as Arclight are not rejected by name; they may fail later
     * if they do not expose the Paper runtime API used by the plugin.
     */
    public static boolean isSupportedServer() {
        if (isPaper) return true;

        String serverName = Bukkit.getName().toLowerCase(Locale.ROOT);
        if (serverName.contains("spigot") || serverName.contains("craftbukkit")) {
            Logs.logWarning("Oraxen no longer supports Spigot/CraftBukkit.");
            Logs.logWarning("Please use Paper, Folia, or a Paper-compatible fork.");
            return false;
        }

        return true;
    }

    public static boolean isFoliaServer() {
        return isFolia;
    }

    private static final String manifest = JarReader.getManifestContent();

    public static boolean isCompiled() {
        List<String> split = Arrays.stream(manifest.split(":|\n")).map(String::trim).toList();
        return Boolean.parseBoolean(split.get(split.indexOf("Compiled") + 1)) && !isValidCompiler();
    }

    public static boolean isValidCompiler() {
        List<String> split = Arrays.stream(manifest.split(":|\n")).map(String::trim).toList();
        return Set.of("sivert", "thomas").contains(split.get(split.indexOf("Built-By") + 1).toLowerCase(Locale.ROOT));
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException var2) {
            return false;
        }
    }
}
