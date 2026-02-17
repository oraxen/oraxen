package io.th0rgal.oraxen.pack.generation;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolVersionTest {

    @Test
    void testFromProtocol_ExactMatch() {
        assertEquals(ProtocolVersion.MC_1_21_4, ProtocolVersion.fromProtocol(769));
        assertEquals(ProtocolVersion.MC_1_21, ProtocolVersion.fromProtocol(767));
        assertEquals(ProtocolVersion.MC_1_20_3, ProtocolVersion.fromProtocol(765));
        assertEquals(ProtocolVersion.MC_1_20, ProtocolVersion.fromProtocol(763));
        assertEquals(ProtocolVersion.MC_1_19, ProtocolVersion.fromProtocol(759));
        assertEquals(ProtocolVersion.MC_1_18_2, ProtocolVersion.fromProtocol(758));
    }

    @Test
    void testFromProtocol_BestMatch() {
        assertEquals(ProtocolVersion.MC_1_21_11, ProtocolVersion.fromProtocol(770));
        assertEquals(ProtocolVersion.MC_1_21_11, ProtocolVersion.fromProtocol(771));
        assertEquals(ProtocolVersion.MC_1_21_11, ProtocolVersion.fromProtocol(800));
        assertEquals(ProtocolVersion.MC_1_21_4, ProtocolVersion.fromProtocol(769));
        assertEquals(ProtocolVersion.MC_1_21_2, ProtocolVersion.fromProtocol(768));
        assertEquals(ProtocolVersion.MC_1_20_5, ProtocolVersion.fromProtocol(766));
        assertEquals(ProtocolVersion.MC_1_20_3, ProtocolVersion.fromProtocol(765));
    }

    @Test
    void testFromProtocol_UnknownVersions() {
        assertEquals(ProtocolVersion.UNKNOWN, ProtocolVersion.fromProtocol(0));
        assertEquals(ProtocolVersion.UNKNOWN, ProtocolVersion.fromProtocol(-1));
        assertEquals(ProtocolVersion.UNKNOWN, ProtocolVersion.fromProtocol(-100));
    }

    @Test
    void testFromProtocol_VeryOldVersion() {
        ProtocolVersion result = ProtocolVersion.fromProtocol(100);
        assertEquals(ProtocolVersion.UNKNOWN, result);
    }

    @Test
    void testGetPackFormatForProtocol() {
        assertEquals(61, ProtocolVersion.getPackFormatForProtocol(770));
        assertEquals(46, ProtocolVersion.getPackFormatForProtocol(769));
        assertEquals(42, ProtocolVersion.getPackFormatForProtocol(768));
        assertEquals(34, ProtocolVersion.getPackFormatForProtocol(767));
        assertEquals(32, ProtocolVersion.getPackFormatForProtocol(766));
        assertEquals(22, ProtocolVersion.getPackFormatForProtocol(765));
        assertEquals(18, ProtocolVersion.getPackFormatForProtocol(764));
        assertEquals(15, ProtocolVersion.getPackFormatForProtocol(763));
        assertEquals(13, ProtocolVersion.getPackFormatForProtocol(762));
        assertEquals(9, ProtocolVersion.getPackFormatForProtocol(759));
        assertEquals(8, ProtocolVersion.getPackFormatForProtocol(758));
    }

    @Test
    void testGetPackFormatForProtocol_UnknownVersion() {
        assertEquals(6, ProtocolVersion.getPackFormatForProtocol(0));
        assertEquals(6, ProtocolVersion.getPackFormatForProtocol(-1));
    }

    @Test
    void testGetVersionStringForProtocol() {
        assertEquals("1.21.4", ProtocolVersion.getVersionStringForProtocol(769));
        assertEquals("1.21.2", ProtocolVersion.getVersionStringForProtocol(768));
        assertEquals("1.21", ProtocolVersion.getVersionStringForProtocol(767));
        assertEquals("1.20.5", ProtocolVersion.getVersionStringForProtocol(766));
        assertEquals("1.20.3", ProtocolVersion.getVersionStringForProtocol(765));
        assertEquals("1.20.2", ProtocolVersion.getVersionStringForProtocol(764));
        assertEquals("1.20", ProtocolVersion.getVersionStringForProtocol(763));
    }

    @Test
    void testGetVersionStringForProtocol_FutureVersion() {
        assertEquals("1.21.11+", ProtocolVersion.getVersionStringForProtocol(771));
        assertEquals("1.21.11+", ProtocolVersion.getVersionStringForProtocol(772));
        assertEquals("1.21.11+", ProtocolVersion.getVersionStringForProtocol(800));
        assertEquals("1.21.11", ProtocolVersion.getVersionStringForProtocol(770));
    }

    @Test
    void testGetVersionStringForProtocol_UnknownVersion() {
        assertEquals("Unknown (0)", ProtocolVersion.getVersionStringForProtocol(0));
        assertEquals("Unknown (-1)", ProtocolVersion.getVersionStringForProtocol(-1));
        assertEquals("Unknown (100)", ProtocolVersion.getVersionStringForProtocol(100));
    }

    @Test
    void testEnumProperties() {
        assertEquals(770, ProtocolVersion.MC_1_21_11.getProtocol());
        assertEquals(61, ProtocolVersion.MC_1_21_11.getPackFormat());
        assertEquals("1.21.11", ProtocolVersion.MC_1_21_11.getVersionString());

        assertEquals(769, ProtocolVersion.MC_1_21_4.getProtocol());
        assertEquals(46, ProtocolVersion.MC_1_21_4.getPackFormat());
        assertEquals("1.21.4", ProtocolVersion.MC_1_21_4.getVersionString());

        assertEquals(767, ProtocolVersion.MC_1_21.getProtocol());
        assertEquals(34, ProtocolVersion.MC_1_21.getPackFormat());
        assertEquals("1.21", ProtocolVersion.MC_1_21.getVersionString());
    }

    @Test
    void testIsKnown() {
        assertTrue(ProtocolVersion.MC_1_21_4.isKnown());
        assertTrue(ProtocolVersion.MC_1_20.isKnown());
        assertFalse(ProtocolVersion.UNKNOWN.isKnown());
    }

    @Test
    void testConsistencyWithPackVersion() {
        File tempFile = new java.io.File("/tmp/test.zip");
        PackVersion packVersion = new PackVersion("1.20.4", 22, 22, 31, tempFile);

        assertTrue(packVersion.supportsProtocol(765));

        assertFalse(packVersion.supportsProtocol(766));
    }
}
