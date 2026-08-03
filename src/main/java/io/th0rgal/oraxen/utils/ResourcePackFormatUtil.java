package io.th0rgal.oraxen.utils;

/**
 * Resolves the correct {@code pack_format} for the server's current Minecraft version.
 *
 * <p>No Bukkit/Paper API exposes pack formats, so this keeps a maintained
 * version-to-format mapping that must be extended for new Minecraft releases.</p>
 */
public final class ResourcePackFormatUtil {

    // Ordering matters: every "26.x" namespace entry must precede all "1.x" entries,
    // because a major-26 version compares greater than any "1.x" threshold.
    private static final PackFormatThreshold[] PACK_FORMAT_THRESHOLDS = {
            new PackFormatThreshold("26.2", 88),
            new PackFormatThreshold("26.1", 84),
            new PackFormatThreshold("1.26.2", 88),
            new PackFormatThreshold("1.26.1", 84),
            new PackFormatThreshold("1.21.11", 75),
            new PackFormatThreshold("1.21.9", 69),
            new PackFormatThreshold("1.21.7", 64),
            new PackFormatThreshold("1.21.6", 63),
            new PackFormatThreshold("1.21.5", 55),
            new PackFormatThreshold("1.21.4", 46),
            new PackFormatThreshold("1.21.2", 42),
            new PackFormatThreshold("1.21", 34),
            new PackFormatThreshold("1.20.5", 32),
            new PackFormatThreshold("1.20.3", 22),
            new PackFormatThreshold("1.20.2", 18),
            new PackFormatThreshold("1.20", 15),
            new PackFormatThreshold("1.19.4", 13),
            new PackFormatThreshold("1.19.3", 12),
            new PackFormatThreshold("1.19", 9),
            new PackFormatThreshold("1.18", 8)
    };
    private static final PackFormatThreshold[] DATA_PACK_FORMAT_THRESHOLDS = {
            new PackFormatThreshold("26.1", 84),
            new PackFormatThreshold("1.26.1", 84),
            new PackFormatThreshold("1.21.11", 89),
            new PackFormatThreshold("1.21.10", 88),
            new PackFormatThreshold("1.21.9", 87),
            new PackFormatThreshold("1.21.7", 81),
            new PackFormatThreshold("1.21.6", 80),
            new PackFormatThreshold("1.21.5", 71),
            new PackFormatThreshold("1.21.4", 61),
            new PackFormatThreshold("1.21.2", 57),
            new PackFormatThreshold("1.21", 48),
            new PackFormatThreshold("1.20.5", 41),
            new PackFormatThreshold("1.20.3", 26),
            new PackFormatThreshold("1.20.2", 18),
            new PackFormatThreshold("1.20", 15),
            new PackFormatThreshold("1.19.4", 12),
            new PackFormatThreshold("1.19.3", 10),
            new PackFormatThreshold("1.19", 10),
            new PackFormatThreshold("1.18", 9)
    };

    private ResourcePackFormatUtil() {
    }

    public static int getCurrentResourcePackFormat() {
        return getPackFormatForVersion(MinecraftVersion.getCurrentVersion());
    }

    public static int getCurrentDataPackFormat() {
        return getDataPackFormatForVersion(MinecraftVersion.getCurrentVersion());
    }

    /**
     * Gets the pack format for a specific Minecraft version.
     * This is a best-effort mapping based on known version-to-format relationships.
     * Used by multi-version pack generation to map server versions to pack formats.
     *
     * @param version Minecraft version to get pack format for
     * @return Pack format number
     */
    public static int getPackFormatForVersion(MinecraftVersion version) {
        return getFormatForVersion(version, PACK_FORMAT_THRESHOLDS, 6);
    }

    public static int getDataPackFormatForVersion(MinecraftVersion version) {
        return getFormatForVersion(version, DATA_PACK_FORMAT_THRESHOLDS, 8);
    }

    private static int getFormatForVersion(MinecraftVersion version, PackFormatThreshold[] thresholds, int fallback) {
        for (PackFormatThreshold threshold : thresholds) {
            if (version.isAtLeast(threshold.minimumVersion)) {
                return threshold.packFormat;
            }
        }

        // Very old / unknown
        return fallback;
    }

    private record PackFormatThreshold(MinecraftVersion minimumVersion, int packFormat) {
        private PackFormatThreshold(String minimumVersion, int packFormat) {
            this(new MinecraftVersion(minimumVersion), packFormat);
        }
    }
}
