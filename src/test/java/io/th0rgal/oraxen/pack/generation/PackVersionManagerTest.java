package io.th0rgal.oraxen.pack.generation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class PackVersionManagerTest {

    @TempDir
    Path tempDir;

    private PackVersionManager manager;

    @BeforeEach
    void setUp() {
        manager = new PackVersionManager(tempDir.toFile());
        manager.setSilentMode(true); // Suppress logging in tests (no Bukkit available)
    }

    @Test
    void testDefinePackVersions() {
        assertFalse(manager.hasVersions());

        manager.definePackVersions();

        assertTrue(manager.hasVersions());
        assertEquals(2, manager.getVersionCount());

        Collection<PackVersion> versions = manager.getAllVersions();
        assertFalse(versions.isEmpty());

        // Verify the two generated version groups.
        PackVersion modern = manager.getVersion("1.21.4");
        PackVersion legacy = manager.getVersion("1.21.3");
        assertNotNull(modern);
        assertNotNull(legacy);
        assertEquals("pack_1_21_4.zip", modern.getPackFile().getName());
        assertEquals("pack_1_21_3.zip", legacy.getPackFile().getName());
        // 1.20/1.20.1 ignore supported_formats, so the canonical value must be format 15.
        assertEquals(15, legacy.getPackFormat());
    }

    @Test
    void testFindBestVersionForFormat() {
        manager.definePackVersions();

        // Formats through 1.21.3 use the legacy variant.
        PackVersion version = manager.findBestVersionForFormat(22);
        assertNotNull(version);
        assertEquals("1.21.3", version.getMinecraftVersion());

        // Format 46 should match 1.21.4 pack
        version = manager.findBestVersionForFormat(46);
        assertNotNull(version);
        assertEquals(46, version.getPackFormat());

        // All newer formats use the modern variant.
        version = manager.findBestVersionForFormat(55);
        assertNotNull(version);
        assertEquals("1.21.4", version.getMinecraftVersion());

        // Unknown high format should match latest pack (open-ended)
        version = manager.findBestVersionForFormat(999);
        assertNotNull(version); // Should return highest format pack
    }

    @Test
    void testFindBestVersionForProtocol() {
        manager.definePackVersions();

        // Protocol 765 (1.20.3-1.20.4) should map to the legacy pack.
        PackVersion version = manager.findBestVersionForProtocol(765);
        assertNotNull(version);
        assertEquals("1.21.3", version.getMinecraftVersion());

        // Protocol 768 (1.21.2-1.21.3) remains on the legacy pack.
        version = manager.findBestVersionForProtocol(768);
        assertNotNull(version);
        assertEquals("1.21.3", version.getMinecraftVersion());

        // Protocol 769 (1.21.4) and every newer known protocol use the modern pack.
        version = manager.findBestVersionForProtocol(769);
        assertNotNull(version);
        assertEquals("1.21.4", version.getMinecraftVersion());
        for (int protocol : new int[] {771, 772, 773, 774, 775, 776}) {
            assertEquals("1.21.4", manager.findBestVersionForProtocol(protocol).getMinecraftVersion());
        }
    }

    @Test
    void testSetServerPackVersion() {
        manager.definePackVersions();

        manager.setServerPackVersion("1.20.3");
        PackVersion serverVersion = manager.getServerPackVersion();
        assertNotNull(serverVersion);

        // If exact match not found, should use highest format
        manager.setServerPackVersion("999.999.999");
        serverVersion = manager.getServerPackVersion();
        assertNotNull(serverVersion);
    }

    @Test
    void testSetServerPackVersionWithNormalizedVersion() {
        manager.definePackVersions();

        // Versions without an exact group key resolve by their pack format.
        manager.setServerPackVersion("1.21.0");
        PackVersion serverVersion = manager.getServerPackVersion();
        assertNotNull(serverVersion);
        assertEquals("1.21.3", serverVersion.getMinecraftVersion());

        // Test that "1.20.0" maps to the legacy group.
        manager.setServerPackVersion("1.20.0");
        serverVersion = manager.getServerPackVersion();
        assertNotNull(serverVersion);
        assertEquals("1.21.3", serverVersion.getMinecraftVersion());

        // Test that "1.21.1" maps to the legacy group.
        manager.setServerPackVersion("1.21.1");
        serverVersion = manager.getServerPackVersion();
        assertNotNull(serverVersion);
        assertEquals("1.21.3", serverVersion.getMinecraftVersion());

        // Test that build-style trailing zeros normalize correctly: "1.20.5.0" -> "1.20.5"
        manager.setServerPackVersion("1.20.5.0");
        serverVersion = manager.getServerPackVersion();
        assertNotNull(serverVersion);
        assertEquals("1.21.3", serverVersion.getMinecraftVersion());
    }

    @Test
    void testGetServerPackVersionFallback() {
        manager.definePackVersions();

        // Without setting server version, should return highest format pack
        PackVersion serverVersion = manager.getServerPackVersion();
        assertNotNull(serverVersion);
    }

    @Test
    void testClear() {
        manager.definePackVersions();
        assertTrue(manager.hasVersions());

        manager.clear();
        assertFalse(manager.hasVersions());
        assertEquals(0, manager.getVersionCount());
    }

    @Test
    void testGetAllVersionsImmutable() {
        manager.definePackVersions();

        Collection<PackVersion> versions = manager.getAllVersions();
        int originalSize = versions.size();

        assertThrows(UnsupportedOperationException.class, () -> {
            versions.clear();
        });

        assertEquals(originalSize, manager.getAllVersions().size());
    }

    @Test
    void testMaxNaturalOrderingReturnsHighestFormat() {
        manager.definePackVersions();

        java.util.List<PackVersion> versions = new java.util.ArrayList<>(manager.getAllVersions());

        PackVersion highest = versions.stream()
            .max(java.util.Comparator.naturalOrder())
            .orElse(null);

        assertNotNull(highest);
        assertEquals(46, highest.getPackFormat());
        assertEquals("1.21.4", highest.getMinecraftVersion());
    }

    @Test
    void testFindBestVersionForFormatOutsideAllRanges() {
        manager.definePackVersions();

        PackVersion version = manager.findBestVersionForFormat(5);
        assertNull(version, "Format 5 is not in any pack range, should return null");

        version = manager.findBestVersionForFormat(14);
        assertNull(version, "Format 14 (pre-1.20) is not in any pack range, should return null");
    }

    @Test
    void testFindBestVersionForProtocolOutsideAllRanges() {
        manager.definePackVersions();

        PackVersion version = manager.findBestVersionForProtocol(47);
        assertNull(version, "Protocol 47 (1.8) maps to format 1, not in any pack range");
    }

    @Test
    void testCompareToConsistentWithNaturalOrdering() {
        manager.definePackVersions();

        java.util.List<PackVersion> versions = new java.util.ArrayList<>(manager.getAllVersions());
        java.util.List<PackVersion> sorted = versions.stream()
            .sorted(java.util.Comparator.naturalOrder())
            .toList();

        for (int i = 1; i < sorted.size(); i++) {
            PackVersion prev = sorted.get(i - 1);
            PackVersion curr = sorted.get(i);
            assertTrue(prev.getPackFormat() <= curr.getPackFormat(),
                "Natural ordering should sort by pack format ascending: " +
                prev.getMinecraftVersion() + " (format " + prev.getPackFormat() + ") should be <= " +
                curr.getMinecraftVersion() + " (format " + curr.getPackFormat() + ")");
        }
    }
}
