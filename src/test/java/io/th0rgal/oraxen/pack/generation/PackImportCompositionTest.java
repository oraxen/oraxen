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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
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
        when(configs.getSettings()).thenAnswer(ignored -> settings);
        plugins = mockStatic(OraxenPlugin.class);
        plugins.when(OraxenPlugin::get).thenReturn(plugin);
        logs = mockStatic(Logs.class);
        Settings.invalidateCache();
    }
    @Test void coreShadersAreOptInAndWarnOnceAcrossFilesAndSnapshots() throws Exception {
        String fragment = "assets/minecraft/shaders/core/test.fsh";
        String vertex = "assets/minecraft/shaders/core/test.vsh";
        String low = "#version 150\nvoid main() { float low = 1.0; }";
        String high = "#version 150\nvoid main() { float high = 2.0; }";
        write("low", Map.of(fragment, low));
        PackMerger disabled = new PackMerger(tempDir.toFile());
        disabled.mergeUploadedPacks();
        var unchanged = contents(disabled.mergeWithHigherPriorityFiles(List.of(vfile(fragment, high))));
        assertEquals(high, unchanged.get(fragment));
        logs.verifyNoInteractions();

        settings.set(Settings.MERGE_SHADERS.getPath(), true);
        Settings.invalidateCache();
        write("low", Map.of(fragment, low, vertex, low, "pack.mcmeta", metadata("modern", 46, 75),
                "modern/" + fragment, high));
        PackMerger enabled = new PackMerger(tempDir.toFile());
        enabled.mergeUploadedPacks();
        var merged = contents(enabled.mergeWithHigherPriorityFiles(List.of(vfile(fragment, high), vfile(vertex, high))));
        assertTrue(merged.get(fragment).contains("float low = 1.0;"));
        assertTrue(merged.get(fragment).contains("float high = 2.0;"));
        assertTrue(merged.get(vertex).contains("float low = 1.0;"));
        logs.verify(() -> Logs.logWarning("Shader merging is experimental and limited. Turn of shader merging in the settings or contact the Support team when running into shader issues."), times(1));
    }

    @Test void incompatibleCoreShadersKeepHigherPrioritySource() throws Exception {
        settings.set(Settings.MERGE_SHADERS.getPath(), true);
        Settings.invalidateCache();
        String path = "assets/minecraft/shaders/core/test.fsh";
        write("low", Map.of(path, "#version 150\nuniform float value;\nvoid main() {}"));
        PackMerger merger = new PackMerger(tempDir.toFile());
        merger.mergeUploadedPacks();
        String high = "#version 150\nuniform vec2 value;\nvoid main() {}";
        assertEquals(high, contents(merger.mergeWithHigherPriorityFiles(List.of(vfile(path, high)))).get(path));
        logs.verify(() -> Logs.logWarning("Failed to merge core shader " + path + ". Keeping the higher priority file."));
    }

    @Test void shaderFailureKeepsWholeHigherPriorityProgramIncludingLinkedStages() throws Exception {
        String root = "assets/minecraft/shaders/core/";
        String lowVertex = "#version 150\nout float value;\nvoid main() { value = 1.0; }";
        String highVertex = "#version 150\nout vec2 value;\nvoid main() { value = vec2(1.0); }";
        String lowFragment = "#version 150\nin float value;\nvoid main() { lowEffect(); }";
        String highFragment = "#version 150\nvoid main() { highEffect(); }";
        String program = "{\"vertex\":\"custom_vertex\",\"fragment\":\"custom_fragment\"}";
        write("a-low", Map.of(root + "custom_vertex.vsh", lowVertex, root + "custom_fragment.fsh", lowFragment,
                root + "program.json", program));
        write("b-high", Map.of(root + "custom_vertex.vsh", highVertex, root + "custom_fragment.fsh", highFragment,
                root + "program.json", program));
        settings.set(Settings.MERGE_SHADERS.getPath(), true);
        Settings.invalidateCache();
        var files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());
        assertEquals(highVertex, files.get(root + "custom_vertex.vsh"));
        assertEquals(highFragment, files.get(root + "custom_fragment.fsh"));
        assertEquals(program, files.get(root + "program.json"));
    }

    @Test void inheritedProgramsKeepLinkedStagesTogetherWithQualifiedAndLegacyReferences() throws Exception {
        String root = "assets/minecraft/shaders/core/";
        String lowVertex = "#version 150\nout float value;\nvoid main() { value = 1.0; }";
        String highVertex = "#version 150\nout vec2 value;\nvoid main() { value = vec2(1.0); }";
        String lowFragment = "#version 150\nin float value;\nvoid main() { if (value < 0.5) { discard; } }";
        String highFragment = "#version 150\nvoid main() { highEffect(); }";
        settings.set(Settings.MERGE_SHADERS.getPath(), true);
        Settings.invalidateCache();
        for (boolean inherited : List.of(true, false)) {
            for (String prefix : List.of("", "minecraft:core/")) {
                String program = "{\"vertex\":\"" + prefix + "custom_vertex\",\"fragment\":\""
                        + prefix + "custom_fragment\"}";
                write("a-low", Map.of(root + "custom_vertex.vsh", lowVertex,
                        root + "custom_fragment.fsh", lowFragment, root + "program.json", program));
                Map<String, String> high = new LinkedHashMap<>(Map.of(root + "custom_vertex.vsh", highVertex,
                        root + "custom_fragment.fsh", highFragment));
                if (!inherited) high.put(root + "program.json", program);
                else Files.deleteIfExists(tempDir.resolve("uploads/b-high/" + root + "program.json"));
                write("b-high", high);
                var files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());
                assertEquals(highVertex, files.get(root + "custom_vertex.vsh"));
                assertEquals(highFragment, files.get(root + "custom_fragment.fsh"));
                assertEquals(program, files.get(root + "program.json"));
            }
        }
    }

    @Test void mergesDuplicateLocalOverlayShadersBeforePreservingNativeOverlays() throws Exception {
        settings.set(Settings.MERGE_SHADERS.getPath(), true);
        Settings.invalidateCache();
        String path = "modern/assets/minecraft/shaders/core/test.fsh";
        String low = "#version 150\nvoid main() { lowEffect(); }";
        String high = "#version 150\nvoid main() { highEffect(); }";
        PackMerger merger = new PackMerger(tempDir.toFile());
        var files = contents(merger.mergeWithHigherPriorityFiles(List.of(
                vfile("pack.mcmeta", metadata("modern", 46, 75)), vfile(path, high), vfile(path, low)), true));
        assertTrue(files.get(path).contains("lowEffect();"));
        assertTrue(files.get(path).contains("highEffect();"));
        String directory = merger.getMergedOverlayEntries().get(0).getAsJsonObject().get("directory").getAsString();
        assertTrue(files.get(directory + "/assets/minecraft/shaders/core/test.fsh").contains("lowEffect();"));
    }

    @AfterEach
    void cleanup() {
        Settings.invalidateCache();
        logs.close();
        plugins.close();
    }

    @Test void skipsUndecodableZipWithoutLosingOtherPacks() throws Exception {
        Path uploads = Files.createDirectories(tempDir.resolve("uploads"));
        Path badZip = uploads.resolve("a-bad.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(badZip))) {
            zip.putNextEntry(new ZipEntry("x"));
            zip.write(1);
            zip.closeEntry();
        }
        byte[] bytes = Files.readAllBytes(badZip);
        // The local ZIP header stores the UTF-8 filename immediately after its 30 fixed bytes.
        bytes[30] = (byte) 0xff;
        Files.write(badZip, bytes);
        write("b-valid", Map.of("assets/test/lang/en_us.json", "{\"valid\":\"yes\"}"));

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());

        assertEquals("yes", json(files, "assets/test/lang/en_us.json").get("valid").getAsString());
        logs.verify(() -> Logs.logError("Failed to import resource pack a-bad.zip"));
    }

    @Test void normalizesDeeplyNestedPackRootsAndSkipsNonPackFiles() throws Exception {
        Path zip = tempDir.resolve("uploads/battlepass.zip");
        Files.createDirectories(zip.getParent());
        writeZip(zip, Map.of(
                "Plugins/BattlePass/config.yml", "some: config",
                "README.md", "# Instructions",
                "Textures (use 1)/ItemsAdders/contents/pack/resourcepack/pack.mcmeta", "{\"pack\":{\"pack_format\":34}}",
                "Textures (use 1)/ItemsAdders/contents/pack/resourcepack/assets/test/models/item/paper.json", "{\"parent\":\"item/generated\"}",
                "Textures (use 1)/vanilla_pack/pack.mcmeta", "{\"pack\":{\"pack_format\":34}}",
                "Textures (use 1)/vanilla_pack/assets/test/textures/icon.png", "PNG_DATA"));

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());

        assertEquals("{\"parent\":\"item/generated\"}", files.get("assets/test/models/item/paper.json"));
        assertEquals("PNG_DATA", files.get("assets/test/textures/icon.png"));
        assertFalse(files.keySet().stream().anyMatch(path -> path.contains("config.yml") || path.contains("README")
                || path.contains("Textures") || path.contains("ItemsAdders")));
    }

    @Test void doesNotTreatFolderEndingWithAssetsAsPackRoot() throws Exception {
        Path zip = tempDir.resolve("uploads/tricky.zip");
        Files.createDirectories(zip.getParent());
        writeZip(zip, Map.of(
                "testassets/minecraft/textures/bad.png", "BAD",
                "realpack/pack.mcmeta", "{\"pack\":{\"pack_format\":34}}",
                "realpack/assets/minecraft/textures/good.png", "GOOD"));

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());

        assertEquals("GOOD", files.get("assets/minecraft/textures/good.png"));
        assertFalse(files.keySet().stream().anyMatch(path -> path.contains("bad.png") || path.contains("testassets")));
    }

    @Test void ignoresMetadataJunkEntriesInUploadedZips() throws Exception {
        Path zip = tempDir.resolve("uploads/junk.zip");
        Files.createDirectories(zip.getParent());
        writeZip(zip, Map.of(
                "pack.mcmeta", "{\"pack\":{\"pack_format\":34}}",
                "assets/test/good.txt", "GOOD",
                "assets/.DS_Store", "junk",
                "__MACOSX/pack.mcmeta", "junk",
                "__MACOSX/assets/test/junk.txt", "junk"));

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());

        assertEquals("GOOD", files.get("assets/test/good.txt"));
        assertFalse(files.keySet().stream().anyMatch(path -> path.contains("DS_Store") || path.contains("MACOSX") || path.contains("junk")));
    }

    @Test void alphabeticallyLaterUploadWinsWhenPacksProvideTheSamePath() throws Exception {
        write("a-low", Map.of("assets/test/notes.txt", "low"));
        write("b-high", Map.of("assets/test/notes.txt", "high"));

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());

        assertEquals("high", files.get("assets/test/notes.txt"));
    }

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

    @ParameterizedTest
    @CsvSource({"equipment, 46, 75", "models/equipment, 42, 42"})
    void mergesEquipmentAcrossUploadsAndKeepsOverlayReplacementWithinAnUpload(String folder, int min, int max) throws Exception {
        String path = "assets/test/" + folder + "/armor.json";
        write("a", Map.of(path, """
                {"layers":{"humanoid":[{"texture":"test:base"}],"wings":[{"texture":"test:wings"}]}}
                """));
        write("b", Map.of(path, "{\"layers\":{\"humanoid\":[{\"texture\":\"test:detail\"}]}}",
                "pack.mcmeta", metadata("variant", min, max),
                "variant/" + path, "{\"layers\":{\"humanoid\":[{\"texture\":\"test:modern\"}]}}"));
        write("c", Map.of(path, "{\"layers\":{\"humanoid\":false}}"));

        PackMerger merger = new PackMerger(tempDir.toFile());
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        String directory = merger.getMergedOverlayEntries().get(0).getAsJsonObject().get("directory").getAsString();
        assertTrue(directory.startsWith("oraxen_import_"));
        assertEquals(JsonParser.parseString("""
                {"humanoid":[{"texture":"test:base"},{"texture":"test:detail"}],"wings":[{"texture":"test:wings"}]}
                """), json(files, path).get("layers"));
        assertEquals(JsonParser.parseString("""
                {"humanoid":[{"texture":"test:base"},{"texture":"test:modern"}],"wings":[{"texture":"test:wings"}]}
                """), json(files, directory + "/" + path).get("layers"));
    }

    @Test void preservesRepeatedEquipmentLayersAndRejectsEmptyDefinitions() throws Exception {
        String path = "assets/test/equipment/armor.json";
        write("a", Map.of(path, "{\"layers\":{\"humanoid\":[{\"texture\":\"test:same\"}]},\"custom\":\"low\"}"));
        write("b", Map.of(path, "{\"layers\":{\"humanoid\":[{\"texture\":\"test:same\"}]},\"custom\":\"high\"}"));
        write("c", Map.of(path, "{\"layers\":{}}"));
        write("d", Map.of(path, "{\"layers\":{\"humanoid\":[]}}"));
        write("e", Map.of(path, "{\"layers\":{\"humanoid\":[false]}}"));
        write("f", Map.of(path, "{\"layers\":{\"humanoid\":[{}]}}"));
        write("g", Map.of(path, "{\"layers\":{\"humanoid\":[{\"texture\":42}]}}"));

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());
        assertEquals(JsonParser.parseString("""
                {"layers":{"humanoid":[{"texture":"test:same"},{"texture":"test:same"}]},"custom":"high"}
                """), json(files, path));
    }

    @Test void rejectsEquipmentWithInvalidTextureIdsWithoutLosingValidShorthandIds() throws Exception {
        String path = "assets/test/equipment/armor.json";
        write("a", Map.of(path, """
                {"layers":{"humanoid":[{"texture":"test:Armor"}]}}
                """));
        write("b", Map.of(path, """
                {"layers":{"humanoid":[{"texture":"armor/detail-1.2"},{"texture":":trim"}]}}
                """));
        write("c", Map.of(path, """
                {"layers":{"humanoid":[{"texture":"Test:trim"}]}}
                """));

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());
        assertEquals(JsonParser.parseString("""
                {"humanoid":[{"texture":"armor/detail-1.2"},{"texture":":trim"}]}
                """), json(files, path).get("layers"));
    }

    @Test void composesLocalLegacyEquipmentWithoutDroppingOrdinaryModelsInItsFolder() throws Exception {
        String root = "assets/test/models/equipment/";
        String equipment = root + "armor.json";
        String model = root + "shield.json";
        String higherModel = "{\"parent\":\"minecraft:item/handheld\"}";
        write("uploaded", Map.of(model, "{\"parent\":\"minecraft:item/generated\"}"));
        PackMerger merger = new PackMerger(tempDir.toFile());
        merger.mergeUploadedPacks();

        Map<String, String> files = contents(merger.mergeWithHigherPriorityFiles(List.of(
                vfile(model, higherModel),
                vfile(equipment, "{\"layers\":{\"humanoid\":[{\"texture\":\"test:detail\"}]}}"),
                vfile(equipment, "{\"layers\":{\"humanoid\":[{\"texture\":\"test:base\"}]}}"))));
        assertEquals(higherModel, files.get(model));
        assertEquals(JsonParser.parseString("""
                {"humanoid":[{"texture":"test:base"},{"texture":"test:detail"}]}
                """), json(files, equipment).get("layers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    {"parent":"minecraft:item/handheld","layers":["preview"]}
                    """,
            """
                    {"parent":"minecraft:item/generated","textures":{"layer0":"test:item/preview"},
                     "layers":{"humanoid":[{"texture":"#layer0"}]}}
                    """,
            """
                    {"parent":"minecraft:item/generated","textures":{"layer0":"test:item/preview"},
                     "layers":{"humanoid":[{"texture":null}]}}
                    """,
            """
                    {"parent":"minecraft:item/generated","textures":{"layer0":"test:item/preview"},
                     "layers":{"humanoid":[{"texture":42}]}}
                    """
    })
    void keepsOrdinaryLegacyEquipmentModelsWithCustomLayersMetadata(String model) throws Exception {
        String root = "assets/test/models/equipment/";
        String replaced = root + "shield.json";
        String standalone = root + "preview.json";
        String local = root + "local.json";
        String replacesEquipment = root + "armor.json";
        String localReplacesEquipment = root + "local_armor.json";
        String lowerModel = "{\"parent\":\"minecraft:item/generated\"}";
        String lowerEquipment = "{\"layers\":{\"humanoid\":[{\"texture\":\"test:base\"}]}}";
        write("a", Map.of(replaced, lowerModel, replacesEquipment, lowerEquipment));
        write("b", Map.of(replaced, model, standalone, model, replacesEquipment, model));
        PackMerger merger = new PackMerger(tempDir.toFile());

        Map<String, String> uploaded = contents(merger.mergeUploadedPacks());
        assertEquals(model, uploaded.get(replaced));
        assertEquals(model, uploaded.get(standalone));
        assertEquals(model, uploaded.get(replacesEquipment));
        Map<String, String> files = contents(merger.mergeWithHigherPriorityFiles(List.of(
                vfile(local, model), vfile(local, lowerModel),
                vfile(localReplacesEquipment, model), vfile(localReplacesEquipment, lowerEquipment))));
        assertEquals(model, files.get(local));
        assertEquals(model, files.get(localReplacesEquipment));
    }

    static Stream<Arguments> malformedLegacyEquipmentOverrides() {
        String equipment = "{\"layers\":{\"humanoid\":[{\"texture\":\"test:base\"}]}}";
        String model = "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"test:item/shield\"}}";
        return Stream.of(
                Arguments.of(equipment, "\"test:Armor\""),
                Arguments.of(model, "\"test:Armor\""),
                Arguments.of(equipment, "\"#trim\""),
                Arguments.of(model, "\"#trim\""),
                Arguments.of(equipment, "null"),
                Arguments.of(model, "null"),
                Arguments.of(equipment, "42"),
                Arguments.of(model, "42"));
    }

    @ParameterizedTest
    @MethodSource("malformedLegacyEquipmentOverrides")
    void rejectsLegacyEquipmentWithInvalidTexturesEvenWhenItHasModelFields(String valid, String texture) throws Exception {
        String root = "assets/test/models/equipment/";
        String uploaded = root + "uploaded.json";
        String local = root + "local.json";
        String generated = root + "generated.json";
        String invalid = """
                {"parent":"minecraft:item/handheld","layers":{"humanoid":[{"texture":%s}]}}
                """.formatted(texture);
        write("a", Map.of(uploaded, valid, generated, valid));
        write("b", Map.of(uploaded, invalid));
        PackMerger merger = new PackMerger(tempDir.toFile());

        Map<String, String> uploadedFiles = contents(merger.mergeUploadedPacks());
        Map<String, String> localFiles = contents(merger.mergeWithHigherPriorityFiles(List.of(
                vfile(local, invalid), vfile(local, valid), vfile(generated, invalid))));
        assertAll(
                () -> assertEquals(valid, uploadedFiles.get(uploaded), "uploaded packs"),
                () -> assertEquals(valid, localFiles.get(local), "local duplicates"),
                () -> assertEquals(valid, localFiles.get(generated), "generated files over uploaded packs"));
    }

    @Test void reportsLegacyEquipmentReplacingAnOrdinaryModelAsAnOverride() throws Exception {
        String path = "assets/test/models/equipment/armor.json";
        String equipment = "{\"layers\":{\"humanoid\":[{\"texture\":\"test:armor\"}]}}";
        write("a", Map.of(path, "{\"parent\":\"minecraft:item/generated\"}"));
        write("b", Map.of(path, equipment));
        settings.set(Settings.DEBUG.getPath(), true);
        Settings.invalidateCache();

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());
        assertEquals(equipment, files.get(path));
        logs.verify(() -> Logs.logInfo("[Debug] Resource pack override '" + path + "' from 'a' replaced by 'b'."));
    }

    @Test void deduplicatesAtlasSourcesAndFontProvidersWithoutChangingPriority() throws Exception {
        String atlas = "assets/test/atlases/blocks.json";
        String font = "assets/test/font/default.json";
        write("a", Map.of(atlas, """
                {"sources":[{"type":"single","resource":"test:restored"},{"type":"filter","pattern":{"path":"restored"}}]}
                """, font, """
                {"providers":[{"type":"reference","id":"test:shared","filter":{"uniform":true}},
                {"type":"reference","id":"test:shared","filter":{"uniform":false}},
                {"type":"reference","id":"test:low"}]}
                """));
        write("b", Map.of(atlas, """
                {"sources":[{"resource":"test:restored","type":"single"}]}
                """, font, """
                {"providers":[{"filter":{"uniform":true},"id":"test:shared","type":"reference"},
                {"type":"reference","id":"test:high"}]}
                """));

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());
        assertEquals(JsonParser.parseString("""
                [{"type":"filter","pattern":{"path":"restored"}},{"type":"single","resource":"test:restored"}]
                """), json(files, atlas).get("sources"));
        assertEquals(JsonParser.parseString("""
                [{"type":"reference","id":"test:shared","filter":{"uniform":true}},
                {"type":"reference","id":"test:high"},
                {"type":"reference","id":"test:shared","filter":{"uniform":false}},
                {"type":"reference","id":"test:low"}]
                """), json(files, font).get("providers"));
    }

    @Test void legacySplitAtlasRetainsTheLaterDuplicateAfterAFilter() {
        byte[] blocks = """
                {"custom":"retained","sources":[{"type":"single","resource":"test:restored"},
                {"type":"filter","pattern":{"path":"restored"}}]}
                """.getBytes(StandardCharsets.UTF_8);
        byte[] items = """
                {"sources":[{"resource":"test:restored","type":"single"}]}
                """.getBytes(StandardCharsets.UTF_8);

        JsonObject result = JsonParser.parseString(new String(
                AtlasGenerator.mergeSplitAtlasesForLegacyTarget(blocks, items), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(JsonParser.parseString("""
                {"custom":"retained","sources":[{"type":"filter","pattern":{"path":"restored"}},
                {"type":"single","resource":"test:restored"}]}
                """), result);
    }

    @Test void reportsChangedUploadOverridesOnceAndOnlyInDebugMode() throws Exception {
        String texture = "assets/test/textures/changed.png";
        String identical = "assets/test/textures/same.png";
        String model = "assets/test/models/block/example.json";
        String equipment = "assets/test/equipment/armor.json";
        write("a-low", Map.of(texture, "low", identical, "same", model, "{\"parent\":\"test:low\"}",
                equipment, "{\"layers\":{\"humanoid\":[{\"texture\":\"test:low\"}]}}"));
        writeZip(tempDir.resolve("uploads/b-high.zip"), Map.of(texture, "high", identical, "same",
                equipment, "{\"layers\":{\"humanoid\":[{\"texture\":\"test:high\"}]}}",
                "pack.mcmeta", metadata("modern", 46, 75), "modern/" + model, "{\"parent\":\"test:high\"}"));

        settings.set(Settings.DEBUG.getPath(), false);
        Settings.invalidateCache();
        assertEquals("high", contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks()).get(texture));
        logs.verifyNoInteractions();

        settings.set(Settings.DEBUG.getPath(), true);
        Settings.invalidateCache();
        assertEquals("high", contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks()).get(texture));
        logs.verify(() -> Logs.logInfo("[Debug] Resource pack override '" + texture + "' from 'a-low' replaced by 'b-high.zip'."));
        logs.verify(() -> Logs.logInfo("[Debug] Resource pack override '" + model + "' from 'a-low' replaced by 'b-high.zip'."));
        logs.verifyNoMoreInteractions();
    }

    @Test void reportsActualNestedSourceAfterIdenticalReplacementAndIgnoresFilteredResources() throws Exception {
        String changed = "assets/test/textures/changed.png";
        String filtered = "assets/test/textures/filtered.png";
        write("a", Map.of(changed, "same", filtered, "low"));
        write("b", Map.of("wrapper/pack.mcmeta", "{}", "wrapper/" + changed, "same"));
        write("c", Map.of(changed, "new", filtered, "high", "pack.mcmeta", """
                {"filter":{"block":[{"namespace":"test","path":"textures/filtered.png"}]}}
                """));
        settings.set(Settings.DEBUG.getPath(), true);
        Settings.invalidateCache();

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());
        assertEquals("new", files.get(changed));
        assertEquals("high", files.get(filtered));
        logs.verify(() -> Logs.logInfo("[Debug] Resource pack override '" + changed + "' from 'b/wrapper' replaced by 'c'."));
        logs.verifyNoMoreInteractions();
    }

    @Test void filtersImportedBaseAndOverlayPathsAfterStrippingWrapperFolders() throws Exception {
        settings.set("pack.import.exclude", List.of(
                "assets/test/private.txt", "assets/minecraft/shaders/core/**"));
        write("wrapped", Map.of(
                "wrapper/pack.mcmeta", metadata("imported_overlay", 46, 80),
                "wrapper/assets/test/private.txt", "excluded base",
                "wrapper/assets/test/public.txt", "kept base",
                "wrapper/assets/minecraft/shaders/core/base.vsh", "excluded base shader",
                "wrapper/assets/minecraft/shaders/include/base.glsl", "kept base include",
                "wrapper/imported_overlay/assets/test/private.txt", "excluded overlay",
                "wrapper/imported_overlay/assets/test/public.txt", "kept overlay",
                "wrapper/imported_overlay/assets/minecraft/shaders/core/overlay.vsh", "excluded overlay shader"));

        PackMerger merger = new PackMerger(tempDir.toFile());
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        String overlayDirectory = merger.getMergedOverlayEntries().get(0).getAsJsonObject()
                .get("directory").getAsString();

        assertFalse(files.containsKey("assets/test/private.txt"));
        assertFalse(files.containsKey(overlayDirectory + "/assets/test/private.txt"));
        assertFalse(files.containsKey("assets/minecraft/shaders/core/base.vsh"));
        assertFalse(files.containsKey(overlayDirectory + "/assets/minecraft/shaders/core/overlay.vsh"));
        assertEquals("kept base", files.get("assets/test/public.txt"));
        assertEquals("kept overlay", files.get(overlayDirectory + "/assets/test/public.txt"));
        assertEquals("kept base include", files.get("assets/minecraft/shaders/include/base.glsl"));
        assertFalse(files.keySet().stream().anyMatch(path -> path.contains("wrapper/")));
    }

    @Test void uploadedZipContainersRemainImportSourcesAndNewBuildsUseRefreshedRules() throws Exception {
        Path uploadedZip = tempDir.resolve("uploads/source.zip");
        Files.createDirectories(uploadedZip.getParent());
        writeZip(uploadedZip, Map.of(
                "pack.mcmeta", "{\"pack\":{\"pack_format\":46}}",
                "assets/test/notes.md", "documentation",
                "assets/test/nested.zip", "nested archive bytes",
                "assets/test/keep.txt", "kept content"));
        settings.set("pack.import.exclude", List.of("**/*.zip"));

        Map<String, String> firstBuild = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());

        assertEquals("documentation", firstBuild.get("assets/test/notes.md"));
        assertFalse(firstBuild.containsKey("assets/test/nested.zip"));
        assertEquals("kept content", firstBuild.get("assets/test/keep.txt"));

        settings = new YamlConfiguration();
        settings.set(Settings.MERGE_DUPLICATE_FONTS.getPath(), true);
        settings.set("pack.import.exclude", List.of("**/*.md"));
        Settings.invalidateCache();
        Map<String, String> refreshedBuild = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());

        assertFalse(refreshedBuild.containsKey("assets/test/notes.md"));
        assertEquals("nested archive bytes", refreshedBuild.get("assets/test/nested.zip"));
        assertEquals("kept content", refreshedBuild.get("assets/test/keep.txt"));
    }

    @Test void exclusionsDoNotRemoveGeneratedHigherPriorityFiles() throws Exception {
        settings.set("pack.import.exclude", List.of("**/*.md"));

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeWithHigherPriorityFiles(
                List.of(vfile("assets/oraxen/generated/README.md", "generated Oraxen resource"))));

        assertEquals("generated Oraxen resource", files.get("assets/oraxen/generated/README.md"));
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

    @Test void preservesNativeOverlayMetadataAndAssetsWhenMcmetaGenerationIsDisabled() throws Exception {
        settings.set(Settings.GENERATE_MCMETA.getPath(), false);
        Settings.invalidateCache();
        String resource = "assets/test/lang/en_us.json";
        write("imported", Map.of(
                "pack.mcmeta", metadata("imported_overlay", 46, 80),
                resource, "{\"importBase\":\"yes\"}",
                "imported_overlay/" + resource, "{\"importOverlay\":\"yes\"}"));

        String localMetadata = """
                {
                  "pack": {"pack_format": 75, "description": "Custom pack"},
                  "custom_metadata": {"keep": true},
                  "overlays": {"custom_property": "keep", "entries": [
                    {"directory": "user_overlay", "formats": [60, 70], "custom_entry": "keep"}
                  ]}
                }
                """;
        List<VirtualFile> localFiles = List.of(
                vfile("pack.mcmeta", localMetadata),
                vfile(resource, "{\"localBase\":\"yes\"}"),
                vfile("user_overlay/" + resource, "{\"userOverlay\":\"yes\"}"));

        PackMerger merger = new PackMerger(tempDir.toFile());
        merger.mergeUploadedPacks();
        Map<String, String> files = contents(merger.mergeWithHigherPriorityFiles(
                localFiles, !Settings.GENERATE_MCMETA.toBool()));

        assertEquals("{\"userOverlay\":\"yes\"}", files.get("user_overlay/" + resource));
        JsonArray compiledEntries = merger.getMergedOverlayEntries();
        JsonObject importedVersion = active(files, compiledEntries, 50);
        assertTrue(importedVersion.has("importOverlay"));
        assertFalse(importedVersion.has("userOverlay"));
        JsonObject localVersion = active(files, compiledEntries, 65);
        assertTrue(localVersion.has("importOverlay"));
        assertTrue(localVersion.has("userOverlay"));

        JsonObject finalMetadata = JsonParser.parseString(localMetadata).getAsJsonObject();
        PackMcmetaUtils.appendOverlayEntries(finalMetadata, compiledEntries);
        assertEquals(JsonParser.parseString("{\"keep\":true}"), finalMetadata.getAsJsonObject("custom_metadata"));
        JsonObject overlays = finalMetadata.getAsJsonObject("overlays");
        assertEquals("keep", overlays.get("custom_property").getAsString());
        JsonArray finalEntries = overlays.getAsJsonArray("entries");
        assertEquals("user_overlay", finalEntries.get(0).getAsJsonObject().get("directory").getAsString());
        assertEquals("keep", finalEntries.get(0).getAsJsonObject().get("custom_entry").getAsString());
        assertTrue(finalEntries.size() > 1);
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

    @Test void prefersHigherSoundMetadataAndPreservesResetWhenAppending() {
        JsonObject low = JsonParser.parseString("{\"event\":{\"replace\":true,\"subtitle\":\"low\",\"sounds\":[\"a\"]}}").getAsJsonObject();
        JsonObject high = JsonParser.parseString("{\"event\":{\"replace\":false,\"subtitle\":\"high\",\"sounds\":[\"b\"]}}").getAsJsonObject();
        JsonObject event = PackAssetMerger.mergeJson("assets/test/sounds.json", low, high).getAsJsonObject("event");
        assertEquals("high", event.get("subtitle").getAsString());
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

    @Test void convertsGenericUploadedLegacyOverridesWithVanillaFallback() throws Exception {
        settings.set(Settings.MERGE_DUPLICATES.getPath(), true);
        Settings.invalidateCache();
        write("generic_pack", Map.of(
                "assets/minecraft/models/item/leather_horse_armor.json", """
                        {"parent":"item/generated","textures":{"layer0":"item/leather_horse_armor"},
                         "overrides":[{"predicate":{"custom_model_data":17},"model":"test:item/custom_horse_armor"}]}
                        """,
                "assets/minecraft/models/item/diamond_horse_armor.json", """
                        {"parent":"item/generated","textures":{"layer0":"item/diamond_horse_armor"},
                         "overrides":[{"predicate":{"custom_model_data":18},"model":"test:item/custom_diamond_horse_armor"}]}
                        """,
                "assets/minecraft/models/item/totem_of_undying.json", """
                        {"parent":"item/generated","textures":{"layer0":"item/totem_of_undying"},
                         "overrides":[{"predicate":{"custom_model_data":19},"model":"test:item/custom_totem"}]}
                        """));

        PackMerger merger = new PackMerger(tempDir.toFile());
        merger.setConvertLegacyItemModels(true);
        Map<String, String> files = contents(merger.mergeUploadedPacks());
        for (String[] item : List.of(new String[]{"leather_horse_armor", "17", "test:item/custom_horse_armor"},
                new String[]{"diamond_horse_armor", "18", "test:item/custom_diamond_horse_armor"},
                new String[]{"totem_of_undying", "19", "test:item/custom_totem"})) {
            JsonObject model = json(files, "assets/minecraft/items/" + item[0] + ".json").getAsJsonObject("model");
            assertEquals(item[2], itemAt(model, Double.parseDouble(item[1])));
            assertEquals("minecraft:item/" + item[0], itemAt(model, 0));
        }
        JsonObject leatherFallback = json(files, "assets/minecraft/items/leather_horse_armor.json")
                .getAsJsonObject("model").getAsJsonObject("fallback");
        assertEquals(0xFFA06540, leatherFallback.getAsJsonArray("tints").get(0).getAsJsonObject()
                .get("default").getAsInt());
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
        logs.verify(() -> Logs.logWarning(org.mockito.ArgumentMatchers.contains(modernPath)), atLeast(2));
        logs.verify(() -> Logs.logWarning(org.mockito.ArgumentMatchers.contains(legacyPath)), atLeast(2));
    }

    @Test void importedMalformedItemDuplicatesLogPathsAndKeepHigherPriorityFiles() throws Exception {
        settings.set(Settings.MERGE_DUPLICATES.getPath(), true);
        Settings.invalidateCache();
        String modernPath = "assets/minecraft/items/paper.json";
        String legacyPath = "assets/minecraft/models/item/paper.json";
        String higherModern = modernItem("test:higher_modern");
        String higherLegacy = """
                {"parent":"minecraft:item/generated","overrides":[
                  {"predicate":{"custom_model_data":5},"model":"test:higher_legacy"}]}
                """;
        write("a", Map.of(modernPath, "{\"model\":\"broken\"}", legacyPath, "{\"overrides\":false}"));
        write("b", Map.of(modernPath, higherModern, legacyPath, higherLegacy));

        Map<String, String> files = contents(new PackMerger(tempDir.toFile()).mergeUploadedPacks());

        assertEquals(JsonParser.parseString(higherModern), json(files, modernPath));
        assertEquals(JsonParser.parseString(higherLegacy), json(files, legacyPath));
        logs.verify(() -> Logs.logWarning(org.mockito.ArgumentMatchers.contains(modernPath)));
        logs.verify(() -> Logs.logWarning(org.mockito.ArgumentMatchers.contains(legacyPath)));
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

    @Test void ignoresMalformedSodiumShaderListsAndResetsMergedMetadataOnReload() throws Exception {
        write("a", Map.of("pack.mcmeta", """
                {"sodium":{"ignored_shaders":["fog.glsl",false,"light.glsl",null,{}]}}
                """));
        write("b", Map.of("pack.mcmeta", """
                {"sodium":{"ignored_shaders":"not a list"}}
                """));
        PackMerger merger = new PackMerger(tempDir.toFile());
        merger.mergeUploadedPacks();
        merger.mergeWithHigherPriorityFiles(List.of(vfile("pack.mcmeta", "{\"sodium\":false}")));

        assertEquals(JsonParser.parseString("""
                {"ignored_shaders":["fog.glsl","light.glsl"]}
                """), merger.getMergedSodiumMetadata());

        write("a", Map.of("pack.mcmeta", "{}"));
        merger.mergeUploadedPacks();
        assertEquals(new JsonObject(), merger.getMergedSodiumMetadata());
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
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue());
        }
    }
    private static void writeZip(Path target, Map<String, String> entries) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(target))) {
            for (var entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
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
