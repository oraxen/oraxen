package io.th0rgal.oraxen.pack.generation;

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
 * <p>The generated client groups and their format ranges live in
 * {@link #VERSION_DEFINITIONS}. Individual Minecraft versions are mapped into one
 * of those ranges by their resource pack format.</p>
 */
public class PackVersionManager {

    /**
     * The two resource pack variants generated for supported clients.
     * Order: representative mcVersion, packFormat, minFormatInclusive, maxFormatInclusive.
     */
    private static final Object[][] VERSION_DEFINITIONS = {
        // mcVersion, format, minFormat, maxFormat
        {"1.21.4", 46, 46, 999}, // 1.21.4 and newer
        {"1.21.3", 42, 15, 45},  // 1.20 through 1.21.3
    };

    private final Map<String, PackVersion> packVersions = new ConcurrentHashMap<>();
    private final File packFolder;
    private volatile PackVersion serverPackVersion;
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
     * Defines the pack variants generated for the supported client groups.
     * Reads from {@link #VERSION_DEFINITIONS} so the ranges are defined in exactly one place.
     * This should be called before generation starts.
     */
    public void definePackVersions() {
        packVersions.clear();

        for (Object[] def : VERSION_DEFINITIONS) {
            addPackVersion((String) def[0], (int) def[1], (int) def[2], (int) def[3]);
        }

        logSuccess("Defined " + packVersions.size() + " pack versions for multi-version support");
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

        // Try exact and normalized keys first before format fallback.
        this.serverPackVersion = findByVersionKey(serverMcVersion);

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

    @Nullable
    private PackVersion findByVersionKey(String serverMcVersion) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(serverMcVersion);

        // Strip all trailing ".0" groups (e.g., "1.20.5.0" -> "1.20.5", "1.21.0.0" -> "1.21").
        String normalized = serverMcVersion;
        while (true) {
            int lastDot = normalized.lastIndexOf('.');
            if (lastDot <= 0) break;
            if (!"0".equals(normalized.substring(lastDot + 1))) break;
            normalized = normalized.substring(0, lastDot);
            candidates.add(normalized);
        }

        // Map ".0"/".1" patch versions to their major.minor key when present
        // (e.g., "1.21.1" and "1.21.0" should match key "1.21").
        String[] parts = normalized.split("\\.");
        if (parts.length >= 3) {
            try {
                int patch = Integer.parseInt(parts[2]);
                if (patch <= 1) {
                    candidates.add(parts[0] + "." + parts[1]);
                }
            } catch (NumberFormatException ignored) {
                // Non-numeric versions should fall back to pack-format lookup below.
            }
        }

        for (String candidate : candidates) {
            PackVersion candidateVersion = packVersions.get(candidate);
            if (candidateVersion != null) {
                return candidateVersion;
            }
        }
        return null;
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
