package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
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

    private static class TexturePath {
        public final String namespace;
        public final String path;

        public TexturePath(String namespace, String path) {
            this.namespace = namespace;
            this.path = path.replace(".png", "");
        }

        public TexturePath(String path) {
            this.namespace = path.contains(":") ? StringUtils.substringBefore(path, ":") : "minecraft";
            this.path = (path.contains(":") ? path.split(":")[1] : path).replace(".png", "");
        }

        public String toPath() {
            return namespace + ":" + path;
        }

        public boolean in(Collection<TexturePath> texturePaths) {
            return texturePaths.stream().anyMatch(texturePath -> namespace.equals(texturePath.namespace) && path.equals(texturePath.path));
        }
    }

    public static void generateAtlasFile(List<VirtualFile> output, Set<String> malformedTextures) {
        Logs.logSuccess("Generating atlas-file for 1.19.3+ Resource Pack format");
        if (Settings.EXCLUDE_MALFORMED_ATLAS.toBool() && !malformedTextures.isEmpty())
            Logs.logWarning("Attempting to exclude malformed textures from atlas-file");

        JsonObject atlas = new JsonObject();
        JsonArray atlasContent = new JsonArray();
        LinkedHashMap<VirtualFile, TexturePath> textureSubFolders = new LinkedHashMap<>();
        for (VirtualFile v : output.stream().filter(v ->
                v.getPath().split("/").length > 3
                        && v.getPath().split("/")[2].equals("textures")
                        && v.getPath().endsWith(".png")
                        && !v.getPath().endsWith("_layer_1.png")
                        && !v.getPath().endsWith("_layer_2.png")
                        && PackSlicer.INPUTS.stream().noneMatch(input -> v.getPath().endsWith(input.path))
                        && PackSlicer.OUTPUT_PATHS.stream().noneMatch(outPath -> v.getPath().endsWith(outPath))
        ).sorted().collect(Collectors.toCollection(LinkedHashSet::new))) {
            textureSubFolders.put(v, new TexturePath(StringUtils.substringBetween(v.getPath(), "assets/", "/textures"), StringUtils.substringAfter(v.getPath(), "textures/")));
        }

        Set<TexturePath> itemTextures = new HashSet<>();
        OraxenItems.getItems().stream().filter(builder -> builder.hasOraxenMeta() && builder.getOraxenMeta().hasLayers())
                .forEach(builder -> itemTextures.addAll(builder.getOraxenMeta().getLayers().stream().map(TexturePath::new).toList()));

        Set<TexturePath> fontTextures = new LinkedHashSet<>();
        Set<JsonObject> fonts = output.stream().filter(v -> v.getPath().matches("assets/.*/font/.*.json") && v.isJsonObject()).map(VirtualFile::toJsonObject).collect(Collectors.toSet());
        for (JsonObject font : fonts) {
            if (font == null || !font.has("providers")) continue;
            for (JsonElement providerElement : font.getAsJsonArray("providers")) {
                if (!providerElement.isJsonObject()) continue;
                JsonObject provider = providerElement.getAsJsonObject();
                if (provider.has("file")) {
                    fontTextures.add(new TexturePath(provider.get("file").getAsString()));
                }
            }
        }

        for (Map.Entry<VirtualFile, TexturePath> entry : textureSubFolders.entrySet()) {
            TexturePath texturePath = entry.getValue();

            // If a texture is for a font, do not add it to atlas, unless an item uses it
            // Default example is required/exit_icon.png
            if (texturePath.in(fontTextures) && !texturePath.in(itemTextures)) continue;

            JsonObject atlasEntry = new JsonObject();
            String malformPathCheck = "assets/" + texturePath.namespace + "/textures/" + texturePath.path + ".png";
            if (Settings.EXCLUDE_MALFORMED_ATLAS.toBool() && malformedTextures.stream().anyMatch(malformPathCheck::startsWith)) {
                Logs.logWarning("Excluding malformed texture from atlas-file: <gold>" + malformPathCheck);
                continue;
            }

            if (Settings.ATLAS_GENERATION_TYPE.toString().equals("DIRECTORY")) {
                String path = StringUtils.substringBeforeLast(texturePath.path, "/");
                atlasEntry.addProperty("type", "directory");
                atlasEntry.addProperty("source", path);
                atlasEntry.addProperty("prefix", path + "/");
            } else {
                atlasEntry.addProperty("type", "single");
                atlasEntry.addProperty("resource", texturePath.toPath());
                atlasEntry.addProperty("sprite", texturePath.toPath());
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
