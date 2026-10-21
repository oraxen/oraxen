package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.pack.generation.AtlasDependencyResolver.AtlasResolutionResult;
import io.th0rgal.oraxen.pack.generation.AtlasDependencyResolver.ModelRenderKind;
import io.th0rgal.oraxen.pack.generation.AtlasDependencyResolver.TextureRef;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AtlasDependencyResolverTest {

    private static VirtualFile virtualFile(String folder, String name, String content) {
        return new VirtualFile(folder, name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void separates2DItemTexturesAndBridges3DVanillaItemReferences() {
        List<VirtualFile> files = new ArrayList<>();

        // 1. Custom 2D item texture + model
        files.add(virtualFile("assets/oraxen/textures/item", "ruby.png", "PNG_BYTES"));
        files.add(virtualFile("assets/oraxen/models/item", "ruby.json", """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "oraxen:item/ruby"
                  }
                }
                """));

        // 2. Custom 3D furniture model that references both a custom texture and a vanilla item texture
        files.add(virtualFile("assets/oraxen/textures/furniture", "table_oak.png", "PNG_BYTES"));
        files.add(virtualFile("assets/oraxen/models/furniture", "table.json", """
                {
                  "textures": {
                    "base": "oraxen:furniture/table_oak",
                    "accent": "#Trim",
                    "Trim": "minecraft:item/diamond"
                  },
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [16, 16, 16],
                      "faces": {
                        "up": { "texture": "#base" },
                        "north": { "texture": "#accent" }
                      }
                    }
                  ]
                }
                """));

        // 3. Font-only glyph texture + font JSON
        files.add(virtualFile("assets/oraxen/textures/font", "emoji_smile.png", "PNG_BYTES"));
        files.add(virtualFile("assets/minecraft/font", "default.json", """
                {
                  "providers": [
                    {
                      "type": "bitmap",
                      "file": "oraxen:font/emoji_smile.png",
                      "ascent": 7,
                      "chars": ["\\uE001"]
                    }
                  ]
                }
                """));

        AtlasDependencyResolver resolver = new AtlasDependencyResolver();
        AtlasResolutionResult result = resolver.resolve(files, Collections.emptySet(), Collections.emptySet());

        TextureRef rubyRef = new TextureRef("oraxen", "item/ruby");
        TextureRef tableRef = new TextureRef("oraxen", "furniture/table_oak");
        TextureRef diamondRef = new TextureRef("minecraft", "item/diamond");
        TextureRef emojiRef = new TextureRef("oraxen", "font/emoji_smile");

        // Ruby is a 2D item texture -> present in both itemAtlasTextures (for 1.21.11+/26.x) and blockAtlasTextures (legacy compatibility)
        assertTrue(result.itemAtlasTextures().contains(rubyRef));
        assertTrue(result.blockAtlasTextures().contains(rubyRef));

        // Table is a 3D model texture -> in blockAtlasTextures, NOT in itemAtlasTextures
        assertTrue(result.blockAtlasTextures().contains(tableRef));
        assertFalse(result.itemAtlasTextures().contains(tableRef));

        // Vanilla item/diamond used on a 3D model is bridged into blockAtlasTextures so 1.21.11+/26.x does not fail with multi-atlas error
        assertTrue(result.bridgedVanillaItemTextures().contains(diamondRef));
        assertTrue(result.blockAtlasTextures().contains(diamondRef));

        // Font-only texture is excluded from both atlases
        assertTrue(result.fontOnlyTextures().contains(emojiRef));
        assertFalse(result.blockAtlasTextures().contains(emojiRef));
        assertFalse(result.itemAtlasTextures().contains(emojiRef));
        assertTrue(result.missingTexturesByModel().isEmpty());
    }

    @Test
    void detectsMissingCustomTexturesInModelHierarchy() {
        List<VirtualFile> files = new ArrayList<>();
        files.add(virtualFile("assets/oraxen/models/block", "parent_cube.json", """
                {
                  "parent": "minecraft:block/cube_all",
                  "textures": {
                    "all": "#surface"
                  }
                }
                """));
        files.add(virtualFile("assets/oraxen/models/block", "broken_ore.json", """
                {
                  "parent": "oraxen:block/parent_cube",
                  "textures": {
                    "surface": "oraxen:block/non_existent_ore"
                  }
                }
                """));

        AtlasDependencyResolver resolver = new AtlasDependencyResolver();
        AtlasResolutionResult result = resolver.resolve(files, Collections.emptySet(), Collections.emptySet());

        assertTrue(result.missingTexturesByModel().containsKey("oraxen:block/broken_ore"));
        assertTrue(result.missingTexturesByModel().get("oraxen:block/broken_ore")
                .contains(new TextureRef("oraxen", "block/non_existent_ore")));
    }

    @Test
    void atlasGeneratorEmitsBlocksAndItemsAtlasesForSplitAtlasVersions() {
        List<VirtualFile> files = new ArrayList<>();
        files.add(virtualFile("assets/oraxen/textures/item", "emerald_staff.png", "PNG_BYTES"));
        files.add(virtualFile("assets/oraxen/models/item", "emerald_staff.json", """
                {
                  "parent": "minecraft:item/handheld",
                  "textures": {
                    "layer0": "oraxen:item/emerald_staff"
                  }
                }
                """));

        AtlasGenerator.generateAtlasFiles(files, Collections.emptySet(), true);

        PackAssetIndex index = new PackAssetIndex(files);
        VirtualFile blocksAtlas = index.getFirst("assets/minecraft/atlases/blocks.json");
        VirtualFile itemsAtlas = index.getFirst("assets/minecraft/atlases/items.json");

        assertNotNull(blocksAtlas);
        assertNotNull(itemsAtlas);

        JsonObject itemsJson = AtlasDependencyResolver.readJsonObjectNonDestructive(itemsAtlas);
        assertNotNull(itemsJson);
        JsonArray sources = itemsJson.getAsJsonArray("sources");
        assertNotNull(sources);
        boolean foundStaff = false;
        for (JsonElement el : sources) {
            JsonObject obj = el.getAsJsonObject();
            if ("single".equals(obj.get("type").getAsString())
                    && "oraxen:item/emerald_staff".equals(obj.get("resource").getAsString())) {
                foundStaff = true;
            }
        }
        assertTrue(foundStaff);

        JsonObject blocksJson = AtlasDependencyResolver.readJsonObjectNonDestructive(blocksAtlas);
        assertNotNull(blocksJson);
        for (JsonElement el : blocksJson.getAsJsonArray("sources")) {
            JsonObject obj = el.getAsJsonObject();
            if ("single".equals(obj.get("type").getAsString())) {
                assertFalse("oraxen:item/emerald_staff".equals(obj.get("resource").getAsString()),
                        "2D item sprite must not be duplicated in blocks.json on 1.21.11+/26.x");
            }
        }
    }

    @Test
    void packAssetIndexFindsDuplicatesAndNormalizesPaths() {
        PackAssetIndex index = new PackAssetIndex();
        index.add(virtualFile("assets//minecraft//font/", "default.json", "{\"providers\":[]}"));
        index.add(virtualFile("/assets/minecraft/font", "default.json", "{\"providers\":[]}"));
        index.add(virtualFile("assets/minecraft/font", "uniform.json", "{\"providers\":[]}"));

        assertEquals(2, index.getAll("assets/minecraft/font/default.json").size());
        Map<String, List<VirtualFile>> dupes = index.findDuplicates(p -> p.endsWith(".json"));
        assertEquals(1, dupes.size());
        assertTrue(dupes.containsKey("assets/minecraft/font/default.json"));

        // Root-level files without slashes (e.g. pack.mcmeta, pack.png) must be indexed without StringIndexOutOfBoundsException
        index.add(virtualFile("", "pack.mcmeta", "{\"pack\":{\"pack_format\":46}}"));
        index.add(virtualFile("", "pack.png", "PNG"));
        assertNotNull(index.getFirst("pack.mcmeta"));
        assertEquals("pack.mcmeta", index.getFirst("pack.mcmeta").getPath());
        assertNotNull(index.getFirst("pack.png"));
        assertEquals("pack.png", index.getFirst("pack.png").getPath());
    }
}
