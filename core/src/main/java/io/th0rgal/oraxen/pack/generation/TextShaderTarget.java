package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.ResourcePackFormatUtil;

/**
 * Target information for text shader generation.
 *
 * @param packFormat Resource pack format
 * @param minecraftVersion Server Minecraft version
 */
public record TextShaderTarget(int packFormat, MinecraftVersion minecraftVersion) {

    /** Pack format for 1.21.4 (format range 46-62 includes 1.21.5) */
    public static final int PACK_FORMAT_1_21_4 = 46;
    /** Pack format for 1.21.6 (format range 63-83; 1.21.6-1.21.11 share the same shader format with texelFetch) */
    public static final int PACK_FORMAT_1_21_6 = 63;
    /** Pack format for 26.x (first version with sample_lightmap replacing texelFetch) */
    public static final int PACK_FORMAT_26 = 84;

    public static TextShaderTarget current() {
        return new TextShaderTarget(ResourcePackFormatUtil.getCurrentResourcePackFormat(),
                MinecraftVersion.getCurrentVersion());
    }

    /**
     * Creates a target for a specific Minecraft version.
     * Used for generating overlay shaders for different client versions.
     */
    public static TextShaderTarget forVersion(String version) {
        MinecraftVersion mcVersion = new MinecraftVersion(version);
        int packFormat = getPackFormatForVersion(mcVersion);
        return new TextShaderTarget(packFormat, mcVersion);
    }

    private static int getPackFormatForVersion(MinecraftVersion version) {
        if (version.isAtLeast(new MinecraftVersion("26"))) return PACK_FORMAT_26;
        if (version.isAtLeast(new MinecraftVersion("1.21.6"))) return PACK_FORMAT_1_21_6;
        if (version.isAtLeast(new MinecraftVersion("1.21.4"))) return PACK_FORMAT_1_21_4;
        if (version.isAtLeast(new MinecraftVersion("1.21.2"))) return 42;
        if (version.isAtLeast(new MinecraftVersion("1.21"))) return 34;
        if (version.isAtLeast(new MinecraftVersion("1.20.5"))) return 32;
        if (version.isAtLeast(new MinecraftVersion("1.20.3"))) return 22;
        if (version.isAtLeast(new MinecraftVersion("1.20.2"))) return 18;
        if (version.isAtLeast(new MinecraftVersion("1.20"))) return 15;
        return 15;
    }

    public boolean isAtLeast(String version) {
        MinecraftVersion threshold = new MinecraftVersion(version);
        if (minecraftVersion.isAtLeast(threshold)) return true;
        // Handle runtimes reporting "1.26.x" instead of "26.x":
        // normalize by comparing without the legacy "1." prefix.
        if (threshold.getMajor() >= 26 && minecraftVersion.getMajor() == 1 && minecraftVersion.getMinor() >= 26) {
            MinecraftVersion normalized = new MinecraftVersion(
                    minecraftVersion.getMinor(), minecraftVersion.getBuild(), 0);
            return normalized.isAtLeast(threshold);
        }
        return false;
    }

    public String displayName() {
        return minecraftVersion.getVersion() + " (pack_format=" + packFormat + ")";
    }
}
