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

    /** Pack format for 1.21.4/1.21.5 */
    public static final int PACK_FORMAT_1_21_4 = 46;
    /** Pack format for 1.21.6+ (first version with new shader format) */
    public static final int PACK_FORMAT_1_21_6 = 55;

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
        // Map major shader-breaking versions to their pack formats
        if (version.isAtLeast(new MinecraftVersion("1.21.6"))) return PACK_FORMAT_1_21_6;
        if (version.isAtLeast(new MinecraftVersion("1.21.4"))) return PACK_FORMAT_1_21_4;
        if (version.isAtLeast(new MinecraftVersion("1.21.2"))) return 42;
        if (version.isAtLeast(new MinecraftVersion("1.21"))) return 34;
        if (version.isAtLeast(new MinecraftVersion("1.20.5"))) return 32;
        if (version.isAtLeast(new MinecraftVersion("1.20.3"))) return 22;
        if (version.isAtLeast(new MinecraftVersion("1.20.2"))) return 18;
        if (version.isAtLeast(new MinecraftVersion("1.20"))) return 15;
        return 15; // fallback
    }

    public boolean isAtLeast(String version) {
        return minecraftVersion.isAtLeast(new MinecraftVersion(version));
    }

    public String displayName() {
        return minecraftVersion.getVersion() + " (pack_format=" + packFormat + ")";
    }
}
