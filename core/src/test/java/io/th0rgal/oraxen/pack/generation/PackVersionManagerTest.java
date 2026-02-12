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
    }

    @Test
    void testDefinePackVersions() {
        assertFalse(manager.hasVersions());

        manager.definePackVersions();

        assertTrue(manager.hasVersions());
        assertTrue(manager.getVersionCount() > 0);

        Collection<PackVersion> versions = manager.getAllVersions();
        assertFalse(versions.isEmpty());

        // Verify we have expected versions
        assertNotNull(manager.getVersion("1.21.4"));
        assertNotNull(manager.getVersion("1.20.3")); // 1.20.3 covers 1.20.3-1.20.4 range
        assertNotNull(manager.getVersion("1.20"));
    }

    @Test
    void testFindBestVersionForFormat() {
        manager.definePackVersions();

        // Format 22 should match 1.20.3 pack (format 22, range 22-31)
        PackVersion version = manager.findBestVersionForFormat(22);
        assertNotNull(version);
        assertTrue(version.supportsFormat(22));

        // Format 46 should match 1.21.4+ pack
        version = manager.findBestVersionForFormat(46);
        assertNotNull(version);
        assertEquals(46, version.getPackFormat());

        // Unknown format should return null or best match
        version = manager.findBestVersionForFormat(999);
        assertNotNull(version); // Should return highest format pack
    }

    @Test
    void testFindBestVersionForProtocol() {
        manager.definePackVersions();

        // Protocol 765 (1.20.3) should map to appropriate pack
        PackVersion version = manager.findBestVersionForProtocol(765);
        assertNotNull(version);

        // Protocol 779 (1.21.4+) should map to latest pack
        version = manager.findBestVersionForProtocol(779);
        assertNotNull(version);
    }

    @Test
    void testSetServerPackVersion() {
        manager.definePackVersions();

        manager.setServerPackVersion("1.20.4");
        PackVersion serverVersion = manager.getServerPackVersion();
        assertNotNull(serverVersion);

        // If exact match not found, should use highest format
        manager.setServerPackVersion("999.999.999");
        serverVersion = manager.getServerPackVersion();
        assertNotNull(serverVersion);
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

        // Trying to modify should throw exception
        assertThrows(UnsupportedOperationException.class, () -> {
            versions.clear();
        });

        // Size should remain the same
        assertEquals(originalSize, manager.getAllVersions().size());
    }
}
