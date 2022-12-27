package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.io.IOUtils;
import org.bukkit.Material;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class PackConvertor {

    public PackConvertor() {
    }

    public static void handlePackConversionFor_1_19_3(List<VirtualFile> output) {
        Logs.logSuccess("Starting pack conversion for 1.19.3");
        convertBlocksPack_1_19_3(output);
        convertItemsPack_1_19_3(output);
        convertAnimPack_1_19_3(output);
        generateAtlasFile(output);
        Logs.logSuccess("Finished converting the resourcepack to 1.19.3 format");
    }

    private static void convertAnimPack_1_19_3(List<VirtualFile> output) {
        try {
            Set<VirtualFile> anims = output.stream().filter(v -> v.getPath().endsWith(".png.mcmeta")).collect(Collectors.toSet());
            for (VirtualFile virtualFile : anims) {
                String metaPath = virtualFile.getPath().split("assets/.*/textures/")[1];
                virtualFile.setPath("assets/oraxen_converted/textures/" + (!metaPath.startsWith("oraxen/") ? "oraxen/" : "") + metaPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO This will probably not get pulling bow models etc, check ModelGenerator for this
    private static void convertItemsPack_1_19_3(List<VirtualFile> output) {
        try {
            Set<VirtualFile> items = output.stream().filter(v -> v.getPath().startsWith("assets/minecraft/models/item") && v.getPath().endsWith(".json")).collect(Collectors.toSet());
            for (VirtualFile virtualFile : items.stream().filter(v -> {
                try {
                    Material.valueOf(v.getPath().split("/")[v.getPath().split("/").length - 1].replace(".json", "").toUpperCase());
                } catch (IllegalArgumentException e) {
                    // If someone has their model inside item folder, but it isn't
                    // a material this should return false to exclude it
                    return false;
                }
                return true;
            }).collect(Collectors.toSet())) {
                String itemModelContent = IOUtils.toString(virtualFile.getInputStream(), String.valueOf(StandardCharsets.UTF_8));
                JsonObject itemModel = JsonParser.parseString(itemModelContent).getAsJsonObject();
                JsonArray overrides = itemModel.getAsJsonArray("overrides");
                List<VirtualFile> models = new ArrayList<>();
                List<VirtualFile> textures = new ArrayList<>();

                for (int index = 0; index < overrides.size(); index++) {
                    JsonObject override = overrides.get(index).getAsJsonObject();
                    if (scanAndRepathModels(output, models, override))
                        overrides.set(index, override);
                }

                scanTexturesInModels(output, models, textures);

                virtualFile.setInputStream(new ByteArrayInputStream(itemModel.toString().getBytes(StandardCharsets.UTF_8)));
                output.set(output.indexOf(virtualFile), virtualFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void convertBlocksPack_1_19_3(List<VirtualFile> output) {
        try {
            for (String blockType : List.of("note_block", "tripwire")) {
                Optional<VirtualFile> virtualState = output.stream().filter(f -> Objects.equals(f.getPath(), "assets/minecraft/blockstates/" + blockType + ".json")).findFirst();
                if (virtualState.isEmpty()) return;
                String blockStateContent = IOUtils.toString(virtualState.get().getInputStream(), String.valueOf(StandardCharsets.UTF_8));
                JsonObject blockStates = JsonParser.parseString(blockStateContent).getAsJsonObject();
                List<VirtualFile> models = new ArrayList<>();
                List<VirtualFile> textures = new ArrayList<>();

                for (Map.Entry<String, JsonElement> entry : blockStates.getAsJsonObject("variants").entrySet()) {
                    JsonObject object = entry.getValue().getAsJsonObject();
                    if (scanAndRepathModels(output, models, object))
                        entry.setValue(object);
                }

                scanTexturesInModels(output, models, textures);

                virtualState.get().setInputStream(new ByteArrayInputStream(blockStates.toString().getBytes(StandardCharsets.UTF_8)));
                output.set(output.indexOf(virtualState.get()), virtualState.get());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean scanAndRepathModels(List<VirtualFile> output, List<VirtualFile> models, JsonObject object) {
        if (!object.has("model")) return false;
        String model = object.get("model").getAsString();
        String namespace = model.split(":").length > 1 ? model.split(":")[0] : "minecraft";
        String path = model.split(":").length > 1 ? model.split(":")[1] : model;
        Optional<VirtualFile> virtualFile = output.stream().filter(v1 ->
                v1.getPath().startsWith("assets/" + namespace + "/models/" + path + ".json") ||
                        v1.getPath().startsWith("assets/oraxen_converted/models/oraxen/" + path + ".json")).findFirst();

        if (virtualFile.isEmpty()) return false;
        if (!models.contains(virtualFile.get()))
            models.add(virtualFile.get());

        return true;
    }

    private static void scanTexturesInModels(List<VirtualFile> output, List<VirtualFile> models, List<VirtualFile> textures) {
        try {
            for (VirtualFile virtual : models) {
                JsonElement element = JsonParser.parseString(IOUtils.toString(virtual.getInputStream(), StandardCharsets.UTF_8));
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                if (!object.has("textures")) continue;
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("textures").entrySet()) {
                    String texture = entry.getValue().getAsString();
                    String namespace = texture.split(":").length > 1 ? texture.split(":")[0] : "minecraft";
                    String path = texture.split(":").length > 1 ? texture.split(":")[1] : texture;
                    output.stream().filter(v2 -> Objects.equals(v2.getPath(), "assets/" + namespace + "/textures/" + path + ".png")).findFirst().ifPresent(v2 -> {
                        if (!textures.contains(v2)) textures.add(v2);
                    });
                }

                object.get("textures").getAsJsonObject().entrySet().forEach(e -> {
                    String texture = e.getValue().getAsString();
                    String path = texture.split(":").length > 1 ? texture.split(":")[1] : texture;
                    e.setValue(new JsonPrimitive("oraxen_converted:" + (path.startsWith("oraxen/") ? path : "oraxen/" + path)));
                });

                virtual.setInputStream(new ByteArrayInputStream(object.toString().getBytes(StandardCharsets.UTF_8)));
                output.set(output.indexOf(virtual), virtual);
            }

            for (VirtualFile file : textures) {
                final String oldPath = file.getPath();
                String texturePath = file.getPath().split("assets/.*/textures/")[1];
                String finalTexturePath = texturePath;
                List<Glyph> glyphsWithTexture = OraxenPlugin.get().getFontManager().getGlyphs().stream().filter(g -> Objects.equals(g.getTexture(), finalTexturePath)).toList();

                if (!texturePath.startsWith("block/") && !texturePath.startsWith("item/")) {
                    // If an item icon is used for a glyph aswell ("required/exit_icon.png" from default ones), add a copy in the original path aswell
                    //TODO Make this not so hacky
                    if (!glyphsWithTexture.isEmpty()) {
                        final VirtualFile glyphTexture = new VirtualFile(Utils.replaceLast(Utils.getStringBeforeLastInSplit(oldPath, "/"), "/", ""), Utils.getLastStringInSplit(oldPath, "/"), file.getInputStream());
                        output.add(glyphTexture);
                    }
                    texturePath = "assets/oraxen_converted/textures/" + (!texturePath.startsWith("oraxen/") ? "oraxen/" : "") + texturePath;
                    file.setPath(texturePath);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateAtlasFile(List<VirtualFile> output) {
        JsonObject atlas = new JsonObject();
        JsonArray atlasContent = new JsonArray();
        JsonObject atlasBlock = new JsonObject();
        JsonObject atlasEntity = new JsonObject();

        atlasBlock.addProperty("type", "directory");
        atlasBlock.addProperty("source", "oraxen");
        atlasBlock.addProperty("prefix", "oraxen/");

        atlasEntity.addProperty("type", "directory");
        atlasEntity.addProperty("source", "entity");
        atlasEntity.addProperty("prefix", "entity/");

        atlasContent.add(atlasBlock);
        atlasContent.add(atlasEntity);
        atlas.add("sources", atlasContent);
        VirtualFile atlasFile = new VirtualFile("assets/minecraft/atlases", "blocks.json", new ByteArrayInputStream(atlas.toString().getBytes(StandardCharsets.UTF_8)));
        if (output.stream().noneMatch(v -> v.getPath().equals(atlasFile.getPath()))) output.add(atlasFile);
    }
}
