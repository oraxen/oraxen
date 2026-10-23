package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PackPipelineTest {
    @Test
    void snapshotProcessingDoesNotCopyBaseTranslationsOverOverlayTranslations() {
        List<VirtualFile> files = new ArrayList<>(List.of(
                file("assets/minecraft/lang/en_us.json", "{\"entry\":\"base\"}"),
                file("oraxen_import_0/assets/minecraft/lang/en_us.json", "{\"entry\":\"overlay\"}")));
        PackPipeline.processSnapshots(files, snapshot -> {
            assertEquals("assets/minecraft/lang/en_us.json", snapshot.getFirst().getPath());
            JsonObject result = PackPipeline.languageWithDefaults(json("{\"default\":\"global\"}"),
                    snapshot.getFirst().toJsonObject());
            snapshot.set(0, file(snapshot.getFirst().getPath(), result.toString()));
        });
        assertEquals("base", files.getFirst().toJsonObject().get("entry").getAsString());
        assertEquals("overlay", files.getLast().toJsonObject().get("entry").getAsString());
        assertEquals("oraxen_import_0/assets/minecraft/lang/en_us.json", files.getLast().getPath());
        assertTrue(files.getLast().toJsonObject().has("default"));
    }

    @Test
    void localTranslationsOverrideImportsAndGlobalDefaultsWithoutRemovingNamespaces() {
        List<VirtualFile> files = new ArrayList<>(List.of(
                file("assets/minecraft/lang/en_us.json", "{\"shared\":\"local\"}"),
                file("assets/minecraft/lang/en_us.json", "{\"shared\":\"import\",\"imported\":\"yes\"}"),
                file("assets/example/lang/en_us.json", "{\"other\":\"retained\"}")));
        PackPipeline.mergeNativeFiles(files, true);
        assertEquals(2, files.size());
        JsonObject locale = PackPipeline.languageWithDefaults(json("{\"shared\":\"global\",\"default\":\"yes\"}"),
                files.getFirst().toJsonObject());
        assertEquals("local", locale.get("shared").getAsString());
        assertTrue(locale.has("imported"));
        assertTrue(locale.has("default"));
        assertEquals("retained", files.getLast().toJsonObject().get("other").getAsString());
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
        return new VirtualFile(path.substring(0, slash), path.substring(slash + 1),
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
