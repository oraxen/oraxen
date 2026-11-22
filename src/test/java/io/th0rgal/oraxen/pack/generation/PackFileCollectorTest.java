package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.contains;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class PackFileCollectorTest {
    @org.junit.jupiter.api.io.TempDir
    Path tempDir;

    private MockedStatic<OraxenPlugin> pluginAccess;
    private MockedStatic<Logs> logs;

    @BeforeEach
    void setUp() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        YamlConfiguration settings = new YamlConfiguration();
        settings.set(Settings.DEBUG.getPath(), false);
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenReturn(settings);
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        logs = mockStatic(Logs.class);
        Settings.invalidateCache();
    }

    @AfterEach
    void tearDown() {
        Settings.invalidateCache();
        logs.close();
        pluginAccess.close();
    }

    @Test
    void validatesBaseAndImportedAssetsTogetherAndLogsOneSuccessSummary() throws IOException {
        List<VirtualFile> files = new ArrayList<>(List.of(
                file("assets/example/models/item/base.json", "{\"textures\":{\"layer0\":\"example:item/shared\"}}"),
                file("assets/example/textures/item/shared.png", png()),
                file("oraxen_import_0/assets/example/models/item/base.json", "{\"textures\":{\"layer0\":\"example:item/shared\"}}"),
                file("oraxen_import_0/assets/example/textures/item/shared.png", png())));

        Map<String, Set<String>> malformedFiles = PackFileCollector.verifyPackFormatting(files);

        assertTrue(malformedFiles.isEmpty());
        logs.verify(() -> Logs.logSuccess("No broken models or textures were found in the resourcepack"), times(1));
    }

    @Test
    void textureInAnotherVersionLayerDoesNotHideMissingTexture() throws IOException {
        String model = "assets/example/models/item/base.json";
        List<VirtualFile> files = new ArrayList<>(List.of(
                file(model, "{\"textures\":{\"layer0\":\"example:item/overlay_only\"}}"),
                file("oraxen_import_0/" + model, "{\"textures\":{\"layer0\":\"example:item/overlay_only\"}}"),
                file("oraxen_import_0/assets/example/textures/item/overlay_only.png", png()),
                file("oraxen_import_1/" + model, "{\"textures\":{\"layer0\":\"example:item/overlay_only\"}}")));

        Map<String, Set<String>> malformedFiles = PackFileCollector.verifyPackFormatting(files);

        assertEquals(Map.of("", Set.of(model), "oraxen_import_1/", Set.of(model)), malformedFiles);
        logs.verify(() -> Logs.logError("Pack contains malformed texture(s) and/or model(s)"), times(1));
        logs.verify(() -> Logs.logSuccess("No broken models or textures were found in the resourcepack"), times(0));
    }

    @Test
    void keepsMalformedTexturePathsScopedToTheirAssetLayer() throws IOException {
        String malformedTexture = "assets/example/textures/item/broken.png";
        List<VirtualFile> files = new ArrayList<>(List.of(
                file("assets/example/models/item/base.json", "{\"textures\":{\"layer0\":\"block/stone\"}}"),
                file(malformedTexture, "not a png"),
                file("oraxen_import_0/" + malformedTexture, "also not a png")));

        Map<String, Set<String>> malformedFiles = PackFileCollector.verifyPackFormatting(files);

        assertEquals(Set.of(malformedTexture), malformedFiles.get(""));
        assertEquals(Set.of(malformedTexture), malformedFiles.get("oraxen_import_0/"));
        logs.verify(() -> Logs.logError("Pack contains malformed texture(s) and/or model(s)"), times(1));
        logs.verify(() -> Logs.logSuccess("No broken models or textures were found in the resourcepack"), times(0));
    }

    private static VirtualFile file(String path, String content) {
        return file(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static VirtualFile file(String path, byte[] content) {
        int slash = path.lastIndexOf('/');
        return new VirtualFile(path.substring(0, slash), path.substring(slash + 1),
                new ByteArrayInputStream(content));
    }

    private static byte[] png() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", output);
        return output.toByteArray();
    }
    @ParameterizedTest
    @ValueSource(strings = {"modelengine:oog_pet", "modelengine:entity/oog_pet"})
    void missingNamespacedTextureIsReportedWithoutThrowing(String reference) {
        VirtualFile model = model("{\"textures\":{\"pet\":\"" + reference + "\"}}");

        Map<String, Set<String>> malformed = PackFileCollector.verifyPackFormatting(List.of(model));

        assertEquals(Map.of("", Set.of(model.getPath())), malformed);
        logs.verify(() -> Logs.logWarning(contains(reference)));
    }

    @Test
    void existingNamespacedPngIsAcceptedAndBothStreamsRemainReadable() throws IOException {
        byte[] png = onePixelPng();
        String modelJson = "{\"textures\":{\"pet\":\"modelengine:entity/oog_pet\"}}";
        VirtualFile model = model(modelJson);
        VirtualFile texture = new VirtualFile("assets/modelengine/textures/entity", "oog_pet.png",
                new ByteArrayInputStream(png));

        Map<String, Set<String>> malformed = PackFileCollector.verifyPackFormatting(List.of(model, texture));

        assertTrue(malformed.isEmpty());
        assertArrayEquals(modelJson.getBytes(StandardCharsets.UTF_8), read(model));
        assertArrayEquals(png, read(texture));
    }

    @ParameterizedTest
    @ValueSource(strings = {"oraxen:item/stone", "oraxen:stone"})
    void customNamespaceDoesNotInheritVanillaMaterialExemptions(String reference) {
        VirtualFile model = model("{\"textures\":{\"item\":\"" + reference + "\"}}");

        Map<String, Set<String>> malformed = PackFileCollector.verifyPackFormatting(List.of(model));

        assertEquals(Map.of("", Set.of(model.getPath())), malformed);
    }

    @ParameterizedTest
    @ValueSource(strings = {"modelengine:entity:oog_pet", "modelengine:", ":entity/oog_pet", "modelengine:entity/oog_pet:"})
    void malformedNamespacedReferencesAreReportedWithoutAborting(String reference) {
        VirtualFile model = model("{\"textures\":{\"pet\":\"" + reference + "\"}}");

        Map<String, Set<String>> malformed = PackFileCollector.verifyPackFormatting(List.of(model));

        assertEquals(Map.of("", Set.of(model.getPath())), malformed);
    }

    @Test
    void textureVariablesAndVanillaTextureExemptionsRemainAccepted() {
        VirtualFile model = model("{\"textures\":{"
                + "\"variable\":\"#layer0\","
                + "\"item\":\"item/stone\","
                + "\"block\":\"minecraft:block/stone\","
                + "\"entity\":\"entity/iron_golem\","
                + "\"material\":\"minecraft:stone\""
                + "}}");

        Map<String, Set<String>> malformed = PackFileCollector.verifyPackFormatting(List.of(model));

        assertTrue(malformed.isEmpty());
    }

    @Test
    void filtersLocalFilesByPackRelativePathBeforeAddingThem() throws IOException {
        Path pack = tempDir.resolve("pack");
        write(pack, "assets/minecraft/models/item/README.md", "ignored documentation");
        write(pack, "assets/minecraft/shaders/core/custom.vsh", "ignored core shader");
        write(pack, "assets/minecraft/shaders/include/custom.glsl", "included shader");
        write(pack, "assets/example/models/item/base.json", "{} ");
        PackExclusions exclusions = PackExclusions.compile(List.of(
                "**/*.md", "assets/minecraft/shaders/core/**"), ignored -> fail("Valid rules should not warn"));
        PackFileCollector collector = new PackFileCollector(pack.toFile(), exclusions);
        List<VirtualFile> files = new ArrayList<>();

        try {
            collector.getAllFiles(pack.resolve("assets").toFile(), files, "");

            List<String> paths = files.stream().map(VirtualFile::getPath).toList();
            assertFalse(paths.contains("assets/minecraft/models/item/README.md"));
            assertFalse(paths.contains("assets/minecraft/shaders/core/custom.vsh"));
            assertTrue(paths.contains("assets/minecraft/shaders/include/custom.glsl"));
            assertTrue(paths.contains("assets/example/models/item/base.json"));
        } finally {
            for (VirtualFile file : files) {
                try (var input = file.getInputStream()) {
                    // Close filesystem streams before the temporary directory is removed on Windows.
                }
            }
        }
    }

    private static VirtualFile model(String content) {
        return new VirtualFile("assets/modelengine/models/entity", "oog_pet.json",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static void write(Path root, String relativePath, String content) throws IOException {
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static byte[] onePixelPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static byte[] read(VirtualFile file) throws IOException {
        try (var input = file.getInputStream()) {
            return input.readAllBytes();
        }
    }
}
