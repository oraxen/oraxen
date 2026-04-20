package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void vanillaTextureOnlyOverridesKeepOriginalPathWhenUnreferenced() {
        List<VirtualFile> files = new ArrayList<>();
        files.add(file("assets/minecraft/textures/item/diamond_sword.png", "png"));
        files.add(file("assets/minecraft/textures/item/diamond_sword.png.mcmeta", "{}"));
        files.add(file("assets/minecraft/textures/default/sword.png", "png"));
        files.add(file("assets/oraxen/models/default/sword.json", """
                {
                  "textures": {
                    "layer0": "default/sword"
                  }
                }
                """));
        files.add(file("assets/oraxen/textures/default/sword.png", "png"));

        PackObfuscator.obfuscate(files, "SIMPLE", false);

        assertTrue(files.stream().anyMatch(file -> file.getPath().equals("assets/minecraft/textures/item/diamond_sword.png")));
        assertTrue(files.stream().anyMatch(file -> file.getPath().equals("assets/minecraft/textures/item/diamond_sword.png.mcmeta")));
        assertTrue(files.stream().anyMatch(file -> file.getPath().equals("assets/minecraft/textures/default/sword.png")));
    }

    @Test
    void knownTexturePropertyDoesNotFallBackToModelKey() throws Exception {
        List<VirtualFile> files = new ArrayList<>();
        files.add(file("assets/oraxen/models/default/holder.json", """
                {
                  "textures": {
                    "layer0": "default/model_only"
                  }
                }
                """));
        files.add(file("assets/oraxen/models/default/model_only.json", "{}"));

        PackObfuscator.obfuscate(files, "SIMPLE", false);

        String layer0 = null;
        for (VirtualFile file : files) {
            if (!file.getPath().contains("/models/")) continue;
            String content = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            if (json.has("textures")) {
                layer0 = json.getAsJsonObject("textures").get("layer0").getAsString();
                break;
            }
        }

        assertEquals("default/model_only", layer0);
    }

    @Test
    void itemSelectWhenValuesAreNotRewrittenAsModelReferences() throws Exception {
        List<VirtualFile> files = new ArrayList<>();
        files.add(file("assets/minecraft/items/diamond_sword.json", """
                {
                  "model": {
                    "type": "minecraft:select",
                    "property": "minecraft:custom_model_data",
                    "cases": [
                      {
                        "when": "oraxen:sword",
                        "model": {
                          "type": "minecraft:model",
                          "model": "oraxen:sword"
                        }
                      }
                    ]
                  }
                }
                """));
        files.add(file("assets/oraxen/models/sword.json", "{}"));

        PackObfuscator.obfuscate(files, "SIMPLE", false);

        VirtualFile itemFile = files.stream()
                .filter(file -> file.getPath().equals("assets/minecraft/items/diamond_sword.json"))
                .findFirst()
                .orElseThrow();
        String content = new String(itemFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject item = JsonParser.parseString(content).getAsJsonObject();
        JsonObject selectCase = item.getAsJsonObject("model").getAsJsonArray("cases").get(0).getAsJsonObject();
        String when = selectCase.get("when").getAsString();
        String model = selectCase.getAsJsonObject("model").get("model").getAsString();

        assertEquals("oraxen:sword", when);
        assertTrue(model.startsWith("oraxen:m/"), model);
    }

    @Test
    void vanillaSoundOnlyOverridesKeepOriginalPathWhenUnreferenced() {
        List<VirtualFile> files = new ArrayList<>();
        files.add(file("assets/minecraft/sounds/item/trident/throw.ogg", "ogg"));
        files.add(file("assets/minecraft/sounds/custom/sound.ogg", "ogg"));
        files.add(file("assets/oraxen/sounds.json", """
                {
                  "custom.sound": {
                    "sounds": ["custom/sound"]
                  }
                }
                """));
        files.add(file("assets/oraxen/sounds/custom/sound.ogg", "ogg"));

        PackObfuscator.obfuscate(files, "SIMPLE", false);

        assertTrue(files.stream().anyMatch(file -> file.getPath().equals("assets/minecraft/sounds/item/trident/throw.ogg")));
        assertTrue(files.stream().anyMatch(file -> file.getPath().equals("assets/minecraft/sounds/custom/sound.ogg")));
        assertTrue(files.stream().anyMatch(file -> file.getPath().startsWith("assets/oraxen/sounds/s/")));
    }

    @Test
    void namePropertiesOutsideSoundsJsonCanRewriteModelReferences() throws Exception {
        List<VirtualFile> files = new ArrayList<>();
        files.add(file("assets/oraxen/models/default/holder.json", """
                {
                  "name": "default/model"
                }
                """));
        files.add(file("assets/oraxen/models/default/model.json", "{}"));

        PackObfuscator.obfuscate(files, "SIMPLE", false);

        String name = null;
        for (VirtualFile file : files) {
            if (!file.getPath().contains("/models/")) continue;
            String content = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            if (json.has("name")) {
                name = json.get("name").getAsString();
                break;
            }
        }

        assertTrue(name.startsWith("oraxen:m/"), name);
    }

    @Test
    void atlasSinglesAreOnlyAddedToBlocksAtlas() throws Exception {
        List<VirtualFile> files = new ArrayList<>();
        files.add(file("assets/minecraft/atlases/blocks.json", "{\"sources\":[]}"));
        files.add(file("assets/minecraft/atlases/paintings.json", "{\"sources\":[]}"));
        files.add(file("assets/oraxen/models/default/sword.json", """
                {
                  "textures": {
                    "layer0": "default/sword"
                  }
                }
                """));
        files.add(file("assets/oraxen/textures/default/sword.png", "png"));

        PackObfuscator.obfuscate(files, "SIMPLE", false);

        int blocksSources = -1;
        int paintingsSources = -1;
        for (VirtualFile file : files) {
            String path = file.getPath();
            if (!path.endsWith("/atlases/blocks.json") && !path.endsWith("/atlases/paintings.json")) continue;
            String content = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int sources = JsonParser.parseString(content).getAsJsonObject().getAsJsonArray("sources").size();
            if (path.endsWith("/atlases/blocks.json")) blocksSources = sources;
            if (path.endsWith("/atlases/paintings.json")) paintingsSources = sources;
        }

        assertEquals(1, blocksSources);
        assertEquals(0, paintingsSources);
    }

    @Test
    void nonBlockAtlasSinglesAreAddedForReferencedDirectories() throws Exception {
        List<VirtualFile> files = new ArrayList<>();
        files.add(file("assets/minecraft/atlases/paintings.json", """
                {
                  "sources": [
                    {
                      "type": "directory",
                      "source": "painting",
                      "prefix": "painting/"
                    }
                  ]
                }
                """));
        files.add(file("assets/minecraft/textures/painting/custom.png", "png"));
        files.add(file("assets/minecraft/textures/item/diamond_sword.png", "png"));

        PackObfuscator.obfuscate(files, "SIMPLE", false);

        VirtualFile atlasFile = files.stream()
                .filter(file -> file.getPath().endsWith("/atlases/paintings.json"))
                .findFirst()
                .orElseThrow();
        String content = new String(atlasFile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject atlas = JsonParser.parseString(content).getAsJsonObject();

        assertEquals(2, atlas.getAsJsonArray("sources").size());
        assertTrue(files.stream().anyMatch(file -> file.getPath().startsWith("assets/minecraft/textures/t/")));
        assertTrue(files.stream().anyMatch(file -> file.getPath().equals("assets/minecraft/textures/item/diamond_sword.png")));
    }

    private static VirtualFile file(String path, String content) {
        int slash = path.lastIndexOf('/');
        return new VirtualFile(path.substring(0, slash), path.substring(slash + 1),
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
