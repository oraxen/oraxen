package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PackPipelineTest {
    @Test
    void snapshotProcessingDoesNotCopyBaseTranslationsOverOverlayTranslations() {
        List<VirtualFile> files = new ArrayList<>(List.of(
                file("assets/minecraft/lang/en_us.json", "{\"entry\":\"base\"}"),
                file("oraxen_import_0/assets/minecraft/lang/en_us.json", "{\"entry\":\"overlay\"}")));
        PackPipeline.processSnapshots(files, snapshot -> {
            assertEquals("assets/minecraft/lang/en_us.json", snapshot.getFirst().getPath());
            JsonObject result = PackAssetMerger.mergeJson("assets/minecraft/lang/locale.json", json("{\"default\":\"global\"}"),
                    snapshot.getFirst().toJsonObject());
            snapshot.set(0, file(snapshot.getFirst().getPath(), result.toString()));
        });
        assertEquals("base", files.getFirst().toJsonObject().get("entry").getAsString());
        assertEquals("overlay", files.getLast().toJsonObject().get("entry").getAsString());
        assertEquals("oraxen_import_0/assets/minecraft/lang/en_us.json", files.getLast().getPath());
        assertTrue(files.getLast().toJsonObject().has("default"));
    }

    @Test
    void sparsifiesGeneratedOverlaysWithoutChangingTheirVisibleResources() throws IOException {
        String metadata = """
                {"overlays":{"entries":[
                  {"directory":"user_overlay","min_format":60,"max_format":70},
                  {"directory":"oraxen_import_0","min_format":60,"max_format":70}
                ]}}
                """;
        List<VirtualFile> files = new ArrayList<>(List.of(
                file("pack.mcmeta", metadata),
                file("assets/test/shared.txt", "base"),
                file("assets/test/unchanged.txt", "same"),
                file("assets/test/lang/en_us.json", "{\"hello\":\"base\"}"),
                file("user_overlay/assets/test/shared.txt", "native"),
                file("user_overlay/assets/test/native_only.txt", "native"),
                file("oraxen_import_0/assets/test/shared.txt", "base"),
                file("oraxen_import_0/assets/test/unchanged.txt", "same"),
                file("oraxen_import_0/assets/test/changed.txt", "changed"),
                file("oraxen_import_0/assets/test/lang/en_us.json", "{}")));
        JsonArray generatedEntries = JsonParser.parseString("""
                [{"directory":"oraxen_import_0","min_format":60,"max_format":70}]
                """).getAsJsonArray();

        Map<String, String> before = activeAssets(files, 65);
        PackPipeline.sparsifyGeneratedOverlays(files, generatedEntries);

        assertEquals(before, activeAssets(files, 65));
        assertFalse(files.stream().anyMatch(file -> file.getPath().equals("oraxen_import_0/assets/test/unchanged.txt")));
        assertTrue(files.stream().anyMatch(file -> file.getPath().equals("oraxen_import_0/assets/test/shared.txt")));
        assertTrue(files.stream().anyMatch(file -> file.getPath().equals("oraxen_import_0/assets/test/lang/en_us.json")));
    }

    @Test
    void localTranslationsOverrideImportsAndGlobalDefaultsWithoutRemovingNamespaces() {
        List<VirtualFile> files = new ArrayList<>(List.of(
                file("assets/minecraft/lang/en_us.json", "{\"shared\":\"local\"}"),
                file("assets/minecraft/lang/en_us.json", "{\"shared\":\"import\",\"imported\":\"yes\"}"),
                file("assets/example/lang/en_us.json", "{\"other\":\"retained\"}")));
        PackPipeline.mergeNativeFiles(files, true);
        assertEquals(2, files.size());
        JsonObject locale = PackAssetMerger.mergeJson("assets/minecraft/lang/locale.json", json("{\"shared\":\"global\",\"default\":\"yes\"}"),
                files.getFirst().toJsonObject());
        assertEquals("local", locale.get("shared").getAsString());
        assertTrue(locale.has("imported"));
        assertTrue(locale.has("default"));
        assertEquals("retained", files.getLast().toJsonObject().get("other").getAsString());
    }

    @Test
    void malformedLowerPriorityLocaleDoesNotPoisonHigherPriorityTranslations() {
        List<VirtualFile> files = new ArrayList<>(List.of(
                file("assets/minecraft/lang/en_us.json", "{\"shared\":\"local\"}"),
                file("assets/minecraft/lang/en_us.json", "{\"imported\":\"yes\",\"broken\":{\"nested\":\"value\"}}")));

        PackPipeline.mergeNativeFiles(files, true);

        assertEquals(1, files.size());
        assertEquals(json("{\"shared\":\"local\"}"), files.getFirst().toJsonObject());
    }

    @Test
    void soundEventsAppendInPackPriorityOrderAndRespectReplace() {
        List<VirtualFile> files = new ArrayList<>(List.of(
                file("assets/example/sounds.json", "{\"append\":{\"sounds\":[\"local\"]},\"reset\":{\"replace\":true,\"sounds\":[\"local\"]}}"),
                file("assets/example/sounds.json", "{\"append\":{\"sounds\":[\"import\"]},\"reset\":{\"sounds\":[\"import\"]}}")));
        PackPipeline.mergeNativeFiles(files, true);
        assertEquals(1, files.size());
        JsonObject result = files.getFirst().toJsonObject();
        assertEquals(json("{\"sounds\":[\"import\",\"local\"]}"), result.get("append"));
        assertEquals(1, result.getAsJsonObject("reset").getAsJsonArray("sounds").size());
    }

    @Test
    void atlasOperationsKeepTheirPriorityOrder() {
        List<VirtualFile> files = new ArrayList<>(List.of(
                file("assets/minecraft/atlases/blocks.json", "{\"sources\":[{\"type\":\"filter\",\"pattern\":{\"path\":\"removed\"}}]}"),
                file("assets/minecraft/atlases/blocks.json", "{\"sources\":[{\"type\":\"single\",\"resource\":\"example:removed\"}]}")));
        PackPipeline.mergeNativeFiles(files, true);
        var sources = files.getFirst().toJsonObject().getAsJsonArray("sources");
        assertEquals("single", sources.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("filter", sources.get(1).getAsJsonObject().get("type").getAsString());
    }

    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    private static VirtualFile file(String path, String content) {
        int slash = path.lastIndexOf('/');
        String parent = slash < 0 ? "" : path.substring(0, slash);
        String name = slash < 0 ? path : path.substring(slash + 1);
        return new VirtualFile(parent, name,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, String> activeAssets(List<VirtualFile> files, int format) throws IOException {
        Map<String, String> active = new HashMap<>();
        Map<String, VirtualFile> overlays = new HashMap<>();
        JsonObject metadata = null;
        for (VirtualFile file : files) {
            String path = file.getPath();
            if (path.equals("pack.mcmeta")) {
                metadata = json(read(file));
            } else if (path.startsWith("assets/")) {
                active.put(path, read(file));
            } else {
                overlays.put(path, file);
            }
        }
        if (metadata == null || !metadata.has("overlays")) return active;
        for (var element : metadata.getAsJsonObject("overlays").getAsJsonArray("entries")) {
            JsonObject declaration = element.getAsJsonObject();
            long[] range = PackMerger.range(declaration);
            long formatValue = (long) format << 31;
            if (range == null || formatValue < range[0] || formatValue > range[1]) continue;
            String prefix = declaration.get("directory").getAsString() + "/";
            for (var overlayFile : overlays.entrySet()) {
                if (!overlayFile.getKey().startsWith(prefix)) continue;
                active.put(overlayFile.getKey().substring(prefix.length()), read(overlayFile.getValue()));
            }
        }
        return active;
    }

    private static String read(VirtualFile file) throws IOException {
        InputStream input = file.getInputStream();
        if (input == null) return "";
        byte[] content;
        try (input) {
            content = input.readAllBytes();
        }
        file.setInputStream(new ByteArrayInputStream(content));
        return new String(content, StandardCharsets.UTF_8);
    }
}
