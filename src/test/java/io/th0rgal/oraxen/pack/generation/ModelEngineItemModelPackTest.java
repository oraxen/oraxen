package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ModelEngineItemModelPackTest {

    @TempDir
    Path tempDir;

    private final YamlConfiguration settings = new YamlConfiguration();
    private MockedStatic<OraxenPlugin> pluginAccess;
    private MockedStatic<Logs> logs;

    @BeforeEach
    void setUp() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.namespace()).thenReturn("oraxen");
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenReturn(settings);
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        logs = mockStatic(Logs.class);
        settings.set(Settings.MERGE_DUPLICATES.getPath(), true);
        Settings.invalidateCache();
    }

    @AfterEach
    void tearDown() {
        Settings.invalidateCache();
        logs.close();
        pluginAccess.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void modelEngineLegacyOverridesResolveInLegacyAndModernVersionPacks(boolean obfuscate) throws Exception {
        Path modelEnginePack = tempDir.resolve("modelengine/resource pack.zip");
        writePack(modelEnginePack, modelEngineFiles());

        PackMerger merger = new PackMerger(tempDir.resolve("oraxen-pack").toFile());
        merger.setConvertLegacyItemModels(true);
        merger.mergeUploadedPacks();
        merger.mergeModelEnginePack(modelEnginePack.toFile());
        List<VirtualFile> generated = generatedOraxenFiles();
        List<VirtualFile> merged = merger.mergeWithHigherPriorityFiles(generated);

        PackObfuscator.obfuscate(merged, obfuscate ? "FULL" : "NONE", false);
        Map<String, byte[]> legacyPack = writeVersionPack(merged, merger.getMergedOverlayEntries(), 15, 15, 45);
        Map<String, byte[]> modernPack = writeVersionPack(merged, merger.getMergedOverlayEntries(), 46, 46, 999);

        assertLegacyOverrides(legacyPack, "leather_horse_armor", 17, 23, "horse_armor", obfuscate);
        assertLegacyOverrides(legacyPack, "player_head", 31, 41, "player_head", obfuscate);
        assertModernOverrides(modernPack, "leather_horse_armor", 17, 23, "horse_armor", obfuscate);
        assertModernOverrides(modernPack, "player_head", 31, 41, "player_head", obfuscate);
        assertPackReferencesResolve(legacyPack);
        assertPackReferencesResolve(modernPack);
        assertTextureFilesArePng(legacyPack);
        assertTextureFilesArePng(modernPack);
    }

    private Map<String, byte[]> modelEngineFiles() throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("pack.mcmeta", bytes("""
                {"pack":{"pack_format":46,"description":"ModelEngine regression fixture"},
                 "overlays":{"entries":[{"directory":"me_overlay","formats":[46,46]}]}}
                """));
        addModelEngineItem(files, "leather_horse_armor", 17, "horse_armor");
        addModelEngineItem(files, "player_head", 31, "player_head");
        files.put("me_overlay/assets/modelengine/textures/item/me_horse_armor.png", onePixelPng());
        return files;
    }

    private void addModelEngineItem(Map<String, byte[]> files, String item, int customModelData, String modelId)
            throws IOException {
        String model = "modelengine:item/me_" + modelId;
        String vanillaModel = item.equals("player_head") ? "item/template_skull" : "item/generated";
        JsonObject legacy = new JsonObject();
        legacy.addProperty("parent", vanillaModel);
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", item.equals("player_head")
                ? "minecraft:item/template_skull" : "minecraft:item/" + item);
        legacy.add("textures", textures);
        JsonArray overrides = new JsonArray();
        JsonObject override = new JsonObject();
        JsonObject predicate = new JsonObject();
        predicate.addProperty("custom_model_data", customModelData);
        override.add("predicate", predicate);
        override.addProperty("model", model);
        overrides.add(override);
        legacy.add("overrides", overrides);
        files.put("assets/minecraft/models/item/" + item + ".json", bytes(legacy.toString()));

        JsonObject customModel = new JsonObject();
        customModel.addProperty("parent", "minecraft:item/generated");
        JsonObject customTextures = new JsonObject();
        customTextures.addProperty("layer0", "modelengine:item/me_" + modelId);
        customModel.add("textures", customTextures);
        files.put("assets/modelengine/models/item/me_" + modelId + ".json", bytes(customModel.toString()));
        files.put("assets/modelengine/textures/item/me_" + modelId + ".png", onePixelPng());
    }

    private List<VirtualFile> generatedOraxenFiles() throws Exception {
        List<VirtualFile> files = new ArrayList<>();
        addGeneratedOraxenItem(files, Material.LEATHER_HORSE_ARMOR, "horse_armor", 23);
        addGeneratedOraxenItem(files, Material.PLAYER_HEAD, "player_head", 41);
        return files;
    }

    private void addGeneratedOraxenItem(List<VirtualFile> files, Material material, String id, int customModelData)
            throws Exception {
        String modelPath = "oraxen:item/custom_" + id;
        OraxenMeta meta = new OraxenMeta();
        meta.setModelName(modelPath);
        meta.setCustomModelData(customModelData);
        Field hasPackInfos = OraxenMeta.class.getDeclaredField("hasPackInfos");
        hasPackInfos.setAccessible(true);
        hasPackInfos.setBoolean(meta, true);

        ItemBuilder item = mock(ItemBuilder.class);
        when(item.getOraxenMeta()).thenReturn(meta);
        when(item.getCustomTag(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(id);

        files.add(file("assets/minecraft/models/item/" + material.name().toLowerCase() + ".json",
                generatedLegacyModel(material, id, customModelData)));

        PredicatesGenerator predicates = mock(PredicatesGenerator.class);
        when(predicates.getVanillaModelName(any(Material.class)))
                .thenReturn("item/" + material.name().toLowerCase());
        VanillaItemDefinitionGenerator modern = new VanillaItemDefinitionGenerator(
                material, List.of(item), predicates, false, false);
        files.add(file("assets/minecraft/items/" + modern.getFileName(), modern.toJSON().toString()));
        files.add(file("assets/oraxen/items/" + id + ".json", new ModelDefinitionGenerator(meta, material).toJSON().toString()));

        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "oraxen:item/custom_" + id);
        model.add("textures", textures);
        files.add(file("assets/oraxen/models/item/custom_" + id + ".json", model.toString()));
        files.add(new VirtualFile("assets/oraxen/textures/item", "custom_" + id + ".png",
                new ByteArrayInputStream(onePixelPng())));
    }

    private String generatedLegacyModel(Material material, String id, int customModelData) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", material == Material.PLAYER_HEAD ? "item/template_skull" : "item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "item/" + material.name().toLowerCase());
        model.add("textures", textures);
        JsonArray overrides = new JsonArray();
        JsonObject override = new JsonObject();
        JsonObject predicate = new JsonObject();
        predicate.addProperty("custom_model_data", customModelData);
        override.add("predicate", predicate);
        override.addProperty("model", "oraxen:item/custom_" + id);
        overrides.add(override);
        model.add("overrides", overrides);
        return model.toString();
    }

    private Map<String, byte[]> writeVersionPack(List<VirtualFile> merged, JsonArray overlays,
            int packFormat, int minFormat, int maxFormat) throws IOException {
        List<VirtualFile> packFiles = new ArrayList<>();
        for (VirtualFile input : merged) {
            try (var stream = input.getInputStream()) {
                byte[] content = stream.readAllBytes();
                input.setInputStream(new ByteArrayInputStream(content));
                packFiles.add(file(input.getPath(), content));
            }
        }
        JsonObject metadata = PackMcmetaUtils.createPackMcmeta(packFormat, minFormat, maxFormat, null);
        if (!overlays.isEmpty()) {
            JsonObject overlayMetadata = new JsonObject();
            overlayMetadata.add("entries", overlays.deepCopy());
            metadata.add("overlays", overlayMetadata);
        }
        packFiles.add(file("pack.mcmeta", bytes(metadata.toString())));

        Path zip = tempDir.resolve("pack_" + packFormat + ".zip");
        try (var output = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
            for (VirtualFile packFile : packFiles) {
                output.putNextEntry(new java.util.zip.ZipEntry(packFile.getPath()));
                try (var input = packFile.getInputStream()) {
                    output.write(input.readAllBytes());
                }
                output.closeEntry();
            }
        }
        return readZip(zip);
    }

    private Map<String, byte[]> readZip(Path zipFile) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipFile), StandardCharsets.UTF_8)) {
            var entry = zip.getNextEntry();
            while (entry != null) {
                files.put(entry.getName(), zip.readAllBytes());
                zip.closeEntry();
                entry = zip.getNextEntry();
            }
        }
        return files;
    }

    private void assertLegacyOverrides(Map<String, byte[]> files, String item, int modelEngineData,
            int oraxenData, String id, boolean obfuscate) {
        JsonObject legacyDefinition = json(files, "assets/minecraft/models/item/" + item + ".json");
        String vanillaParent = item.equals("player_head") ? "item/template_skull" : "item/generated";
        String vanillaTexture = item.equals("player_head") ? "item/player_head" : "item/" + item;
        assertEquals(vanillaParent, legacyDefinition.get("parent").getAsString());
        assertEquals(vanillaTexture, legacyDefinition.getAsJsonObject("textures").get("layer0").getAsString());

        JsonArray overrides = legacyDefinition.getAsJsonArray("overrides");
        String modelEngineModel = legacyModel(overrides, modelEngineData);
        String oraxenModel = legacyModel(overrides, oraxenData);
        if (!obfuscate) {
            assertEquals("modelengine:item/me_" + id, modelEngineModel);
            assertEquals("oraxen:item/custom_" + id, oraxenModel);
        } else {
            assertModelReferenceExists(modelEngineModel, files);
            assertModelReferenceExists(oraxenModel, files);
            assertNotEquals(modelEngineModel, oraxenModel);
        }
    }

    private void assertModernOverrides(Map<String, byte[]> files, String item, int modelEngineData,
            int oraxenData, String id, boolean obfuscate) {
        JsonObject definition = json(files, "assets/minecraft/items/" + item + ".json");
        JsonObject model = definition.getAsJsonObject("model");
        assertEquals("minecraft:range_dispatch", model.get("type").getAsString());
        assertEquals("minecraft:custom_model_data", model.get("property").getAsString());
        assertEquals(1, countCustomModelDispatchers(model));
        String modelEngineModel = modernModel(model, modelEngineData);
        String oraxenModel = modernModel(model, oraxenData);
        if (!obfuscate) {
            assertEquals("modelengine:item/me_" + id, modelEngineModel);
            assertEquals("oraxen:item/custom_" + id, oraxenModel);
        } else {
            assertModelReferenceExists(modelEngineModel, files);
            assertModelReferenceExists(oraxenModel, files);
            assertNotEquals(modelEngineModel, oraxenModel);
        }

        if (item.equals("leather_horse_armor")) {
            JsonObject fallback = model.getAsJsonObject("fallback");
            assertEquals("minecraft:item/leather_horse_armor", fallback.get("model").getAsString());
            assertEquals(0xFFA06540, fallback.getAsJsonArray("tints").get(0).getAsJsonObject().get("default").getAsInt());
        } else {
            JsonObject fallback = model.getAsJsonObject("fallback");
            assertEquals("minecraft:special", fallback.get("type").getAsString());
            assertEquals("minecraft:item/template_skull", fallback.get("base").getAsString());
            assertEquals("minecraft:player_head", fallback.getAsJsonObject("model").get("type").getAsString());
        }
    }

    private String legacyModel(JsonArray overrides, int customModelData) {
        for (JsonElement element : overrides) {
            JsonObject override = element.getAsJsonObject();
            if (override.getAsJsonObject("predicate").get("custom_model_data").getAsInt() == customModelData)
                return override.get("model").getAsString();
        }
        fail("Missing legacy custom_model_data " + customModelData);
        return null;
    }

    private String modernModel(JsonObject dispatch, int customModelData) {
        for (JsonElement element : dispatch.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            if (entry.get("threshold").getAsInt() == customModelData)
                return entry.getAsJsonObject("model").get("model").getAsString();
        }
        fail("Missing modern custom_model_data " + customModelData);
        return null;
    }

    private int countCustomModelDispatchers(JsonElement model) {
        if (model.isJsonArray()) return model.getAsJsonArray().asList().stream().mapToInt(this::countCustomModelDispatchers).sum();
        if (!model.isJsonObject()) return 0;
        JsonObject object = model.getAsJsonObject();
        int count = object.has("type") && object.get("type").getAsString().equals("minecraft:range_dispatch")
                && object.has("property") && object.get("property").getAsString().equals("minecraft:custom_model_data") ? 1 : 0;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) count += countCustomModelDispatchers(entry.getValue());
        return count;
    }

    private void assertPackReferencesResolve(Map<String, byte[]> files) {
        for (var entry : files.entrySet()) {
            if (!entry.getKey().endsWith(".json")) continue;
            JsonElement json = JsonParser.parseString(new String(entry.getValue(), StandardCharsets.UTF_8));
            assertReferencesResolve(json, null, entry.getKey().contains("/assets/")
                    ? entry.getKey().substring(0, entry.getKey().indexOf("/assets/") + 1) : "", files);
        }
    }

    private void assertReferencesResolve(JsonElement element, String key, String prefix, Map<String, byte[]> files) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet())
                assertReferencesResolve(entry.getValue(), entry.getKey(), prefix, files);
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) assertReferencesResolve(value, key, prefix, files);
            return;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString() || key == null) return;
        String value = element.getAsString();
        if (key.equals("model") || key.equals("parent") || key.equals("base")) {
            String path = namespacedPath(value, "/models/", ".json");
            if (path != null && !value.startsWith("minecraft:")) assertTrue(files.containsKey(prefix + path), prefix + path);
        } else if (key.equals("texture") || key.equals("particle") || key.equals("layer0") || key.equals("layer1")) {
            String path = namespacedPath(value, "/textures/", ".png");
            if (path != null && !value.startsWith("minecraft:")) assertTrue(files.containsKey(prefix + path), prefix + path);
        }
    }

    private String namespacedPath(String reference, String directory, String extension) {
        int separator = reference.indexOf(':');
        if (separator < 0) return null;
        String namespace = reference.substring(0, separator);
        String path = reference.substring(separator + 1);
        return "assets/" + namespace + directory + path + extension;
    }

    private void assertModelReferenceExists(String reference, Map<String, byte[]> files) {
        String path = namespacedPath(reference, "/models/", ".json");
        assertNotNull(path, reference);
        assertTrue(files.containsKey(path), path);
    }

    private void assertTextureFilesArePng(Map<String, byte[]> files) throws IOException {
        for (var entry : files.entrySet()) {
            if (!entry.getKey().endsWith(".png")) continue;
            assertNotNull(ImageIO.read(new ByteArrayInputStream(entry.getValue())), entry.getKey());
        }
    }

    private byte[] onePixelPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private JsonObject json(Map<String, byte[]> files, String path) {
        assertTrue(files.containsKey(path), "Missing " + path);
        String content = new String(files.get(path), StandardCharsets.UTF_8);
        return JsonParser.parseString(content).getAsJsonObject();
    }

    private VirtualFile file(String path, String content) {
        return file(path, bytes(content));
    }

    private VirtualFile file(String path, byte[] content) {
        int separator = path.lastIndexOf('/');
        return new VirtualFile(separator < 0 ? "" : path.substring(0, separator), path.substring(separator + 1),
                new ByteArrayInputStream(content));
    }

    private void writePack(Path target, Map<String, byte[]> files) throws IOException {
        Files.createDirectories(target.getParent());
        try (var output = new java.util.zip.ZipOutputStream(Files.newOutputStream(target))) {
            for (var entry : files.entrySet()) {
                output.putNextEntry(new java.util.zip.ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
    }

    private byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
