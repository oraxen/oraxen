package io.th0rgal.oraxen.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtils {

    private static final HexFormat HEX = HexFormat.of();

    private HashUtils() {
    }

    public static String sha256(byte[] content) {
        try {
            return bytesToHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Converts a digest byte array to a lowercase hex string.
     *
     * @param hash the digest bytes, or null
     * @return lowercase hex string, or null if input is null
     */
    public static String bytesToHex(byte[] hash) {
        if (hash == null) return null;
        return HEX.formatHex(hash);
    }

    /**
     * Converts a hex string to a byte array.
     *
     * @param hex the hex string (must have even length)
     * @return the decoded byte array
     */
    public static byte[] hexToBytes(String hex) {
        return HEX.parseHex(hex);
    }
}
