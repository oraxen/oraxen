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

    public static TextShaderTarget current() {
        return new TextShaderTarget(ResourcePackFormatUtil.getCurrentResourcePackFormat(),
                MinecraftVersion.getCurrentVersion());
    }

    public boolean isAtLeast(String version) {
        return minecraftVersion.isAtLeast(new MinecraftVersion(version));
    }

    public String displayName() {
        return minecraftVersion.getVersion() + " (pack_format=" + packFormat + ")";
    }
}
