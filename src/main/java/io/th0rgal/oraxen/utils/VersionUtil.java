package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;

import java.util.*;

public class VersionUtil {
    private static final boolean isPaper;
    private static final boolean isFolia;

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

    static boolean isModernVersionNamespace(MinecraftVersion version) {
        // Mojang switched the release version namespace after 1.21.11 (e.g. "26.1.2").
        // Also accept the "1.26.x" spelling some sources use for the same releases.
        return version.getMajor() >= 26 || (version.getMajor() == 1 && version.getMinor() >= 26);
    }

    public static boolean supportsSingleNmsHandler() {
        // The NMS handler requires APIs introduced in 1.21.2. Loading an incompatible
        // handler on an unknown newer version fails safe via the LinkageError fallback
        // in NMSHandlers#setup.
        return supportsSingleNmsHandler(MinecraftVersion.getCurrentVersion());
    }

    static boolean supportsSingleNmsHandler(MinecraftVersion version) {
        return version.isAtLeast(new MinecraftVersion("1.21.2"));
    }

    public static String supportedVersions() {
        return "Paper and Paper forks 1.21.2+ / 26.x through the guarded NMS handler";
    }

    public static boolean atOrAbove(String versionString) {
        return new MinecraftVersion(versionString).atOrAbove();
    }

    /**
     * Scoreboard/tablist background hiding tweaks are only supported from 1.21.6 through 1.21.7.
     * Scoreboard number hiding is unaffected: it is handled per version by
     * TextShaderGenerator#hideScoreboardNumbers.
     */
    public static boolean supportsScoreboardBackgroundHiding() {
        return supportsScoreboardBackgroundHiding(MinecraftVersion.getCurrentVersion());
    }

    static boolean supportsScoreboardBackgroundHiding(MinecraftVersion version) {
        return version.isAtLeast(new MinecraftVersion("1.21.6"))
                && !version.isAtLeast(new MinecraftVersion("1.21.8"));
    }

    /**
     * @return true if the server is Paper or false of not
     * @throws IllegalArgumentException if server is null
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

    private final static String manifest = JarReader.getManifestContent();

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
