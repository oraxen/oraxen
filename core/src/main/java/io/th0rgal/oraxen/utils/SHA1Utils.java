package io.th0rgal.oraxen.utils;

/**
 * Shared SHA-1 hex conversion utilities.
 * Replaces duplicated byte-to-hex and hex-to-byte logic
 * previously found in PackVersion, SelfHost, and Polymath.
 */
public final class SHA1Utils {

    private SHA1Utils() {
    }

    /**
     * Converts a SHA-1 byte array to a lowercase hex string.
     *
     * @param hash the SHA-1 digest bytes, or null
     * @return lowercase hex string, or null if input is null
     */
    public static String bytesToHex(byte[] hash) {
        if (hash == null) return null;
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * Converts a hex string to a byte array.
     *
     * @param hex the hex string (must have even length)
     * @return the decoded byte array
     */
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
