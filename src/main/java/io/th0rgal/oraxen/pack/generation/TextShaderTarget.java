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
    /** Pack format for 26.1.x (first version with sample_lightmap replacing texelFetch) */
    public static final int PACK_FORMAT_26 = 84;
    /** Pack format for 26.2+ (text shaders renamed to core/text and variants use defines) */
    public static final int PACK_FORMAT_26_2 = 88;

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
        int packFormat = ResourcePackFormatUtil.getPackFormatForVersion(mcVersion);
        return new TextShaderTarget(packFormat, mcVersion);
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

    boolean usesUnifiedTextShader() {
        return packFormat >= PACK_FORMAT_26_2;
    }

    public String displayName() {
        return minecraftVersion.getVersion() + " (pack_format=" + packFormat + ")";
    }
}
