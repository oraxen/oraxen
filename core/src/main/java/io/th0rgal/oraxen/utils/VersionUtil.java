package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VersionUtil {
    private static final Map<NMSVersion, Map<Integer, MinecraftVersion>> versionMap = new HashMap<>();
    private static final boolean IS_PAPER;
    private static final boolean IS_FOLIA;

    public enum NMSVersion {
        v1_21_R5,
        v1_21_R4,
        v1_21_R3,
        v1_21_R2,
        v1_21_R1,
        v1_20_R4,
        v1_20_R3,
        v1_20_R2,
        v1_20_R1,
        v1_19_R3,
        v1_19_R2,
        v1_19_R1,
        v1_18_R2,
        v1_18_R1,
        UNKNOWN;

        public static boolean matchesServer(NMSVersion version) {
            return version != UNKNOWN && getNMSVersion(MinecraftVersion.getCurrentVersion()).equals(version);
        }
    }

    static {
        IS_PAPER = hasClass("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent");
        IS_FOLIA = hasClass("io.papermc.paper.threadedregions.RegionizedServer");
        versionMap.put(NMSVersion.v1_21_R5,
                Map.of(22, new MinecraftVersion("1.21.7"),23, new MinecraftVersion("1.21.8")));
        versionMap.put(NMSVersion.v1_21_R4,
                Map.of(20, new MinecraftVersion("1.21.5")));
        versionMap.put(NMSVersion.v1_21_R3,
                Map.of(19, new MinecraftVersion("1.21.4")));
        versionMap.put(NMSVersion.v1_21_R2,
                Map.of(17, new MinecraftVersion("1.21.2"), 18, new MinecraftVersion("1.21.3")));
        versionMap.put(NMSVersion.v1_21_R1,
                Map.of(15, new MinecraftVersion("1.21"), 16, new MinecraftVersion("1.21.1")));
        versionMap.put(NMSVersion.v1_20_R4,
                Map.of(13, new MinecraftVersion("1.20.5"), 14, new MinecraftVersion("1.20.6")));
        versionMap.put(NMSVersion.v1_20_R3,
                Map.of(11, new MinecraftVersion("1.20.3"), 12, new MinecraftVersion("1.20.4")));
        versionMap.put(NMSVersion.v1_20_R2, Map.of(10, new MinecraftVersion("1.20.2")));
        versionMap.put(NMSVersion.v1_20_R1, Map.of(8, new MinecraftVersion("1.20"), 9, new MinecraftVersion("1.20.1")));
        versionMap.put(NMSVersion.v1_19_R3, Map.of(7, new MinecraftVersion("1.19.4")));
        versionMap.put(NMSVersion.v1_19_R2, Map.of(6, new MinecraftVersion("1.19.3")));
        versionMap.put(NMSVersion.v1_19_R1, Map.of(3, new MinecraftVersion("1.19"), 4, new MinecraftVersion("1.19.1"),
                5, new MinecraftVersion("1.19.2")));
        versionMap.put(NMSVersion.v1_18_R2, Map.of(2, new MinecraftVersion("1.18.2")));
        versionMap.put(NMSVersion.v1_18_R1, Map.of(0, new MinecraftVersion("1.18"), 1, new MinecraftVersion("1.18.1")));
    }

    public static NMSVersion getNMSVersion(MinecraftVersion version) {
        return versionMap.entrySet().stream().filter(e -> e.getValue().containsValue(version)).map(Map.Entry::getKey)
                .findFirst().orElse(NMSVersion.UNKNOWN);
    }

    public static boolean matchesServer(String server) {
        return MinecraftVersion.getCurrentVersion().equals(new MinecraftVersion(server));
    }

    public static boolean atOrAbove(String versionString) {
        return new MinecraftVersion(versionString).atOrAbove();
    }

    /**
     * @return true if the server is Paper or false of not
     * @throws IllegalArgumentException if server is null
     */
    public static boolean isPaperServer() {
        return IS_PAPER;
    }

    public static boolean isFoliaServer() {
        return IS_FOLIA;
    }

    public static boolean isSupportedVersion(@NotNull NMSVersion serverVersion,
            @NotNull NMSVersion... supportedVersions) {
        for (NMSVersion version : supportedVersions) {
            if (version.equals(serverVersion))
                return true;
        }

        Logs.logWarning(
                String.format("The Server version which you are running is unsupported, you are running version '%s'.",
                        serverVersion));
        Logs.logWarning(
                String.format("The plugin supports following versions %s.", combineVersions(supportedVersions)));

        if (serverVersion == NMSVersion.UNKNOWN) {
            Logs.logWarning(String.format(
                    "The Version '%s' can indicate, that you are using a newer Minecraft version than currently supported.",
                    serverVersion));
            Logs.logWarning(
                    "In this case please update to the newest version of this plugin. If this is the newest Version, than please be patient. It can take a few weeks until the plugin is updated.");
        }

        Logs.logWarning("No compatible Server version found!");

        return false;
    }

    @NotNull
    private static String combineVersions(@NotNull NMSVersion... versions) {
        StringBuilder stringBuilder = new StringBuilder();

        boolean first = true;

        for (NMSVersion version : versions) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(" ");
            }

            stringBuilder.append("'");
            stringBuilder.append(version);
            stringBuilder.append("'");
        }

        return stringBuilder.toString();
    }

    private final static String manifest = JarReader.getManifestContent();

    public static boolean isCompiled() {
        List<String> split = Arrays.stream(manifest.split(":|\n")).map(String::trim).toList();
        return Boolean.parseBoolean(split.get(split.indexOf("Compiled") + 1)) && !isValidCompiler();
    }

    private static final boolean leaked = JarReader.checkIsLeaked();

    public static boolean isLeaked() {
        return leaked;
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
