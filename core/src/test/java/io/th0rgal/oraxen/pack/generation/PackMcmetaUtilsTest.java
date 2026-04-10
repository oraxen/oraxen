package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that validate the supported_formats range data used by PackMcmetaUtils
 * is consistent with the pack version definitions in PackVersionManager.
 *
 * Direct testing of PackMcmetaUtils.createPackMcmeta() requires Gson/Bukkit
 * which are not on the test classpath, so we validate the underlying data model instead.
 */
class PackMcmetaUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void testPackVersionRangesAreContiguous() {
        // The supported_formats ranges in PackVersionManager (which feed into
        // PackMcmetaUtils.createPackMcmeta for multi-version packs and
        // updatePackMcmetaFile for single-pack mode) must be contiguous.
        PackVersionManager manager = new PackVersionManager(tempDir.toFile());
        manager.setSilentMode(true);
        manager.definePackVersions();

        Collection<PackVersion> versions = manager.getAllVersions();
        List<PackVersion> sorted = versions.stream()
            .sorted(Comparator.comparingInt(PackVersion::getMinFormatInclusive))
            .toList();

        assertTrue(sorted.size() >= 7, "Should have at least 7 pack versions defined");

        // Each version's max + 1 should equal the next version's min (no gaps or overlaps)
        for (int i = 0; i < sorted.size() - 1; i++) {
            PackVersion current = sorted.get(i);
            PackVersion next = sorted.get(i + 1);

            assertEquals(current.getMaxFormatInclusive() + 1, next.getMinFormatInclusive(),
                "Gap or overlap between " + current.getMinecraftVersion()
                    + " [" + current.getMinFormatInclusive() + "-" + current.getMaxFormatInclusive() + "]"
                    + " and " + next.getMinecraftVersion()
                    + " [" + next.getMinFormatInclusive() + "-" + next.getMaxFormatInclusive() + "]");
        }
    }

    @Test
    void testAllPackVersionFormatsWithinDeclaredRange() {
        PackVersionManager manager = new PackVersionManager(tempDir.toFile());
        manager.setSilentMode(true);
        manager.definePackVersions();

        for (PackVersion pv : manager.getAllVersions()) {
            assertTrue(pv.getPackFormat() >= pv.getMinFormatInclusive(),
                pv.getMinecraftVersion() + ": pack_format " + pv.getPackFormat()
                    + " should be >= min_inclusive " + pv.getMinFormatInclusive());
            assertTrue(pv.getPackFormat() <= pv.getMaxFormatInclusive(),
                pv.getMinecraftVersion() + ": pack_format " + pv.getPackFormat()
                    + " should be <= max_inclusive " + pv.getMaxFormatInclusive());
        }
    }

    @Test
    void testLowestRangeStartsAtFormat15() {
        // The lowest pack version (1.20) should start at format 15
        PackVersionManager manager = new PackVersionManager(tempDir.toFile());
        manager.setSilentMode(true);
        manager.definePackVersions();

        PackVersion lowest = manager.getAllVersions().stream()
            .min(Comparator.comparingInt(PackVersion::getMinFormatInclusive))
            .orElse(null);

        assertNotNull(lowest);
        assertEquals(15, lowest.getMinFormatInclusive(),
            "Lowest pack version should start at format 15 (1.20)");
        assertEquals("1.20", lowest.getMinecraftVersion());
    }

    @Test
    void testHighestRangeEndsAt999() {
        // The highest pack version (1.21.4) should have max 999 (open-ended)
        PackVersionManager manager = new PackVersionManager(tempDir.toFile());
        manager.setSilentMode(true);
        manager.definePackVersions();

        PackVersion highest = manager.getAllVersions().stream()
            .max(Comparator.comparingInt(PackVersion::getMaxFormatInclusive))
            .orElse(null);

        assertNotNull(highest);
        assertEquals(999, highest.getMaxFormatInclusive(),
            "Highest pack version should have max_inclusive 999");
        assertEquals("26.1", highest.getMinecraftVersion());
    }

    @Test
    void testExpectedVersionRanges() {
        // Verify specific known ranges for each pack version
        PackVersionManager manager = new PackVersionManager(tempDir.toFile());
        manager.setSilentMode(true);
        manager.definePackVersions();

        // 1.20: format 15, range [15, 17]
        PackVersion v120 = manager.getVersion("1.20");
        assertNotNull(v120);
        assertEquals(15, v120.getMinFormatInclusive());
        assertEquals(17, v120.getMaxFormatInclusive());

        // 1.20.2: format 18, range [18, 21]
        PackVersion v1202 = manager.getVersion("1.20.2");
        assertNotNull(v1202);
        assertEquals(18, v1202.getMinFormatInclusive());
        assertEquals(21, v1202.getMaxFormatInclusive());

        // 1.20.3: format 22, range [22, 31]
        PackVersion v1203 = manager.getVersion("1.20.3");
        assertNotNull(v1203);
        assertEquals(22, v1203.getMinFormatInclusive());
        assertEquals(31, v1203.getMaxFormatInclusive());

        // 1.20.5: format 32, range [32, 33]
        PackVersion v1205 = manager.getVersion("1.20.5");
        assertNotNull(v1205);
        assertEquals(32, v1205.getMinFormatInclusive());
        assertEquals(33, v1205.getMaxFormatInclusive());

        // 1.21: format 34, range [34, 41]
        PackVersion v121 = manager.getVersion("1.21");
        assertNotNull(v121);
        assertEquals(34, v121.getMinFormatInclusive());
        assertEquals(41, v121.getMaxFormatInclusive());

        // 1.21.2: format 42, range [42, 45]
        PackVersion v1212 = manager.getVersion("1.21.2");
        assertNotNull(v1212);
        assertEquals(42, v1212.getMinFormatInclusive());
        assertEquals(45, v1212.getMaxFormatInclusive());

        // 1.21.4: format 46, range [46, 54]
        PackVersion v1214 = manager.getVersion("1.21.4");
        assertNotNull(v1214);
        assertEquals(46, v1214.getMinFormatInclusive());
        assertEquals(54, v1214.getMaxFormatInclusive());

        // 1.21.5: format 55, range [55, 62]
        PackVersion v1215 = manager.getVersion("1.21.5");
        assertNotNull(v1215);
        assertEquals(55, v1215.getMinFormatInclusive());
        assertEquals(62, v1215.getMaxFormatInclusive());

        // 1.21.9: format 69, range [69, 74]
        PackVersion v1219 = manager.getVersion("1.21.9");
        assertNotNull(v1219);
        assertEquals(69, v1219.getMinFormatInclusive());
        assertEquals(74, v1219.getMaxFormatInclusive());

        // 1.21.11: format 75, range [75, 83]
        PackVersion v12111 = manager.getVersion("1.21.11");
        assertNotNull(v12111);
        assertEquals(75, v12111.getMinFormatInclusive());
        assertEquals(83, v12111.getMaxFormatInclusive());

        // 26.1: format 84, range [84, 999]
        PackVersion v261 = manager.getVersion("26.1");
        assertNotNull(v261);
        assertEquals(84, v261.getMinFormatInclusive());
        assertEquals(999, v261.getMaxFormatInclusive());
    }

    @Test
    void testPackFormat65PlusUsesMinAndMaxFormat() {
        JsonObject mcmeta = PackMcmetaUtils.createPackMcmeta(75, 0, 0, null);
        JsonObject pack = mcmeta.getAsJsonObject("pack");

        assertEquals(75, pack.get("pack_format").getAsInt());
        assertEquals(75, pack.get("min_format").getAsInt());
        assertEquals(75, pack.get("max_format").getAsInt());
        assertFalse(pack.has("supported_formats"));
    }

    @Test
    void testPackFormat18To64UsesSupportedFormatsArray() {
        JsonObject mcmeta = PackMcmetaUtils.createPackMcmeta(50, 50, 64, null);
        JsonObject pack = mcmeta.getAsJsonObject("pack");

        assertEquals(50, pack.get("pack_format").getAsInt());
        assertTrue(pack.has("supported_formats"));
        assertEquals(50, pack.getAsJsonArray("supported_formats").get(0).getAsInt());
        assertEquals(64, pack.getAsJsonArray("supported_formats").get(1).getAsInt());
        assertFalse(pack.has("min_format"));
        assertFalse(pack.has("max_format"));
    }
}
