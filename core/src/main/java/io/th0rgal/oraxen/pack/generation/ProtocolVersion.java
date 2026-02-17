package io.th0rgal.oraxen.pack.generation;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/**
 * Centralized mapping of Minecraft protocol versions to pack formats and version strings.
 * This is the single source of truth for protocol version mappings used by:
 * - PackVersion.estimatePackFormatFromProtocol()
 * - PlayerVersionDetector.protocolToVersionString()
 * 
 * Protocol version reference: https://minecraft.wiki/w/Protocol_version
 * Pack format reference: https://minecraft.wiki/w/Pack_format
 */
public enum ProtocolVersion {
    MC_1_21_11(774, 61, "1.21.11"),
    MC_1_21_5(770, 48, "1.21.5"),
    MC_1_21_4(769, 46, "1.21.4"),
    MC_1_21_2(768, 42, "1.21.2"),
    MC_1_21(767, 34, "1.21"),
    MC_1_20_5(766, 32, "1.20.5"),
    MC_1_20_3(765, 22, "1.20.3"),
    MC_1_20_2(764, 18, "1.20.2"),
    MC_1_20(763, 15, "1.20"),
    MC_1_19_4(762, 13, "1.19.4"),
    MC_1_19_3(761, 12, "1.19.3"),
    MC_1_19_1(760, 9, "1.19.1"),
    MC_1_19(759, 9, "1.19"),
    MC_1_18_2(758, 8, "1.18.2"),
    MC_1_18(757, 8, "1.18"),
    UNKNOWN(0, 6, "Unknown");

    private final int protocol;
    private final int packFormat;
    private final String versionString;

    ProtocolVersion(int protocol, int packFormat, String versionString) {
        this.protocol = protocol;
        this.packFormat = packFormat;
        this.versionString = versionString;
    }

    public int getProtocol() {
        return protocol;
    }

    public int getPackFormat() {
        return packFormat;
    }

    public String getVersionString() {
        return versionString;
    }

    /**
     * Finds the ProtocolVersion enum for a given protocol number.
     * Returns the best matching version (highest protocol <= given protocol).
     *
     * @param protocol The protocol version number
     * @return The matching ProtocolVersion, or UNKNOWN if not found
     */
    @NotNull
    public static ProtocolVersion fromProtocol(int protocol) {
        if (protocol <= 0) {
            return UNKNOWN;
        }

        Optional<ProtocolVersion> match = Arrays.stream(values())
                .filter(v -> v != UNKNOWN)
                .filter(v -> v.protocol <= protocol)
                .max(Comparator.comparingInt(v -> v.protocol));

        return match.orElse(UNKNOWN);
    }

    /**
     * Gets the pack format for a given protocol version.
     * This is a convenience method equivalent to fromProtocol(protocol).getPackFormat().
     *
     * @param protocol The protocol version number
     * @return The corresponding pack format
     */
    public static int getPackFormatForProtocol(int protocol) {
        return fromProtocol(protocol).getPackFormat();
    }

    /**
     * Gets a human-readable version string for a given protocol version.
     * This is a convenience method equivalent to fromProtocol(protocol).getVersionString().
     * For the highest known protocol and above, appends "+" to indicate "or newer".
     *
     * @param protocol The protocol version number
     * @return The corresponding version string
     */
    @NotNull
    public static String getVersionStringForProtocol(int protocol) {
        ProtocolVersion version = fromProtocol(protocol);

        if (version == UNKNOWN) {
            return "Unknown (" + protocol + ")";
        }

        if (protocol > version.protocol) {
            return version.versionString + "+";
        }

        return version.versionString;
    }

    /**
     * Checks if this is a known (non-UNKNOWN) protocol version.
     *
     * @return true if this is a known version
     */
    public boolean isKnown() {
        return this != UNKNOWN;
    }
}
