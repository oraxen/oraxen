package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple versions of resource packs for different Minecraft client versions.
 */
public class PackVersionManager {

    private final Map<String, PackVersion> packVersions = new ConcurrentHashMap<>();
    private final File packFolder;
    private PackVersion serverPackVersion; // Pack version for the server's MC version

    public PackVersionManager(File packFolder) {
        this.packFolder = packFolder;
    }

    /**
     * Defines the pack versions that should be generated based on supported Minecraft versions.
     * This should be called before generation starts.
     */
    public void definePackVersions() {
        packVersions.clear();

        // Define pack versions for different Minecraft client versions
        // Each entry targets a specific Minecraft version range

        // 1.21.4+ (latest format with full Item Properties support)
        addPackVersion("1.21.4", 46, 46, 999);

        // 1.21.2-1.21.3
        addPackVersion("1.21.2", 42, 42, 45);

        // 1.21-1.21.1
        addPackVersion("1.21", 34, 34, 41);

        // 1.20.5-1.20.6
        addPackVersion("1.20.5", 32, 32, 33);

        // 1.20.3-1.20.4
        addPackVersion("1.20.3", 22, 22, 31);

        // 1.20.2
        addPackVersion("1.20.2", 18, 18, 21);

        // 1.20-1.20.1
        addPackVersion("1.20", 15, 15, 17);

        Logs.logSuccess("Defined " + packVersions.size() + " pack versions for multi-version support");
    }

    private void addPackVersion(String mcVersion, int format, int minFormat, int maxFormat) {
        File packFile = new File(packFolder, "pack_" + mcVersion.replace(".", "_") + ".zip");
        PackVersion version = new PackVersion(mcVersion, format, minFormat, maxFormat, packFile);
        packVersions.put(mcVersion, version);
        Logs.logInfo("  - " + mcVersion + ": format " + format + " (supports " + minFormat + "-" + maxFormat + ")");
    }

    /**
     * Gets all defined pack versions.
     *
     * @return Unmodifiable collection of pack versions
     */
    @NotNull
    public Collection<PackVersion> getAllVersions() {
        return Collections.unmodifiableCollection(packVersions.values());
    }

    /**
     * Gets a pack version by Minecraft version.
     *
     * @param minecraftVersion Minecraft version string
     * @return PackVersion or null if not found
     */
    @Nullable
    public PackVersion getVersion(String minecraftVersion) {
        return packVersions.get(minecraftVersion);
    }

    /**
     * Finds the best pack version for a given pack format.
     *
     * @param packFormat Client's pack format
     * @return Best matching PackVersion, or null if none match
     */
    @Nullable
    public PackVersion findBestVersionForFormat(int packFormat) {
        return packVersions.values().stream()
            .filter(v -> v.supportsFormat(packFormat))
            .max(Comparator.naturalOrder()) // Prefer higher pack formats
            .orElse(null);
    }

    /**
     * Finds the best pack version for a given protocol version.
     *
     * @param protocolVersion Client's protocol version
     * @return Best matching PackVersion, or null if none match
     */
    @Nullable
    public PackVersion findBestVersionForProtocol(int protocolVersion) {
        return packVersions.values().stream()
            .filter(v -> v.supportsProtocol(protocolVersion))
            .max(Comparator.naturalOrder()) // Prefer higher pack formats
            .orElse(null);
    }

    /**
     * Sets the pack version that represents the server's Minecraft version.
     * This is used as a fallback when client version cannot be detected.
     *
     * @param serverMcVersion Server Minecraft version
     */
    public void setServerPackVersion(String serverMcVersion) {
        // Find exact match first
        this.serverPackVersion = packVersions.get(serverMcVersion);

        // If no exact match, find compatible version based on pack format
        if (this.serverPackVersion == null) {
            int serverFormat = getPackFormatForVersion(serverMcVersion);
            this.serverPackVersion = packVersions.values().stream()
                .filter(pack -> pack.supportsFormat(serverFormat))
                .min(Comparator.naturalOrder()) // Prefer lowest compatible format for broadest support
                .orElse(null);

            // Final fallback: use highest version
            if (this.serverPackVersion == null) {
                this.serverPackVersion = packVersions.values().stream()
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            }
        }

        if (this.serverPackVersion != null) {
            Logs.logInfo("Server pack version set to: " + this.serverPackVersion.getMinecraftVersion()
                + " (pack format " + this.serverPackVersion.getPackFormat() + ")");
        }
    }

    /**
     * Maps Minecraft version string to pack format.
     * Matches the logic in ResourcePackFormatUtil.fallbackByMinecraftVersion().
     *
     * @param version Minecraft version (e.g., "1.20.4", "1.21.1", "1.21.11")
     * @return Pack format number
     */
    private int getPackFormatForVersion(String version) {
        // Parse version string to compare
        // NOTE: For 1.21.x versions, pack formats vary significantly between patches
        if (version.startsWith("1.21.11")) return 61; // 1.21.11+
        if (version.startsWith("1.21.4") || version.startsWith("1.21.5") || version.startsWith("1.21.6")
            || version.startsWith("1.21.7") || version.startsWith("1.21.8") || version.startsWith("1.21.9")
            || version.startsWith("1.21.10")) return 46; // 1.21.4-1.21.10
        if (version.startsWith("1.21.2") || version.startsWith("1.21.3")) return 42;
        if (version.startsWith("1.21")) return 34; // 1.21-1.21.1
        if (version.startsWith("1.20.5") || version.startsWith("1.20.6")) return 32;
        if (version.startsWith("1.20.3") || version.startsWith("1.20.4")) return 22;
        if (version.startsWith("1.20.2")) return 18;
        if (version.startsWith("1.20")) return 15;
        if (version.startsWith("1.19.4")) return 13;
        if (version.startsWith("1.19.3")) return 12;
        if (version.startsWith("1.19")) return 9;
        if (version.startsWith("1.18")) return 8;
        return 15; // Default fallback
    }

    /**
     * Gets the server's pack version (used as fallback).
     *
     * @return Server pack version, or the highest format pack if not set
     */
    @NotNull
    public PackVersion getServerPackVersion() {
        if (serverPackVersion != null) {
            return serverPackVersion;
        }

        // Fallback to highest format pack
        return packVersions.values().stream()
            .max(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("No pack versions defined"));
    }

    /**
     * Gets the number of defined pack versions.
     *
     * @return Number of pack versions
     */
    public int getVersionCount() {
        return packVersions.size();
    }

    /**
     * Checks if any pack versions are defined.
     *
     * @return true if at least one pack version is defined
     */
    public boolean hasVersions() {
        return !packVersions.isEmpty();
    }

    /**
     * Clears all pack version definitions.
     */
    public void clear() {
        packVersions.clear();
        serverPackVersion = null;
    }
}
