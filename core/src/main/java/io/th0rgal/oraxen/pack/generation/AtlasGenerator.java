package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class AtlasGenerator {

    public AtlasGenerator() {
    }

    public static void generateAtlasFile(List<VirtualFile> output, Set<String> malformedTextures) {
        Logs.logSuccess("Generating atlas-file for 1.19.3+ Resource Pack format");
        if (Settings.EXCLUDE_MALFORMED_ATLAS.toBool() && !malformedTextures.isEmpty())
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
                        && PackSlicer.INPUTS.stream().noneMatch(input -> v.getPath().endsWith(input.path))
                        && PackSlicer.OUTPUT_PATHS.stream().noneMatch(outPath -> v.getPath().endsWith(outPath))
        ).sorted().collect(Collectors.toCollection(LinkedHashSet::new))) {
            textureSubFolders.put(v, Utils.removeExtensionOnly(v.getPath().replaceFirst("assets/.*/textures/", "")));
        }

        Set<String> itemTextures = new HashSet<>();
        OraxenItems.getItems().stream().filter(builder -> builder.hasOraxenMeta() && builder.getOraxenMeta().hasLayers())
                .forEach(builder -> itemTextures.addAll(builder.getOraxenMeta().getLayers()));

        Set<String> fontTextures = new LinkedHashSet<>();
        Set<VirtualFile> fonts = output.stream().filter(v -> v.getPath().matches("assets/.*/font/.*.json")).collect(Collectors.toSet());
        for (VirtualFile font : fonts) {
            JsonObject fontObject = font.toJsonElement().getAsJsonObject();
            fontObject.getAsJsonArray("providers").forEach(provider -> {
                JsonObject providerObject = provider.getAsJsonObject();
                if (providerObject.has("file")) {
                    fontTextures.add(providerObject.get("file").getAsString().replace(".png", ""));
                }
            });
        }

        for (Map.Entry<VirtualFile, String> entry : textureSubFolders.entrySet()) {
            VirtualFile virtual = entry.getKey();
            String path = entry.getValue();

            // If a texture is for a font, do not add it to atlas, unless an item uses it
            // Default example is required/exit_icon.png
            if (fontTextures.contains(path) && !itemTextures.contains(path)) continue;

            JsonObject atlasEntry = new JsonObject();
            String namespace = virtual.getPath().replaceFirst("assets/", "").split("/")[0];
            String malformPathCheck = "assets/" + namespace + "/textures/" + path + ".png";
            if (Settings.EXCLUDE_MALFORMED_ATLAS.toBool() && malformedTextures.stream().anyMatch(malformPathCheck::startsWith)) {
                Logs.logWarning("Excluding malformed texture from atlas-file: <gold>" + malformPathCheck);
                continue;
            }

            if (Settings.ATLAS_GENERATION_TYPE.toString().equals("DIRECTORY")) {
                path = StringUtils.substringBeforeLast(path, "/");
                atlasEntry.addProperty("type", "directory");
                atlasEntry.addProperty("source", path);
                atlasEntry.addProperty("prefix", path + "/");
            } else {
                String sprite = namespace + ":" + path;
                atlasEntry.addProperty("type", "single");
                atlasEntry.addProperty("resource", sprite);
                atlasEntry.addProperty("sprite", sprite);
            }

            if (!atlasContent.contains(atlasEntry))
                atlasContent.add(atlasEntry);
        }

        removeChildEntriesInDirectoryAtlas(atlasContent);

        atlas.add("sources", atlasContent);
        VirtualFile atlasFile = new VirtualFile("assets/minecraft/atlases", "blocks.json", new ByteArrayInputStream(atlas.toString().getBytes(StandardCharsets.UTF_8)));
        output.removeIf(v -> v.getPath().equals(atlasFile.getPath()));
        output.add(atlasFile);
        Logs.newline();
    }

    // If atlas contains entry for "parent"-source, remove following child-directory-entries like "parent/child"
    private static void removeChildEntriesInDirectoryAtlas(JsonArray atlasContent) {
        if (Settings.ATLAS_GENERATION_TYPE.toString().equals("DIRECTORY")) {
            Set<JsonElement> remove = new HashSet<>();
            atlasContent.forEach(element -> {
                String source = element.getAsJsonObject().get("source").getAsString();
                if (source.contains("/")) {
                    JsonObject parentObject = new JsonObject();
                    parentObject.addProperty("type", "directory");
                    parentObject.addProperty("source", source);
                    parentObject.addProperty("prefix", source + "/");
                    if (atlasContent.contains(parentObject))
                        remove.add(element);
                }
            });
            remove.forEach(atlasContent::remove);
        }
    }
}
