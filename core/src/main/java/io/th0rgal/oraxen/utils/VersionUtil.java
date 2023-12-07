package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VersionUtil {
    private static final Map<NMSVersion, Map<Integer, String>> versionMap = new HashMap<>();

    public enum NMSVersion {
        v1_20_R3,
        v1_20_R2,
        v1_20_R1,
        v1_19_R3,
        v1_19_R2,
        v1_19_R1,
        v1_18_R2,
        v1_18_R1,
        UNKNOWN;

        public static boolean matchesServer(String version) {
            return getNMSVersion(getMinecraftVersion()).equals(getNMSVersion(version));
        }
        public static boolean matchesServer(NMSVersion version) {
            return version != UNKNOWN && getNMSVersion(getMinecraftVersion()).equals(version);
        }
    }

    static {
        versionMap.put(NMSVersion.v1_20_R3, Map.of(11, "1.20.3", 12, "1.20.4"));
        versionMap.put(NMSVersion.v1_20_R2, Map.of(10, "1.20.2"));
        versionMap.put(NMSVersion.v1_20_R1, Map.of(8, "1.20", 9, "1.20.1"));
        versionMap.put(NMSVersion.v1_19_R3, Map.of(7, "1.19.4"));
        versionMap.put(NMSVersion.v1_19_R2, Map.of(6, "1.19.3"));
        versionMap.put(NMSVersion.v1_19_R1, Map.of(3, "1.19", 4, "1.19.1", 5, "1.19.2"));
        versionMap.put(NMSVersion.v1_18_R2, Map.of(2, "1.18.2"));
        versionMap.put(NMSVersion.v1_18_R1, Map.of(0, "1.18", 1, "1.18.1"));
    }

    public static NMSVersion getNMSVersion(String version) {
        return versionMap.entrySet().stream().filter(e -> e.getValue().containsValue(version)).map(Map.Entry::getKey).findFirst().orElse(NMSVersion.UNKNOWN);
    }

    private static int getVersionValue(String version) {
        for (Map.Entry<Integer, String> pair : versionMap.values().stream().flatMap(map -> map.entrySet().stream()).toList()) {
            if (pair.getValue().equals(version)) return pair.getKey();
        }
        return -1;
    }

    public static boolean matchesServer(String server) {
        return server.equals(getMinecraftVersion());
    }

    public static boolean isSupportedVersionOrNewer(String versionString) {
        int serverValue = getVersionValue(getMinecraftVersion());
        return getVersionValue(versionString) <= serverValue;
    }

    /**
     * @return true if the server is Paper or false of not
     * @throws IllegalArgumentException if server is null
     */
    public static boolean isPaperServer() {
        Server server = Bukkit.getServer();
        Validate.notNull(server, "Server cannot be null");
        if (server.getName().equalsIgnoreCase("Paper")) return true;

        try {
            Class.forName("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isFoliaServer() {
        Server server = Bukkit.getServer();
        Validate.notNull(server, "Server cannot be null");

        return server.getName().equalsIgnoreCase("Folia");
    }

    public static boolean isSupportedVersion(@NotNull NMSVersion serverVersion, @NotNull NMSVersion... supportedVersions) {
        for (NMSVersion version : supportedVersions) {
            if (version.equals(serverVersion)) return true;
        }

        Logs.logWarning(String.format("The Server version which you are running is unsupported, you are running version '%s'.", serverVersion));
        Logs.logWarning(String.format("The plugin supports following versions %s.", combineVersions(supportedVersions)));

        if (serverVersion == NMSVersion.UNKNOWN) {
            Logs.logWarning(String.format("The Version '%s' can indicate, that you are using a newer Minecraft version than currently supported.", serverVersion));
            Logs.logWarning("In this case please update to the newest version of this plugin. If this is the newest Version, than please be patient. It can take a few weeks until the plugin is updated.");
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
        return Set.of("sivert", "thomas").contains(split.get(split.indexOf("Built-By") + 1).toLowerCase());
    }

    public static String getMinecraftVersion() {
        if (isPaperServer()) return Bukkit.getMinecraftVersion();
        else return Bukkit.getVersion().split("MC: ")[1].split("\\)")[0];
    }
}
