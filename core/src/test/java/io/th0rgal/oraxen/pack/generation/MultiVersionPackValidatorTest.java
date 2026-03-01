package io.th0rgal.oraxen.pack.generation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultiVersionPackValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void testPackVersionManagerVersionRanges() {
        File packFolder = tempDir.toFile();
        PackVersionManager manager = new PackVersionManager(packFolder);
        manager.setSilentMode(true);
        manager.definePackVersions();

        var versions = manager.getAllVersions();
        assertFalse(versions.isEmpty(), "Should have defined pack versions");

        for (PackVersion v : versions) {
            assertTrue(v.getMinFormatInclusive() <= v.getMaxFormatInclusive(),
                    String.format("Version %s has invalid range: min=%d > max=%d",
                            v.getMinecraftVersion(), v.getMinFormatInclusive(), v.getMaxFormatInclusive()));

            assertTrue(v.getPackFormat() >= v.getMinFormatInclusive(),
                    String.format("Version %s pack_format=%d is below min=%d",
                            v.getMinecraftVersion(), v.getPackFormat(), v.getMinFormatInclusive()));

            assertTrue(v.getPackFormat() <= v.getMaxFormatInclusive(),
                    String.format("Version %s pack_format=%d is above max=%d",
                            v.getMinecraftVersion(), v.getPackFormat(), v.getMaxFormatInclusive()));
        }
    }

    @Test
    void testPackVersionManagerNoOverlappingRanges() {
        File packFolder = tempDir.toFile();
        PackVersionManager manager = new PackVersionManager(packFolder);
        manager.setSilentMode(true);
        manager.definePackVersions();

        var versions = manager.getAllVersions();

        for (PackVersion v1 : versions) {
            for (PackVersion v2 : versions) {
                if (v1 == v2) continue;

                boolean rangesOverlap = v1.getMinFormatInclusive() <= v2.getMaxFormatInclusive() &&
                        v1.getMaxFormatInclusive() >= v2.getMinFormatInclusive();

                assertFalse(rangesOverlap, String.format(
                        "Pack versions %s (range [%d,%d]) and %s (range [%d,%d]) have overlapping ranges",
                        v1.getMinecraftVersion(), v1.getMinFormatInclusive(), v1.getMaxFormatInclusive(),
                        v2.getMinecraftVersion(), v2.getMinFormatInclusive(), v2.getMaxFormatInclusive()
                ));
            }
        }
    }

    @Test
    void testPackVersionManagerCompleteCoverage() {
        File packFolder = tempDir.toFile();
        PackVersionManager manager = new PackVersionManager(packFolder);
        manager.setSilentMode(true);
        manager.definePackVersions();

        var versions = manager.getAllVersions();

        int minFormat = versions.stream().mapToInt(PackVersion::getMinFormatInclusive).min().orElse(0);
        int maxFormat = versions.stream().mapToInt(PackVersion::getMaxFormatInclusive).max().orElse(0);

        for (int format = minFormat; format <= maxFormat; format++) {
            int matchCount = 0;
            for (PackVersion v : versions) {
                if (v.supportsFormat(format)) {
                    matchCount++;
                }
            }

            assertEquals(1, matchCount, String.format(
                    "Format %d should be covered by exactly one pack version, but found %d matches",
                    format, matchCount
            ));
        }
    }

    @Test
    void testPackVersionManagerServerVersionFallback() {
        File packFolder = tempDir.toFile();
        PackVersionManager manager = new PackVersionManager(packFolder);
        manager.setSilentMode(true);
        manager.definePackVersions();

        manager.setServerPackVersion("1.21.4");
        PackVersion serverPack = manager.getServerPackVersion();
        assertNotNull(serverPack, "Server pack version should not be null");
        assertEquals("1.21.4", serverPack.getMinecraftVersion());
    }

    @Test
    void testPackVersionManagerServerVersionNormalization() {
        File packFolder = tempDir.toFile();
        PackVersionManager manager = new PackVersionManager(packFolder);
        manager.setSilentMode(true);
        manager.definePackVersions();

        manager.setServerPackVersion("1.21.0");
        PackVersion serverPack = manager.getServerPackVersion();
        assertNotNull(serverPack, "Server pack version should not be null after normalizing 1.21.0 -> 1.21");
    }
}
