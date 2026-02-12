package io.th0rgal.oraxen.pack.generation;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a specific version of a resource pack targeting a Minecraft client version range.
 */
public class PackVersion implements Comparable<PackVersion> {

    private final String minecraftVersion;
    private final int packFormat;
    private final int minFormatInclusive;
    private final int maxFormatInclusive;
    private final File packFile;
    private UUID packUUID; // Not final - set by hosting provider after upload
    private String packURL;
    private byte[] packSHA1;

    /**
     * Creates a new pack version.
     *
     * @param minecraftVersion Representative Minecraft version (e.g., "1.20.4")
     * @param packFormat Primary pack format
     * @param minFormatInclusive Minimum supported pack format (inclusive)
     * @param maxFormatInclusive Maximum supported pack format (inclusive)
     * @param packFile The zip file for this pack version
     */
    public PackVersion(String minecraftVersion, int packFormat, int minFormatInclusive,
                      int maxFormatInclusive, File packFile) {
        this.minecraftVersion = minecraftVersion;
        this.packFormat = packFormat;
        this.minFormatInclusive = minFormatInclusive;
        this.maxFormatInclusive = maxFormatInclusive;
        this.packFile = packFile;
        // UUID will be set by hosting provider after upload (content-based for consistency)
        this.packUUID = null;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public int getPackFormat() {
        return packFormat;
    }

    public int getMinFormatInclusive() {
        return minFormatInclusive;
    }

    public int getMaxFormatInclusive() {
        return maxFormatInclusive;
    }

    public File getPackFile() {
        return packFile;
    }

    public UUID getPackUUID() {
        return packUUID;
    }

    public String getPackURL() {
        return packURL;
    }

    public void setPackURL(String packURL) {
        this.packURL = packURL;
    }

    public byte[] getPackSHA1() {
        return packSHA1;
    }

    public void setPackSHA1(byte[] packSHA1) {
        this.packSHA1 = packSHA1;
    }

    public void setPackUUID(UUID packUUID) {
        this.packUUID = packUUID;
    }

    /**
     * Checks if this pack version supports the given pack format.
     *
     * @param format Pack format to check
     * @return true if this pack supports the format
     */
    public boolean supportsFormat(int format) {
        return format >= minFormatInclusive && format <= maxFormatInclusive;
    }

    /**
     * Checks if this pack version supports the given protocol version.
     *
     * @param protocolVersion Client protocol version
     * @return true if this pack likely supports the protocol
     */
    public boolean supportsProtocol(int protocolVersion) {
        // Map protocol versions to pack formats (best effort)
        int estimatedFormat = estimatePackFormatFromProtocol(protocolVersion);
        return supportsFormat(estimatedFormat);
    }

    private int estimatePackFormatFromProtocol(int protocolVersion) {
        // Protocol to pack format mapping (Minecraft 1.19+)
        // https://minecraft.wiki/w/Protocol_version
        // https://minecraft.wiki/w/Pack_format

        if (protocolVersion >= 769) return 46; // 1.21.4+ (protocol 769+)
        if (protocolVersion >= 768) return 42; // 1.21.2 (protocol 768+)
        if (protocolVersion >= 767) return 34; // 1.21 (protocol 767)
        if (protocolVersion >= 766) return 32; // 1.20.5 (protocol 766)
        if (protocolVersion >= 765) return 22; // 1.20.3 (protocol 765)
        if (protocolVersion >= 764) return 18; // 1.20.2 (protocol 764)
        if (protocolVersion >= 763) return 15; // 1.20 (protocol 763)
        if (protocolVersion >= 762) return 13; // 1.19.4 (protocol 762)
        if (protocolVersion >= 761) return 12; // 1.19.3 (protocol 761)
        if (protocolVersion >= 760) return 9;  // 1.19.1 (protocol 760)
        if (protocolVersion >= 759) return 9;  // 1.19 (protocol 759)
        if (protocolVersion >= 758) return 8;  // 1.18.2 (protocol 758)

        return 6; // Fallback for older versions
    }

    @Override
    public int compareTo(@NotNull PackVersion other) {
        // Sort by pack format in ascending order (standard natural ordering)
        // Higher pack formats will be "greater than" lower ones
        return Integer.compare(this.packFormat, other.packFormat);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackVersion that = (PackVersion) o;
        return packFormat == that.packFormat &&
               minFormatInclusive == that.minFormatInclusive &&
               maxFormatInclusive == that.maxFormatInclusive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(packFormat, minFormatInclusive, maxFormatInclusive);
    }

    @Override
    public String toString() {
        return String.format("PackVersion{mc=%s, format=%d, range=[%d,%d]}",
            minecraftVersion, packFormat, minFormatInclusive, maxFormatInclusive);
    }

    /**
     * Gets a filename-safe identifier for this pack version.
     *
     * @return Filename-safe identifier (e.g., "1_20_4")
     */
    public String getFileIdentifier() {
        return minecraftVersion.replace(".", "_");
    }
}
