package io.th0rgal.oraxen.utils.customarmor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class TrimArmorDatapack {

    private final JsonObject datapackMeta = new JsonObject();
    private final JsonObject sourcesObject = new JsonObject();
    public TrimArmorDatapack() {
        JsonObject data = new JsonObject();
        data.addProperty("description", "Datapack for Oraxens Custom Armor trims");
        data.addProperty("pack_format", 26);
        datapackMeta.add("pack", data);
    }

    public void generateTrimDatapack(List<VirtualFile> output) {
        List<VirtualFile> armorLayers = output.stream().filter(v -> v.getPath().endsWith("_armor_layer_1.png")).toList();
        File datapacksRoot = Bukkit.getWorldContainer().toPath().resolve("world/datapacks").toFile();
        File datapack = datapacksRoot.toPath().resolve("trims_test").toFile();
        datapack.toPath().resolve("data").toFile().mkdirs();
        writeMCMeta(datapack);
        writeTrimPattern(datapack, armorLayers);
        writeTrimAtlas(output, armorLayers);
    }

    private void writeTrimPattern(File datapack, List<VirtualFile> armorLayers) {
        for (VirtualFile armorLayer : armorLayers) {
            String armorPrefix = StringUtils.substringAfterLast(StringUtils.substringBefore(armorLayer.getPath(), "_armor_layer_1.png"), "/");
            File armorJson = datapack.toPath().resolve("data/oraxen/trim_pattern/" + armorPrefix + ".json").toFile();
            armorJson.getParentFile().mkdirs();
            JsonObject trimPattern = new JsonObject();
            JsonObject description = new JsonObject();
            description.addProperty("translate", "trim_pattern.oraxen." + armorPrefix);
            trimPattern.add("description", description);
            trimPattern.addProperty("asset_id", "oraxen:" + armorPrefix);
            trimPattern.addProperty("template_item", "minecraft:debug_stick");
            try {
                armorJson.createNewFile();
                FileUtils.writeStringToFile(armorJson, trimPattern.toString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeTrimAtlas(List<VirtualFile> output, List<VirtualFile> armorLayers) {
        Optional<VirtualFile> trimsAtlas = output.stream().filter(v -> v.getPath().equals("assets/minecraft/atlases/armor_trims.json")).findFirst();

        // If for some reason the atlas exists already, we append to it
        if (trimsAtlas.isPresent()) {
            try {
                String trimsAtlasContent = IOUtils.toString(trimsAtlas.get().getInputStream(), StandardCharsets.UTF_8);
                JsonObject atlasJson = (JsonObject) JsonParser.parseString(trimsAtlasContent);
                JsonArray sourcesArray = atlasJson.getAsJsonArray("sources");
                for (JsonElement element : sourcesArray) {
                    JsonObject sourceObject = element.getAsJsonObject();

                    // Get the textures array
                    JsonArray texturesArray = sourceObject.getAsJsonArray("textures");
                    for (VirtualFile armorLayer : armorLayers) {
                        String armorNamespace = StringUtils.substringBetween(armorLayer.getPath(), "assets/", "/textures");
                        String armorPath = StringUtils.substringAfter(armorLayer.getPath(), "textures/").replace(".png", "");
                        texturesArray.add(armorNamespace + ":" + armorPath);
                    }
                    sourceObject.remove("textures");
                    sourceObject.add("textures", texturesArray);
                }

                atlasJson.remove("sources");
                atlasJson.add("sources", sourcesArray);
                trimsAtlas.get().setInputStream(IOUtils.toInputStream(atlasJson.toString(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            JsonObject atlasJson = new JsonObject();
            JsonArray sourcesArray = new JsonArray();
            JsonArray texturesArray = new JsonArray();

            for (VirtualFile armorLayer : armorLayers) {
                String armorNamespace = StringUtils.substringBetween(armorLayer.getPath(), "assets/", "/textures");
                String armorPath = StringUtils.substringAfter(armorLayer.getPath(), "textures/").replace(".png", "");
                texturesArray.add(armorNamespace + ":" + armorPath);
                texturesArray.add(armorNamespace + ":" + armorPath.replace("_layer_1", "_layer_2"));
            }

            sourcesObject.add("textures", texturesArray);
            sourcesArray.add(sourcesObject);
            atlasJson.add("sources", sourcesArray);

            output.add(new VirtualFile("assets/minecraft/atlases", "armor_trims.json", IOUtils.toInputStream(atlasJson.toString(), StandardCharsets.UTF_8)));
        }
    }

    private void writeMCMeta(File datapack) {
        try {
            File packMeta = datapack.toPath().resolve("pack.mcmeta").toFile();
            packMeta.createNewFile();
            FileUtils.writeStringToFile(packMeta, datapackMeta.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void trimSourcesObject() {
        JsonObject permutationObject = new JsonObject();
        sourcesObject.addProperty("type", "minecraft:paletted_permutations");
        sourcesObject.addProperty("palette_key", "trims/color_palettes/trim_palette");
        permutationObject.addProperty("quartz", "trims/color_palettes/quartz");
        permutationObject.addProperty("iron", "trims/color_palettes/iron");
        permutationObject.addProperty("gold", "trims/color_palettes/gold");
        permutationObject.addProperty("diamond", "trims/color_palettes/diamond");
        permutationObject.addProperty("netherite", "trims/color_palettes/netherite");
        permutationObject.addProperty("redstone", "trims/color_palettes/redstone");
        permutationObject.addProperty("copper", "trims/color_palettes/copper");
        permutationObject.addProperty("emerald", "trims/color_palettes/emerald");
        permutationObject.addProperty("lapis", "trims/color_palettes/lapis");
        permutationObject.addProperty("amethyst", "trims/color_palettes/amethyst");
        sourcesObject.add("permutations", permutationObject);
    }

    private void getPermutationObject() {

    }

}
