package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
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

        assertEquals(2, sorted.size());

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
        assertEquals("1.21.3", lowest.getMinecraftVersion());
    }

    @Test
    void testHighestRangeEndsAt999() {
        // The 1.21.4+ pack should have max 999 (open-ended)
        PackVersionManager manager = new PackVersionManager(tempDir.toFile());
        manager.setSilentMode(true);
        manager.definePackVersions();

        PackVersion highest = manager.getAllVersions().stream()
            .max(Comparator.comparingInt(PackVersion::getMaxFormatInclusive))
            .orElse(null);

        assertNotNull(highest);
        assertEquals(999, highest.getMaxFormatInclusive(),
            "Highest pack version should have max_inclusive 999");
        assertEquals("1.21.4", highest.getMinecraftVersion());
    }

    @Test
    void testExpectedVersionRanges() {
        // Verify the two generated groups cover every supported format.
        PackVersionManager manager = new PackVersionManager(tempDir.toFile());
        manager.setSilentMode(true);
        manager.definePackVersions();

        PackVersion legacy = manager.getVersion("1.21.3");
        assertNotNull(legacy);
        assertEquals(15, legacy.getPackFormat());
        assertEquals(15, legacy.getMinFormatInclusive());
        assertEquals(45, legacy.getMaxFormatInclusive());

        PackVersion v1214 = manager.getVersion("1.21.4");
        assertNotNull(v1214);
        assertEquals(46, v1214.getPackFormat());
        assertEquals(46, v1214.getMinFormatInclusive());
        assertEquals(999, v1214.getMaxFormatInclusive());
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
    void testPackFormat18To64UsesSupportedFormatsObject() {
        JsonObject mcmeta = PackMcmetaUtils.createPackMcmeta(50, 50, 64, null);
        JsonObject pack = mcmeta.getAsJsonObject("pack");

        assertEquals(50, pack.get("pack_format").getAsInt());
        assertTrue(pack.has("supported_formats"));
        JsonObject supportedFormats = pack.getAsJsonObject("supported_formats");
        assertEquals(50, supportedFormats.get("min_inclusive").getAsInt());
        assertEquals(64, supportedFormats.get("max_inclusive").getAsInt());
        assertFalse(pack.has("min_format"));
        assertFalse(pack.has("max_format"));
    }

    @Test
    void testSinglePackFormat18To64OmitsRedundantSupportedFormats() {
        // Single-pack mode passes min/max of 0, so they default to packFormat.
        // A degenerate range (min == max) must not emit supported_formats, which would
        // only duplicate pack_format and narrow acceptance to exactly that format.
        JsonObject mcmeta = PackMcmetaUtils.createPackMcmeta(42, 0, 0, null);
        JsonObject pack = mcmeta.getAsJsonObject("pack");

        assertEquals(42, pack.get("pack_format").getAsInt());
        assertFalse(pack.has("supported_formats"));
        assertFalse(pack.has("min_format"));
        assertFalse(pack.has("max_format"));
    }

    @Test
    void testLegacyGroupMetadataSupportsFormats15Through45() {
        JsonObject mcmeta = PackMcmetaUtils.createPackMcmeta(15, 15, 45, null);
        JsonObject pack = mcmeta.getAsJsonObject("pack");

        // 1.20/1.20.1 read only pack_format; 1.20.2+ also honor supported_formats.
        assertEquals(15, pack.get("pack_format").getAsInt());
        JsonObject supportedFormats = pack.getAsJsonObject("supported_formats");
        assertEquals(15, supportedFormats.get("min_inclusive").getAsInt());
        assertEquals(45, supportedFormats.get("max_inclusive").getAsInt());
        assertFalse(pack.has("min_format"));
        assertFalse(pack.has("max_format"));
    }

    @Test
    void testModernGroupMetadataCrossesMetadataFormats() {
        JsonObject mcmeta = PackMcmetaUtils.createPackMcmeta(46, 46, 999, null);
        JsonObject pack = mcmeta.getAsJsonObject("pack");

        assertEquals(46, pack.get("pack_format").getAsInt());
        assertEquals(46, pack.get("min_format").getAsInt());
        assertEquals(999, pack.get("max_format").getAsInt());
        JsonObject supportedFormats = pack.getAsJsonObject("supported_formats");
        assertEquals(46, supportedFormats.get("min_inclusive").getAsInt());
        assertEquals(999, supportedFormats.get("max_inclusive").getAsInt());
    }

    @Test
    void testMergeOverlayEntriesPreservesPackMetadata() {
        JsonObject mcmeta = JsonParser.parseString("""
                {
                  "pack": {
                    "pack_format": 75,
                    "description": "Oraxen",
                    "min_format": 18,
                    "max_format": 999
                  },
                  "overlays": {
                    "entries": [
                      {
                        "formats": {"min_inclusive": 18, "max_inclusive": 45},
                        "directory": "overlay_1_20_2"
                      }
                    ]
                  }
                }
                """).getAsJsonObject();
        JsonArray importedEntries = JsonParser.parseString("""
                [
                  {
                    "formats": [35, 45],
                    "directory": "betterhud_1_21_2",
                    "min_format": 35,
                    "max_format": 45
                  }
                ]
                """).getAsJsonArray();

        PackMcmetaUtils.mergeOverlayEntries(mcmeta, importedEntries);

        JsonObject pack = mcmeta.getAsJsonObject("pack");
        assertEquals(75, pack.get("pack_format").getAsInt());
        assertEquals("Oraxen", pack.get("description").getAsString());
        assertEquals(18, pack.get("min_format").getAsInt());
        assertEquals(999, pack.get("max_format").getAsInt());

        JsonArray entries = mcmeta.getAsJsonObject("overlays").getAsJsonArray("entries");
        assertEquals(2, entries.size());
        assertEquals("overlay_1_20_2", entries.get(0).getAsJsonObject().get("directory").getAsString());
        assertEquals("betterhud_1_21_2", entries.get(1).getAsJsonObject().get("directory").getAsString());
    }

    @Test
    void testMergeOverlayEntriesIntoOutputConsumesNestedPackMcmetaOnly() throws Exception {
        List<VirtualFile> output = new ArrayList<>();
        output.add(jsonFile("", "pack.mcmeta", """
                {
                  "pack": {
                    "pack_format": 75,
                    "description": "Oraxen"
                  },
                  "overlays": {
                    "entries": [
                      {"directory": "oraxen_overlay"}
                    ]
                  }
                }
                """));
        output.add(jsonFile("betterhud", "pack.mcmeta", """
                {
                  "pack": {
                    "pack_format": 75,
                    "description": "Ignored"
                  },
                  "overlays": {
                    "entries": [
                      {"directory": "betterhud_overlay"}
                    ]
                  }
                }
                """));
        output.add(jsonFile("assets/minecraft/models/item", "stick.json", "{}"));

        PackMcmetaUtils.mergeOverlayEntriesIntoOutput(output, new JsonArray());

        assertEquals(2, output.size());
        assertTrue(output.stream().noneMatch(file -> file.getPath().equals("betterhud/pack.mcmeta")));

        byte[] mergedContent = output.stream()
                .filter(file -> file.getPath().equals("pack.mcmeta"))
                .findFirst()
                .orElseThrow()
                .getInputStream()
                .readAllBytes();
        JsonObject merged = JsonParser.parseString(new String(mergedContent, StandardCharsets.UTF_8)).getAsJsonObject();

        assertNotNull(merged);
        assertEquals("Oraxen", merged.getAsJsonObject("pack").get("description").getAsString());
        JsonArray entries = merged.getAsJsonObject("overlays").getAsJsonArray("entries");
        assertEquals(2, entries.size());
        assertEquals("oraxen_overlay", entries.get(0).getAsJsonObject().get("directory").getAsString());
        assertEquals("betterhud_overlay", entries.get(1).getAsJsonObject().get("directory").getAsString());
    }

    private static VirtualFile jsonFile(String parentFolder, String name, String content) {
        return new VirtualFile(parentFolder, name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
