package io.th0rgal.oraxen.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SHA1UtilsTest {

    @Test
    void bytesToHex_returnsLowercaseHex() {
        byte[] input = {(byte) 0xAB, (byte) 0xCD, (byte) 0x01, (byte) 0x23};
        assertEquals("abcd0123", SHA1Utils.bytesToHex(input));
    }

    @Test
    void bytesToHex_nullReturnsNull() {
        assertNull(SHA1Utils.bytesToHex(null));
    }

    @Test
    void hexToBytes_decodesCorrectly() {
        byte[] expected = {(byte) 0xAB, (byte) 0xCD, (byte) 0x01, (byte) 0x23};
        assertArrayEquals(expected, SHA1Utils.hexToBytes("abcd0123"));
    }

    @Test
    void roundTrip_bytesToHexToBytes() {
        byte[] original = {0x00, 0x7F, (byte) 0xFF, 0x10, (byte) 0xDE, (byte) 0xAD};
        assertArrayEquals(original, SHA1Utils.hexToBytes(SHA1Utils.bytesToHex(original)));
    }

    @Test
    void roundTrip_hexToBytesToHex() {
        String original = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
        assertEquals(original, SHA1Utils.bytesToHex(SHA1Utils.hexToBytes(original)));
    }
}
