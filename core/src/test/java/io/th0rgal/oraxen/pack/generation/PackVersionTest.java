package io.th0rgal.oraxen.pack.generation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PackVersionTest {

    @TempDir
    Path tempDir;

    @Test
    void testPackVersionCreation() {
        File packFile = tempDir.resolve("pack_1_20_4.zip").toFile();
        PackVersion version = new PackVersion("1.20.4", 22, 22, 31, packFile);

        assertEquals("1.20.4", version.getMinecraftVersion());
        assertEquals(22, version.getPackFormat());
        assertEquals(22, version.getMinFormatInclusive());
        assertEquals(31, version.getMaxFormatInclusive());
        assertEquals(packFile, version.getPackFile());
        // UUID is null until set by hosting provider after upload
        assertNull(version.getPackUUID());
    }

    @Test
    void testSupportsFormat() {
        File packFile = tempDir.resolve("pack.zip").toFile();
        PackVersion version = new PackVersion("1.20.4", 22, 22, 31, packFile);

        // Within range
        assertTrue(version.supportsFormat(22));
        assertTrue(version.supportsFormat(25));
        assertTrue(version.supportsFormat(31));

        // Outside range
        assertFalse(version.supportsFormat(21));
        assertFalse(version.supportsFormat(32));
    }

    @Test
    void testSupportsProtocol() {
        File packFile = tempDir.resolve("pack.zip").toFile();
        PackVersion version = new PackVersion("1.20.4", 22, 22, 31, packFile);

        // Protocol 765 is 1.20.3, which maps to pack format 22
        assertTrue(version.supportsProtocol(765));

        // Protocol 766 is 1.20.5, which maps to pack format 32
        assertFalse(version.supportsProtocol(766));
    }

    @Test
    void testComparison() {
        File packFile1 = tempDir.resolve("pack1.zip").toFile();
        File packFile2 = tempDir.resolve("pack2.zip").toFile();

        PackVersion version1_20 = new PackVersion("1.20", 15, 15, 17, packFile1);
        PackVersion version1_21 = new PackVersion("1.21", 34, 34, 41, packFile2);

        // Natural ordering: higher format > lower format (ascending order)
        assertTrue(version1_21.compareTo(version1_20) > 0); // 1.21 (format 34) > 1.20 (format 15)
        assertTrue(version1_20.compareTo(version1_21) < 0); // 1.20 (format 15) < 1.21 (format 34)
        assertEquals(0, version1_20.compareTo(version1_20));
    }

    @Test
    void testFileIdentifier() {
        File packFile = tempDir.resolve("pack.zip").toFile();
        PackVersion version = new PackVersion("1.20.4", 22, 22, 31, packFile);

        assertEquals("1_20_4", version.getFileIdentifier());
    }

    @Test
    void testPackMetadata() {
        File packFile = tempDir.resolve("pack.zip").toFile();
        PackVersion version = new PackVersion("1.20.4", 22, 22, 31, packFile);

        assertNull(version.getPackURL());
        assertNull(version.getPackSHA1());

        version.setPackURL("http://example.com/pack.zip");
        byte[] sha1 = new byte[]{1, 2, 3, 4, 5};
        version.setPackSHA1(sha1);

        assertEquals("http://example.com/pack.zip", version.getPackURL());
        assertArrayEquals(sha1, version.getPackSHA1());
    }

    @Test
    void testToString() {
        File packFile = tempDir.resolve("pack.zip").toFile();
        PackVersion version = new PackVersion("1.20.4", 22, 22, 31, packFile);

        String str = version.toString();
        assertTrue(str.contains("1.20.4"));
        assertTrue(str.contains("22"));
        assertTrue(str.contains("[22,31]"));
    }

    @Test
    void testEquality() {
        File packFile1 = tempDir.resolve("pack1.zip").toFile();
        File packFile2 = tempDir.resolve("pack2.zip").toFile();

        PackVersion version1 = new PackVersion("1.20.4", 22, 22, 31, packFile1);
        PackVersion version2 = new PackVersion("1.20.4", 22, 22, 31, packFile2);
        PackVersion version3 = new PackVersion("1.21", 34, 34, 41, packFile1);

        assertEquals(version1, version2); // Same format ranges
        assertNotEquals(version1, version3); // Different format ranges
        assertEquals(version1.hashCode(), version2.hashCode());
    }

    @Test
    void testProtocolToPackFormatMappings() {
        File packFile = tempDir.resolve("pack.zip").toFile();

        PackVersion v1_21_4 = new PackVersion("1.21.4", 46, 46, 999, packFile);
        PackVersion v1_21_2 = new PackVersion("1.21.2", 42, 42, 45, packFile);
        PackVersion v1_21 = new PackVersion("1.21", 34, 34, 41, packFile);
        PackVersion v1_20_5 = new PackVersion("1.20.5", 32, 32, 33, packFile);
        PackVersion v1_20_3 = new PackVersion("1.20.3", 22, 22, 31, packFile);
        PackVersion v1_20_2 = new PackVersion("1.20.2", 18, 18, 21, packFile);
        PackVersion v1_20 = new PackVersion("1.20", 15, 15, 17, packFile);

        assertTrue(v1_21_4.supportsProtocol(769), "Protocol 769 (1.21.4) should match format 46");
        assertTrue(v1_21_4.supportsProtocol(770), "Protocol 770+ should map to format 46 (future versions)");

        assertTrue(v1_21_2.supportsProtocol(768), "Protocol 768 (1.21.2-1.21.3) should match format 42");

        assertTrue(v1_21.supportsProtocol(767), "Protocol 767 (1.21-1.21.1) should match format 34");

        assertTrue(v1_20_5.supportsProtocol(766), "Protocol 766 (1.20.5-1.20.6) should match format 32");

        assertTrue(v1_20_3.supportsProtocol(765), "Protocol 765 (1.20.3-1.20.4) should match format 22");

        assertTrue(v1_20_2.supportsProtocol(764), "Protocol 764 (1.20.2) should match format 18");

        assertTrue(v1_20.supportsProtocol(763), "Protocol 763 (1.20-1.20.1) should match format 15");

        assertFalse(v1_21_4.supportsProtocol(768), "Protocol 768 should NOT match format 46");
        assertFalse(v1_20_3.supportsProtocol(766), "Protocol 766 should NOT match format 22");
    }
}
