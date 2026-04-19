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

    private static VirtualFile file(String path, String content) {
        int slash = path.lastIndexOf('/');
        return new VirtualFile(path.substring(0, slash), path.substring(slash + 1),
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
