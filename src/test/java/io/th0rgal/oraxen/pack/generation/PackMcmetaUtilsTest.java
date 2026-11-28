package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PackMcmetaUtilsTest {

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
        assertEquals("betterhud_1_21_2", entries.get(0).getAsJsonObject().get("directory").getAsString());
        assertEquals("overlay_1_20_2", entries.get(1).getAsJsonObject().get("directory").getAsString());
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
        assertEquals("betterhud_overlay", entries.get(0).getAsJsonObject().get("directory").getAsString());
        assertEquals("oraxen_overlay", entries.get(1).getAsJsonObject().get("directory").getAsString());
    }

    @Test
    void mixedOverlayFormatsKeepLegacyRangesForEveryEntryAndPreserveMinorBounds() {
        JsonArray original = JsonParser.parseString("""
                [
                  {"directory":"old","formats":[46,64]},
                  {"directory":"new","min_format":[97,1],"max_format":[97,2]}
                ]
                """).getAsJsonArray();
        JsonArray normalized = PackMcmetaUtils.normalizeOverlayEntries(original);
        JsonObject oldEntry = normalized.get(0).getAsJsonObject();
        JsonObject newEntry = normalized.get(1).getAsJsonObject();
        assertEquals(46, oldEntry.get("min_format").getAsInt());
        assertEquals(64, oldEntry.get("max_format").getAsInt());
        assertEquals(JsonParser.parseString("{\"min_inclusive\":46,\"max_inclusive\":64}"), oldEntry.get("formats"));
        assertEquals(JsonParser.parseString("{\"min_inclusive\":97,\"max_inclusive\":97}"), newEntry.get("formats"));
        assertEquals(JsonParser.parseString("[97,1]"), newEntry.get("min_format"));
        assertEquals(JsonParser.parseString("[97,2]"), newEntry.get("max_format"));
        assertFalse(original.get(1).getAsJsonObject().has("formats"));

        JsonObject metadata = new JsonObject();
        JsonObject overlays = new JsonObject();
        JsonArray existing = new JsonArray();
        existing.add(original.get(1).deepCopy());
        overlays.add("entries", existing);
        metadata.add("overlays", overlays);
        JsonArray imported = new JsonArray();
        imported.add(original.get(0).deepCopy());
        PackMcmetaUtils.mergeOverlayEntries(metadata, imported);
        assertEquals(normalized, metadata.getAsJsonObject("overlays").getAsJsonArray("entries"));
    }

    @Test
    void modernOnlyOverlayFormatsRemoveLegacyRangesFromEveryEntry() {
        JsonArray entries = JsonParser.parseString("""
                [
                  {"directory":"a","formats":[97,97]},
                  {"directory":"b","formats":{"min_inclusive":98,"max_inclusive":99},"min_format":[98,1],"max_format":[99,0]}
                ]
                """).getAsJsonArray();
        JsonArray normalized = PackMcmetaUtils.normalizeOverlayEntries(entries);
        for (var entry : normalized) assertFalse(entry.getAsJsonObject().has("formats"));
        assertEquals(97, normalized.get(0).getAsJsonObject().get("max_format").getAsInt());
        assertEquals(JsonParser.parseString("[99,0]"), normalized.get(1).getAsJsonObject().get("max_format"));
    }

    private static VirtualFile jsonFile(String parentFolder, String name, String content) {
        return new VirtualFile(parentFolder, name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
