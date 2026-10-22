package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PackImportCompositionTest {
    @TempDir Path tempDir;
    private MockedStatic<OraxenPlugin> plugins;
    private MockedStatic<Logs> logs;
    private YamlConfiguration settings;

    @BeforeEach void setup() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getConfigsManager()).thenReturn(configs);
        settings = new YamlConfiguration();
        settings.set(Settings.MERGE_DUPLICATE_FONTS.getPath(), true);
        when(configs.getSettings()).thenReturn(settings);
        plugins = mockStatic(OraxenPlugin.class);
        plugins.when(OraxenPlugin::get).thenReturn(plugin);
        logs = mockStatic(Logs.class);
        Settings.invalidateCache();
    }
    @AfterEach void cleanup() { Settings.invalidateCache(); logs.close(); plugins.close(); }

    @Test void composesNativeResourcesAndRetainsValidLowerFiles() throws Exception {
        write("a", Map.of(
                "assets/test/lang/en_us.json", "{\"low\":\"low\",\"shared\":\"low\"}",
                "assets/test/font/default.json", "{\"providers\":[{\"type\":\"reference\",\"id\":\"test:low\",\"filter\":{\"uniform\":true}}]}",
                "assets/test/sounds.json", "{\"append\":{\"sounds\":[\"low\"]},\"replace\":{\"sounds\":[\"low\"]}}",
                "assets/test/atlases/blocks.json", "{\"sources\":[{\"type\":\"single\",\"resource\":\"test:low\"}]}",
                "assets/test/textures/a.png", "low",
                "assets/test/textures/a.png.mcmeta", "{\"animation\":{}}"));
        write("b", Map.of(
                "assets/test/lang/en_us.json", "{\"high\":\"high\",\"shared\":\"high\"}",
                "assets/test/font/default.json", "{\"providers\":[{\"type\":\"reference\",\"id\":\"test:high\"}]}",
                "assets/test/sounds.json", "{\"append\":{\"sounds\":[\"high\"]},\"replace\":{\"replace\":true,\"sounds\":[\"high\"]}}",
                "assets/test/atlases/blocks.json", "{\"sources\":[{\"type\":\"filter\",\"pattern\":{}}]}",
                "assets/test/textures/a.png", "high"));
        write("c", Map.of("assets/test/font/default.json", "{\"providers\":false}", "assets/test/lang/en_us.json", "[invalid"));
        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());
        assertEquals("high", json(files, "assets/test/lang/en_us.json").get("shared").getAsString());
        assertTrue(json(files, "assets/test/lang/en_us.json").has("low"));
        JsonArray providers = json(files, "assets/test/font/default.json").getAsJsonArray("providers");
        assertEquals("test:high", providers.get(0).getAsJsonObject().get("id").getAsString());
        assertTrue(providers.get(1).getAsJsonObject().has("filter"));
        assertEquals(2, json(files, "assets/test/sounds.json").getAsJsonObject("append").getAsJsonArray("sounds").size());
        assertEquals(1, json(files, "assets/test/sounds.json").getAsJsonObject("replace").getAsJsonArray("sounds").size());
        assertEquals("single", json(files, "assets/test/atlases/blocks.json").getAsJsonArray("sources").get(0).getAsJsonObject().get("type").getAsString());
        assertFalse(files.containsKey("assets/test/textures/a.png.mcmeta"));
    }

    @Test void partitionsOverlappingOverlaysAndKeepsHigherBaseAboveLowerOverlay() throws Exception {
        write("a", Map.of("pack.mcmeta", metadata("low", 46, 70),
                "assets/test/lang/en_us.json", "{\"lowBase\":\"yes\"}",
                "low/assets/test/lang/en_us.json", "{\"lowOverlay\":\"yes\",\"shared\":\"low\"}"));
        write("b", Map.of("pack.mcmeta", metadata("high", 60, 80),
                "assets/test/lang/en_us.json", "{\"higherBase\":\"yes\",\"shared\":\"high base\"}",
                "high/assets/test/lang/en_us.json", "{\"higherOverlay\":\"yes\",\"shared\":\"high overlay\"}"));
        PackMerger merger = new PackMerger(tempDir.toFile());
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        JsonArray entries = merger.getMergedOverlayEntries();
        JsonObject before = active(files, entries, 50);
        assertTrue(before.has("lowOverlay"));
        assertFalse(before.has("lowBase"));
        assertEquals("high base", before.get("shared").getAsString());
        JsonObject overlap = active(files, entries, 65);
        assertTrue(overlap.has("lowOverlay"));
        assertTrue(overlap.has("higherOverlay"));
        assertFalse(overlap.has("higherBase"));
        JsonObject after = active(files, entries, 75);
        assertTrue(after.has("lowBase"));
        assertFalse(after.has("lowOverlay"));
        assertTrue(after.has("higherOverlay"));

        List<VirtualFile> higher = List.of(vfile("pack.mcmeta", metadata("local", 64, 66)),
                vfile("assets/test/lang/en_us.json", "{\"localBase\":\"yes\"}"),
                vfile("local/assets/test/lang/en_us.json", "{\"localOverlay\":\"yes\"}"));
        files = contents(merger.mergeWithHigherPriorityFiles(higher));
        overlap = active(files, merger.getMergedOverlayEntries(), 65);
        assertTrue(overlap.has("localOverlay"));
        assertFalse(overlap.has("localBase"));
        assertTrue(overlap.has("higherOverlay"));
    }

    @Test void distinguishesEmbeddedPacksFromDeclaredOverlaysAndScopesFilters() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("wrapper/pack.mcmeta", metadata("overlay", 46, 80));
        entries.put("wrapper/assets/test/lang/en_us.json", "{\"root\":\"yes\"}");
        entries.put("wrapper/overlay/assets/test/lang/en_us.json", "{\"overlay\":\"yes\"}");
        entries.put("wrapper/nested/pack.mcmeta", "{\"language\":{\"test\":{\"name\":\"Test\"}},\"filter\":{\"block\":[{\"namespace\":\"test\",\"path\":\"lang/en_us.json\"}]}}");
        entries.put("wrapper/nested/assets/test/lang/en_us.json", "{\"nested\":\"yes\"}");
        Path zip = tempDir.resolve("uploads/a.zip");
        Files.createDirectories(zip.getParent());
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (var entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        PackMerger merger = new PackMerger(tempDir.toFile());
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        assertEquals(JsonParser.parseString("{\"nested\":\"yes\"}"), json(files, "assets/test/lang/en_us.json"));
        assertEquals(JsonParser.parseString("{\"nested\":\"yes\"}"), active(files, merger.getMergedOverlayEntries(), 60));
        assertTrue(merger.getMergedLanguageMetadata().has("test"));
        assertEquals(1, merger.getMergedFilterMetadata().getAsJsonArray("block").size());
    }

    @Test void guardsLowerBlockstateDefaultsAcrossSeveralHigherLayers() {
        String path = "assets/test/blockstates/test.json";
        JsonObject lower = JsonParser.parseString("{\"variants\":{\"\":{\"model\":\"test:base\"}}}").getAsJsonObject();
        JsonObject middle = JsonParser.parseString("{\"variants\":{\"facing=north\":{\"model\":\"test:north\"}}}").getAsJsonObject();
        JsonObject higher = JsonParser.parseString("{\"variants\":{\"powered=true\":{\"model\":\"test:powered\"}}}").getAsJsonObject();
        JsonObject merged = PackAssetMerger.mergeJson(path, PackAssetMerger.mergeJson(path, lower, middle), higher);
        assertEquals("test:powered", modelFor(merged, Map.of("facing", "north", "powered", "true")));
        assertEquals("test:north", modelFor(merged, Map.of("facing", "north", "powered", "false")));
        assertEquals("test:base", modelFor(merged, Map.of("facing", "south", "powered", "false")));
    }

    @Test void keepsSoundEventMetadataAndResetWhenAppending() {
        JsonObject low = JsonParser.parseString("{\"event\":{\"replace\":true,\"subtitle\":\"low\",\"sounds\":[\"a\"]}}").getAsJsonObject();
        JsonObject high = JsonParser.parseString("{\"event\":{\"replace\":false,\"subtitle\":\"high\",\"sounds\":[\"b\"]}}").getAsJsonObject();
        JsonObject event = PackAssetMerger.mergeJson("assets/test/sounds.json", low, high).getAsJsonObject("event");
        assertEquals("low", event.get("subtitle").getAsString());
        assertTrue(event.get("replace").getAsBoolean());
        assertEquals(JsonParser.parseString("[\"a\",\"b\"]"), event.get("sounds"));
    }

    @Test void convertsLegacyRulesBeforeCrossSourcePriorityResolution() throws Exception {
        settings.set(Settings.MERGE_DUPLICATES.getPath(), true);
        Settings.invalidateCache();
        String legacy = "{\"parent\":\"minecraft:item/generated\",\"overrides\":[{\"predicate\":{\"custom_model_data\":5},\"model\":\"test:high_legacy\"}]}";
        write("a", Map.of("assets/minecraft/items/paper.json", modernItem("test:low_modern")));
        write("b", Map.of("assets/minecraft/models/item/paper.json", legacy));
        PackMerger merger = new PackMerger(tempDir.toFile());
        merger.setConvertLegacyItemModels(true);
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        assertEquals("test:high_legacy", itemAt(json(files, "assets/minecraft/items/paper.json").getAsJsonObject("model"), 5));
        assertEquals("test:fallback", itemAt(json(files, "assets/minecraft/items/paper.json").getAsJsonObject("model"), 0));

        write("b", Map.of("assets/minecraft/items/paper.json", modernItem("test:own_modern")));
        files = contents(merger.mergeUploadedPacks());
        assertEquals("test:own_modern", itemAt(json(files, "assets/minecraft/items/paper.json").getAsJsonObject("model"), 5));
    }

    @Test void convertsLegacyRulesWhenDuplicateMergingIsDisabled() throws Exception {
        settings.set(Settings.MERGE_DUPLICATES.getPath(), false);
        Settings.invalidateCache();
        write("a", Map.of("assets/minecraft/models/item/paper.json",
                "{\"parent\":\"minecraft:item/generated\",\"overrides\":[{\"predicate\":{\"custom_model_data\":5},\"model\":\"test:converted\"}]}"));
        PackMerger merger = new PackMerger(tempDir.toFile());
        merger.setConvertLegacyItemModels(true);
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        JsonObject model = json(files, "assets/minecraft/items/paper.json").getAsJsonObject("model");
        assertEquals("test:converted", itemAt(model, 5));
        assertEquals("minecraft:item/paper", itemAt(model, 0));
    }

    @Test void malformedLocalItemDuplicatesKeepGeneratedFilesAndContinueMerging() throws Exception {
        settings.set(Settings.MERGE_DUPLICATES.getPath(), true);
        Settings.invalidateCache();
        String modernPath = "assets/minecraft/items/paper.json";
        String legacyPath = "assets/minecraft/models/item/paper.json";
        String generatedModern = modernItem("test:generated");
        String generatedLegacy = """
                {"parent":"minecraft:item/generated","overrides":[
                  {"predicate":{"custom_model_data":5},"model":"test:generated"}]}
                """;
        for (String malformed : List.of("{\"model\":\"broken\"}", """
                {"model":{"type":"minecraft:range_dispatch","property":"minecraft:custom_model_data",
                  "entries":[{"threshold":"broken","model":{"type":"minecraft:empty"}}]}}
                """)) {
            Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeWithHigherPriorityFiles(List.of(
                    vfile(modernPath, generatedModern), vfile(legacyPath, generatedLegacy),
                    vfile(modernPath, malformed), vfile(legacyPath, "{\"overrides\":false}"),
                    vfile("assets/test/lang/en_us.json", "{\"continued\":\"yes\"}"))));
            assertEquals(JsonParser.parseString(generatedModern), json(files, modernPath));
            assertEquals(JsonParser.parseString(generatedLegacy), json(files, legacyPath));
            assertEquals("yes", json(files, "assets/test/lang/en_us.json").get("continued").getAsString());
        }
    }

    @Test void mergesGeneratedAndLocalItemRulesBeforeCollapsingPaths() throws Exception {
        settings.set(Settings.MERGE_DUPLICATES.getPath(), true);
        Settings.invalidateCache();
        String legacyPath = "assets/minecraft/models/item/paper.json";
        String modernPath = "assets/minecraft/items/paper.json";
        String generatedLegacy = """
                {"parent":"minecraft:item/generated","overrides":[
                  {"predicate":{"custom_model_data":5},"model":"test:generated"}]}
                """;
        String localLegacy = """
                {"parent":"minecraft:item/handheld","overrides":[
                  {"predicate":{"custom_model_data":5},"model":"test:local_conflict"},
                  {"predicate":{"custom_model_data":10},"model":"test:local"}]}
                """;
        JsonObject localDefinition = JsonParser.parseString(modernItem("test:local_conflict")).getAsJsonObject();
        JsonObject localEntry = localDefinition.getAsJsonObject("model").getAsJsonArray("entries").get(0).getAsJsonObject().deepCopy();
        localEntry.addProperty("threshold", 10);
        localEntry.getAsJsonObject("model").addProperty("model", "test:local");
        localDefinition.getAsJsonObject("model").getAsJsonArray("entries").add(localEntry);
        String localModern = localDefinition.toString();
        PackMerger merger = new PackMerger(tempDir.toFile());
        Map<String, String> files = contents(merger.mergeWithHigherPriorityFiles(List.of(
                vfile(legacyPath, generatedLegacy), vfile(modernPath, modernItem("test:generated")),
                vfile(legacyPath, localLegacy), vfile(modernPath, localModern))));
        JsonObject legacy = json(files, legacyPath);
        assertEquals("minecraft:item/generated", legacy.get("parent").getAsString());
        assertEquals(2, legacy.getAsJsonArray("overrides").size());
        assertEquals("test:generated", legacy.getAsJsonArray("overrides").get(0).getAsJsonObject().get("model").getAsString());
        JsonObject model = json(files, modernPath).getAsJsonObject("model");
        assertEquals("test:generated", itemAt(model, 5));
        assertEquals("test:local", itemAt(model, 10));

        settings.set(Settings.MERGE_DUPLICATES.getPath(), false);
        Settings.invalidateCache();
        files = contents(new PackMerger(tempDir.toFile()).mergeWithHigherPriorityFiles(List.of(
                vfile(legacyPath, generatedLegacy), vfile(legacyPath, localLegacy),
                vfile(modernPath, modernItem("test:generated")), vfile(modernPath, localModern))));
        assertEquals(1, json(files, legacyPath).getAsJsonArray("overrides").size());
        assertEquals("test:generated", itemAt(json(files, modernPath).getAsJsonObject("model"), 10));
    }

    @Test void masksBaseTextureMetadataWhenOverlayReplacesOnlyTheImage() throws Exception {
        String texture = "assets/test/textures/a.png";
        write("a", Map.of("pack.mcmeta", metadata("overlay", 46, 70),
                texture, "base", texture + ".mcmeta", "{\"animation\":{}}",
                "overlay/" + texture, "replacement"));
        PackMerger merger = new PackMerger(tempDir.toFile());
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        String directory = merger.getMergedOverlayEntries().get(0).getAsJsonObject().get("directory").getAsString();
        assertEquals("replacement", files.get(directory + "/" + texture));
        assertEquals(new JsonObject(), json(files, directory + "/" + texture + ".mcmeta"));
        assertTrue(json(files, texture + ".mcmeta").has("animation"));
    }

    @Test void languageDeclarationsFollowTranslationPriority() throws Exception {
        write("a", Map.of("pack.mcmeta", """
                {"language":{"test":{"name":"Low","region":"Low","bidirectional":false},
                             "other":{"name":"Other","region":"Other"}}}
                """, "assets/test/lang/test.json", "{\"shared\":\"low\"}"));
        write("b", Map.of("pack.mcmeta", """
                {"language":{"test":{"name":"High","region":"High","bidirectional":true}}}
                """, "assets/test/lang/test.json", "{\"shared\":\"high\"}"));
        PackMerger merger = new PackMerger(tempDir.toFile());
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        assertEquals("high", json(files, "assets/test/lang/test.json").get("shared").getAsString());
        JsonObject declaration = merger.getMergedLanguageMetadata().getAsJsonObject("test");
        assertEquals("High", declaration.get("name").getAsString());
        assertEquals("High", declaration.get("region").getAsString());
        assertTrue(declaration.get("bidirectional").getAsBoolean());
        assertTrue(merger.getMergedLanguageMetadata().has("other"));
        merger.mergeWithHigherPriorityFiles(List.of(vfile("pack.mcmeta", """
                {"language":{"test":{"name":"Local","region":"Local"}}}
                """)));
        assertEquals("Local", merger.getMergedLanguageMetadata().getAsJsonObject("test").get("name").getAsString());
    }

    private static String modernItem(String model) {
        return "{\"model\":{\"type\":\"minecraft:range_dispatch\",\"property\":\"minecraft:custom_model_data\",\"index\":0,"
                + "\"entries\":[{\"threshold\":5,\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + model + "\"}}],"
                + "\"fallback\":{\"type\":\"minecraft:model\",\"model\":\"test:fallback\"}}}";
    }
    private static String itemAt(JsonObject model, double data) {
        if (model.get("type").getAsString().equals("minecraft:model")) return model.get("model").getAsString();
        JsonObject selected = model.getAsJsonObject("fallback");
        double threshold = Double.NEGATIVE_INFINITY;
        for (JsonElement value : model.getAsJsonArray("entries")) {
            JsonObject entry = value.getAsJsonObject();
            double candidate = entry.get("threshold").getAsDouble();
            if (candidate <= data && candidate >= threshold) {
                selected = entry.getAsJsonObject("model");
                threshold = candidate;
            }
        }
        return itemAt(selected, data);
    }

    @Test void preservesMinorBoundariesWhenEncodingOverlayIntervals() throws Exception {
        write("a", Map.of("pack.mcmeta", "{\"overlays\":{\"entries\":["
                        + "{\"directory\":\"all\",\"min_format\":[97],\"max_format\":97},"
                        + "{\"directory\":\"zero\",\"min_format\":[97,0],\"max_format\":[97,0]},"
                        + "{\"directory\":\"one\",\"min_format\":[97,1],\"max_format\":[97,1]}]}}",
                "assets/test/lang/en_us.json", "{\"value\":\"base\"}",
                "all/assets/test/lang/en_us.json", "{\"value\":\"all\"}",
                "zero/assets/test/lang/en_us.json", "{\"value\":\"zero\"}",
                "one/assets/test/lang/en_us.json", "{\"value\":\"one\"}"));
        PackMerger merger = new PackMerger(tempDir.toFile());
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        JsonArray declarations = merger.getMergedOverlayEntries();
        assertEquals("zero", activeMinor(files, declarations, 97, 0).get("value").getAsString());
        assertEquals("one", activeMinor(files, declarations, 97, 1).get("value").getAsString());
        assertEquals("all", activeMinor(files, declarations, 97, 2).get("value").getAsString());
        assertEquals("all", activeMinor(files, declarations, 97, Integer.MAX_VALUE).get("value").getAsString());
        assertEquals("base", activeMinor(files, declarations, 98, 0).get("value").getAsString());
        assertEquals(JsonParser.parseString("[97,0]"), declarations.get(0).getAsJsonObject().get("max_format"));
        for (JsonElement value : declarations) {
            JsonObject declaration = value.getAsJsonObject();
            for (String key : List.of("min_format", "max_format")) {
                JsonElement bound = declaration.get(key);
                if (bound.isJsonArray()) bound.getAsJsonArray().forEach(number -> assertTrue(number.getAsLong() <= Integer.MAX_VALUE));
                else assertTrue(bound.getAsLong() <= Integer.MAX_VALUE);
            }
        }
    }

    @Test void normalizesCompleteCompiledOverlayListAcrossOldAndNewFormats() throws Exception {
        write("a", Map.of("pack.mcmeta", metadata("old", 46, 64),
                "assets/test/lang/en_us.json", "{}", "old/assets/test/lang/en_us.json", "{\"old\":\"yes\"}"));
        write("b", Map.of("pack.mcmeta", "{\"overlays\":{\"entries\":[{\"directory\":\"new\",\"min_format\":[97,1],\"max_format\":[97,2]}]}}",
                "assets/test/lang/en_us.json", "{}", "new/assets/test/lang/en_us.json", "{\"new\":\"yes\"}"));
        PackMerger merger = new PackMerger(tempDir.toFile());
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        JsonArray entries = merger.getMergedOverlayEntries();
        assertEquals(2, entries.size());
        for (JsonElement entry : entries) assertTrue(entry.getAsJsonObject().has("formats"));
        JsonObject modern = entries.get(1).getAsJsonObject();
        assertEquals(JsonParser.parseString("[97,1]"), modern.get("min_format"));
        assertEquals(JsonParser.parseString("[97,2]"), modern.get("max_format"));
        assertTrue(activeMinor(files, entries, 97, 1).has("new"));
        assertTrue(activeMinor(files, entries, 97, 2).has("new"));
        assertFalse(activeMinor(files, entries, 97, 0).has("new"));
        assertFalse(activeMinor(files, entries, 97, 3).has("new"));
    }

    private static JsonObject activeMinor(Map<String, String> files, JsonArray declarations, int major, int minor) {
        long version = major * (1L << 31) + minor;
        int matches = 0;
        String path = "assets/test/lang/en_us.json";
        for (JsonElement value : declarations) {
            JsonObject declaration = value.getAsJsonObject();
            long min = decodeBound(declaration.get("min_format"), false);
            long max = decodeBound(declaration.get("max_format"), true);
            if (min <= version && version <= max) {
                matches++;
                path = declaration.get("directory").getAsString() + "/assets/test/lang/en_us.json";
            }
        }
        assertTrue(matches <= 1);
        return json(files, path);
    }
    private static long decodeBound(JsonElement value, boolean max) {
        long major = value.isJsonArray() ? value.getAsJsonArray().get(0).getAsLong() : value.getAsLong();
        long minor = value.isJsonArray() && value.getAsJsonArray().size() == 2
                ? value.getAsJsonArray().get(1).getAsLong() : max ? Integer.MAX_VALUE : 0;
        return major * (1L << 31) + minor;
    }

    private static String modelFor(JsonObject blockstate, Map<String, String> properties) {
        List<String> matches = new ArrayList<>();
        for (JsonElement value : blockstate.getAsJsonArray("multipart")) {
            JsonObject part = value.getAsJsonObject();
            if (!part.has("when") || matches(part.getAsJsonObject("when"), properties)) matches.add(part.getAsJsonObject("apply").get("model").getAsString());
        }
        assertEquals(1, matches.size());
        return matches.getFirst();
    }
    private static boolean matches(JsonObject condition, Map<String, String> properties) {
        if (condition.has("AND")) return condition.getAsJsonArray("AND").asList().stream().allMatch(value -> matches(value.getAsJsonObject(), properties));
        if (condition.has("OR")) return condition.getAsJsonArray("OR").asList().stream().anyMatch(value -> matches(value.getAsJsonObject(), properties));
        return condition.entrySet().stream().allMatch(entry -> {
            String expected = entry.getValue().getAsString();
            boolean negated = expected.startsWith("!");
            return Arrays.asList((negated ? expected.substring(1) : expected).split("\\|")).contains(properties.get(entry.getKey())) != negated;
        });
    }
    private String metadata(String directory, int min, int max) { return "{\"overlays\":{\"entries\":[{\"directory\":\"" + directory + "\",\"formats\":[" + min + "," + max + "]}]}}"; }
    private void write(String name, Map<String, String> entries) throws IOException {
        for (var entry : entries.entrySet()) {
            Path path = tempDir.resolve("uploads").resolve(name).resolve(entry.getKey());
            Files.createDirectories(path.getParent()); Files.writeString(path, entry.getValue());
        }
    }
    private static VirtualFile vfile(String path, String text) {
        int index = path.lastIndexOf('/');
        return new VirtualFile(index < 0 ? "" : path.substring(0, index), path.substring(index + 1), new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static Map<String, String> contents(List<VirtualFile> files) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        for (VirtualFile file : files) try (InputStream input = file.getInputStream()) { result.put(file.getPath(), new String(input.readAllBytes(), StandardCharsets.UTF_8)); }
        return result;
    }
    private static JsonObject json(Map<String, String> files, String path) { return JsonParser.parseString(files.get(path)).getAsJsonObject(); }
    private static JsonObject active(Map<String, String> files, JsonArray entries, int format) {
        String path = "assets/test/lang/en_us.json";
        for (JsonElement value : entries) {
            JsonObject entry = value.getAsJsonObject();
            JsonElement minValue = entry.get("min_format"), maxValue = entry.get("max_format");
            int min = minValue.isJsonArray() ? minValue.getAsJsonArray().get(0).getAsInt() : minValue.getAsInt();
            int max = maxValue.isJsonArray() ? maxValue.getAsJsonArray().get(0).getAsInt() : maxValue.getAsInt();
            if (min <= format && format <= max) return json(files, entry.get("directory").getAsString() + "/" + path);
        }
        return json(files, path);
    }
}
