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
        assertNotNull(version.getPackUUID());
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
}
