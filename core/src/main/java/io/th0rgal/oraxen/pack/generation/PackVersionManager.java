package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.ResourcePackFormatUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple versions of resource packs for different Minecraft client versions.
 *
 * <p>The canonical version-to-format-range mapping lives in {@link #VERSION_DEFINITIONS}.
 * Both {@link #definePackVersions()} and {@link #getFormatRangeForPackFormat(int)} read
 * from that single table, so there is exactly one place to update when Minecraft adds
 * a new pack format.</p>
 */
public class PackVersionManager {

    /**
     * Single source of truth for Minecraft version → pack format range mappings.
     * Order: mcVersion, packFormat, minFormatInclusive, maxFormatInclusive.
     * Sorted newest-first for readability only; the code does not depend on order.
     */
    private static final Object[][] VERSION_DEFINITIONS = {
        // mcVersion, format, minFormat, maxFormat
        {"1.21.4", 46, 46, 999},  // 1.21.4+ (latest, open-ended)
        {"1.21.2", 42, 42, 45},   // 1.21.2-1.21.3
        {"1.21",   34, 34, 41},   // 1.21-1.21.1
        {"1.20.5", 32, 32, 33},   // 1.20.5-1.20.6
        {"1.20.3", 22, 22, 31},   // 1.20.3-1.20.4
        {"1.20.2", 18, 18, 21},   // 1.20.2
        {"1.20",   15, 15, 17},   // 1.20-1.20.1
    };

    private final Map<String, PackVersion> packVersions = new ConcurrentHashMap<>();
    private final File packFolder;
    private PackVersion serverPackVersion;
    private boolean silentMode = false;

    public PackVersionManager(File packFolder) {
        this.packFolder = packFolder;
    }

    public void setSilentMode(boolean silent) {
        this.silentMode = silent;
    }

    private void logInfo(String message) {
        if (silentMode) return;
        try {
            Logs.logInfo(message);
        } catch (NoClassDefFoundError ignored) {}
    }

    private void logSuccess(String message) {
        if (silentMode) return;
        try {
            Logs.logSuccess(message);
        } catch (NoClassDefFoundError ignored) {}
    }

    private void logWarning(String message) {
        if (silentMode) return;
        try {
            Logs.logWarning(message);
        } catch (NoClassDefFoundError ignored) {}
    }

    /**
     * Defines the pack versions that should be generated based on supported Minecraft versions.
     * Reads from {@link #VERSION_DEFINITIONS} so the mapping is defined in exactly one place.
     * This should be called before generation starts.
     */
    public void definePackVersions() {
        packVersions.clear();

        for (Object[] def : VERSION_DEFINITIONS) {
            addPackVersion((String) def[0], (int) def[1], (int) def[2], (int) def[3]);
        }

        logSuccess("Defined " + packVersions.size() + " pack versions for multi-version support");
    }

    /**
     * Returns the supported_formats range for a given pack format.
     * Looks up the range in {@link #VERSION_DEFINITIONS}, the same table
     * that {@link #definePackVersions()} uses — eliminating data duplication.
     *
     * @param packFormat the server's pack format
     * @return int[2] with {min_inclusive, max_inclusive}, or {0,0} if no range covers this format
     */
    public static int[] getFormatRangeForPackFormat(int packFormat) {
        for (Object[] def : VERSION_DEFINITIONS) {
            int min = (int) def[2];
            int max = (int) def[3];
            if (packFormat >= min && packFormat <= max) {
                return new int[]{min, max};
            }
        }
        return new int[]{0, 0};
    }

    private void addPackVersion(String mcVersion, int format, int minFormat, int maxFormat) {
        File packFile = new File(packFolder, "pack_" + mcVersion.replace(".", "_") + ".zip");
        PackVersion version = new PackVersion(mcVersion, format, minFormat, maxFormat, packFile);
        packVersions.put(mcVersion, version);
        logInfo("  - " + mcVersion + ": format " + format + " (supports " + minFormat + "-" + maxFormat + ")");
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
        if (serverMcVersion == null) {
            logWarning("Cannot set server pack version: version is null");
            return;
        }

        // Find exact match first
        this.serverPackVersion = packVersions.get(serverMcVersion);

        // Try normalized version (e.g., "1.21.0" -> "1.21")
        if (this.serverPackVersion == null && serverMcVersion.endsWith(".0")) {
            String normalized = serverMcVersion.replace(".0", "");
            this.serverPackVersion = packVersions.get(normalized);
        }

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
            logInfo("Server pack version set to: " + this.serverPackVersion.getMinecraftVersion()
                + " (pack format " + this.serverPackVersion.getPackFormat() + ")");
        }
    }

    /**
     * Maps Minecraft version string to pack format.
     * Delegates to ResourcePackFormatUtil for a single source of truth.
     *
     * @param version Minecraft version (e.g., "1.20.4", "1.21.1", "1.21.11")
     * @return Pack format number
     */
    private int getPackFormatForVersion(String version) {
        return ResourcePackFormatUtil.getPackFormatForVersion(new MinecraftVersion(version));
    }

    /**
     * Gets the server's pack version (used as fallback).
     *
     * @return Server pack version, or null if no versions are defined
     */
    @Nullable
    public PackVersion getServerPackVersion() {
        if (serverPackVersion != null) {
            return serverPackVersion;
        }

        // Fallback to highest format pack
        return packVersions.values().stream()
            .max(Comparator.naturalOrder())
            .orElse(null);
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
