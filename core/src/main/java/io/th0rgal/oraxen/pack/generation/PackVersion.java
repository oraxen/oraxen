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
    // Written on async upload thread, read from main/event thread — must be volatile.
    private volatile UUID packUUID;
    private volatile String packURL;
    private volatile byte[] packSHA1;

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

    /**
     * Returns the SHA1 hash as a lowercase hex string, or null if not set.
     */
    public String getPackSHA1Hex() {
        byte[] sha1 = this.packSHA1;
        if (sha1 == null) return null;
        StringBuilder sb = new StringBuilder(sha1.length * 2);
        for (byte b : sha1) sb.append(String.format("%02x", b));
        return sb.toString();
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
        int estimatedFormat = ProtocolVersion.getPackFormatForProtocol(protocolVersion);
        return supportsFormat(estimatedFormat);
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
