package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackObfuscatorTest {

    @Test
    void textureValuesPreferTextureKeysWhenModelAndTextureSharePath() throws Exception {
        List<VirtualFile> files = new ArrayList<>();
        files.add(file("assets/oraxen/models/default/sword.json", """
                {
                  "parent": "item/generated",
                  "textures": {
                    "layer0": "default/sword"
                  }
                }
                """));
        files.add(file("assets/oraxen/textures/default/sword.png", "png"));

        PackObfuscator.obfuscate(files, "SIMPLE", false);

        VirtualFile modelFile = files.stream()
                .filter(file -> file.getPath().contains("/models/"))
                .findFirst()
                .orElseThrow();
        String modelContent = new String(modelFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject model = JsonParser.parseString(modelContent).getAsJsonObject();
        String layer0 = model.getAsJsonObject("textures").get("layer0").getAsString();

        assertTrue(layer0.startsWith("oraxen:t/"), layer0);
        assertNotEquals("default/sword", layer0);
    }

    @Test
    void soundArrayValuesPreferSoundKeysWhenAssetsSharePath() throws Exception {
        List<VirtualFile> files = new ArrayList<>();
        files.add(file("assets/oraxen/sounds.json", """
                {
                  "default.sword": {
                    "sounds": [
                      "default/sword",
                      { "name": "default/sword" }
                    ]
                  }
                }
                """));
        files.add(file("assets/oraxen/models/default/sword.json", "{}"));
        files.add(file("assets/oraxen/textures/default/sword.png", "png"));
        files.add(file("assets/oraxen/sounds/default/sword.ogg", "ogg"));

        PackObfuscator.obfuscate(files, "SIMPLE", false);

        VirtualFile soundsFile = files.stream()
                .filter(file -> file.getPath().endsWith("/sounds.json"))
                .findFirst()
                .orElseThrow();
        String soundsContent = new String(soundsFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject sounds = JsonParser.parseString(soundsContent).getAsJsonObject()
                .getAsJsonObject("default.sword");
        String arrayValue = sounds.getAsJsonArray("sounds").get(0).getAsString();
        String objectValue = sounds.getAsJsonArray("sounds").get(1).getAsJsonObject().get("name").getAsString();

        assertTrue(arrayValue.startsWith("oraxen:s/"), arrayValue);
        assertEquals(arrayValue, objectValue);
    }

    @Test
    void genericValuesPreferModelKeysWhenModelAndTextureSharePath() throws Exception {
        List<VirtualFile> files = new ArrayList<>();
        files.add(file("assets/oraxen/models/default/holder.json", """
                {
                  "custom_model": "default/sword"
                }
                """));
        files.add(file("assets/oraxen/models/default/sword.json", "{}"));
        files.add(file("assets/oraxen/textures/default/sword.png", "png"));

        PackObfuscator.obfuscate(files, "SIMPLE", false);

        String customModel = null;
        for (VirtualFile file : files) {
            if (!file.getPath().contains("/models/")) continue;
            String content = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            if (json.has("custom_model")) {
                customModel = json.get("custom_model").getAsString();
                break;
            }
        }

        assertTrue(customModel.startsWith("oraxen:m/"), customModel);
    }

    private static VirtualFile file(String path, String content) {
        int slash = path.lastIndexOf('/');
        return new VirtualFile(path.substring(0, slash), path.substring(slash + 1),
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
