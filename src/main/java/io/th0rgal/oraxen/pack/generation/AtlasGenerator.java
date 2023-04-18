package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class AtlasGenerator {

    public AtlasGenerator() {
    }

    public static void generateAtlasFile(List<VirtualFile> output, Set<String> malformedTextures) {
        Logs.logSuccess("Generating atlas-file for 1.19.3+ Resource Pack format");
        if (Settings.EXCLUDE_MALFORMED_ATLAS.toBool())
            Logs.logWarning("Attempting to exclude malformed textures from atlas-file");

        JsonObject atlas = new JsonObject();
        JsonArray atlasContent = new JsonArray();
        LinkedHashMap<VirtualFile, String> textureSubFolders = new LinkedHashMap<>();
        for (VirtualFile v : output.stream().filter(v ->
                v.getPath().split("/").length > 3
                        && v.getPath().split("/")[2].equals("textures")
                        && v.getPath().endsWith(".png")
                        && !v.getPath().endsWith("_layer_1.png")
                        && !v.getPath().endsWith("_layer_2.png")
        ).sorted().collect(Collectors.toCollection(LinkedHashSet::new))) {
            textureSubFolders.put(v, Utils.removeExtensionOnly(v.getPath().replaceFirst("assets/.*/textures/", "")));
        }

        Set<String> itemTextures = new HashSet<>();
        OraxenItems.getItems().stream().filter(builder -> builder.hasOraxenMeta() && builder.getOraxenMeta().hasLayers())
                .forEach(builder -> itemTextures.addAll(builder.getOraxenMeta().getLayers()));

        Set<String> glyphTextures = new LinkedHashSet<>();
        OraxenPlugin.get().getFontManager().getGlyphs()
                .forEach(glyph -> glyphTextures.add(glyph.getTexture().replace(".png", "")));

        for (Map.Entry<VirtualFile, String> entry : textureSubFolders.entrySet()) {
            VirtualFile virtual = entry.getKey();
            String path = entry.getValue();

            // If a texture is for a glyph, do not add it to atlas, unless an item uses it
            // Default example is required/exit_icon.png
            if (glyphTextures.contains(path) && !itemTextures.contains(path)) continue;

            JsonObject atlasEntry = new JsonObject();
            String namespace = virtual.getPath().replaceFirst("assets/", "").split("/")[0];
            if (Settings.ATLAS_GENERATION_TYPE.toString().equals("DIRECTORY")) {
                if (path.endsWith(".png")) {
                    String sprite = Utils.removeParentDirs(Utils.removeExtension(virtual.getPath()));
                    atlasEntry.addProperty("type", "single");
                    atlasEntry.addProperty("resource", namespace + ":" + Utils.removeExtension(path));
                    atlasEntry.addProperty("sprite", namespace + ":" + sprite);
                } else {
                    atlasEntry.addProperty("type", "directory");
                    atlasEntry.addProperty("source", path);
                    atlasEntry.addProperty("prefix", path + "/");
                }
            } else {
                if (Settings.EXCLUDE_MALFORMED_ATLAS.toBool() && malformedTextures.contains("assets/" + namespace + "/textures/" + path)) continue;
                String sprite = namespace + ":" + path;
                atlasEntry.addProperty("type", "single");
                atlasEntry.addProperty("resource", sprite);
                atlasEntry.addProperty("sprite", sprite);
            }

            if (!atlasContent.contains(atlasEntry))
                atlasContent.add(atlasEntry);
        }

        atlas.add("sources", atlasContent);
        VirtualFile atlasFile = new VirtualFile("assets/minecraft/atlases", "blocks.json", new ByteArrayInputStream(atlas.toString().getBytes(StandardCharsets.UTF_8)));
        output.removeIf(v -> v.getPath().equals(atlasFile.getPath()));
        output.add(atlasFile);
    }
}
