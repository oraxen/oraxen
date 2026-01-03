package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ModelGenerator {

    private final JsonObject json = new JsonObject();

    public ModelGenerator(OraxenMeta oraxenMeta) {
        String parentModel = oraxenMeta.getParentModel();
        json.addProperty("parent", parentModel);

        JsonObject textures = buildTextures(oraxenMeta, parentModel);
        json.add("textures", textures);

        generateSpecialModels(oraxenMeta);
    }

    private JsonObject buildTextures(OraxenMeta oraxenMeta, String parentModel) {
        JsonObject textures = new JsonObject();

        if (oraxenMeta.hasLayersMap()) {
            oraxenMeta.getLayersMap().forEach(textures::addProperty);
        } else if (oraxenMeta.hasLayers()) {
            List<String> layers = new ArrayList<>(oraxenMeta.getLayers());
            String defaultTexture = Utils.getOrDefault(layers, 0, "");
            configureTexturesForModel(textures, parentModel, layers, defaultTexture);
        }

        return textures;
    }

    private void configureTexturesForModel(JsonObject textures, String parentModel, List<String> layers, String defaultTexture) {
        if (isCubeModel(parentModel)) {
            configureCubeTextures(textures, layers, defaultTexture);
        } else if (isCubeAllModel(parentModel)) {
            textures.addProperty("all", defaultTexture);
        } else if (parentModel.equals("block/cross")) {
            textures.addProperty("cross", defaultTexture);
        } else if (parentModel.startsWith("block/orientable")) {
            configureOrientableTextures(textures, parentModel, layers, defaultTexture);
        } else if (parentModel.startsWith("block/cube_column")) {
            configureColumnTextures(textures, layers, defaultTexture);
        } else if (parentModel.equals("block/cube_bottom_top")) {
            configureBottomTopTextures(textures, layers, defaultTexture);
        } else if (parentModel.equals("block/cube_top")) {
            configureCubeTopTextures(textures, layers, defaultTexture);
        } else if (isStairsOrSlabModel(parentModel)) {
            configureStairsSlabTextures(textures, layers, defaultTexture);
        } else if (isDoorModel(parentModel)) {
            configureDoorTextures(textures, layers, defaultTexture);
        } else if (isTrapDoorModel(parentModel)) {
            textures.addProperty("texture", defaultTexture);
        } else {
            configureLayerTextures(textures, layers);
        }
    }

    private boolean isCubeModel(String parentModel) {
        return parentModel.equals("block/cube") || parentModel.equals("block/cube_directional") || parentModel.equals("block/cube_mirrored");
    }

    private boolean isCubeAllModel(String parentModel) {
        return parentModel.equals("block/cube_all") || parentModel.equals("block/cube_mirrored_all");
    }

    private boolean isStairsOrSlabModel(String parentModel) {
        return parentModel.equals("block/stairs") || parentModel.equals("block/inner_stairs")
            || parentModel.equals("block/outer_stairs") || parentModel.equals("block/slab")
            || parentModel.equals("block/slab_top");
    }

    private boolean isDoorModel(String parentModel) {
        return parentModel.startsWith("block/door") || parentModel.startsWith("block/template_door");
    }

    private boolean isTrapDoorModel(String parentModel) {
        return parentModel.startsWith("block/template_trapdoor") || parentModel.startsWith("block/trapdoor");
    }

    private void configureCubeTextures(JsonObject textures, List<String> layers, String defaultTexture) {
        textures.addProperty("particle", Utils.getOrDefault(layers, 2, defaultTexture));
        textures.addProperty("down", defaultTexture);
        textures.addProperty("up", Utils.getOrDefault(layers, 1, defaultTexture));
        textures.addProperty("north", Utils.getOrDefault(layers, 2, defaultTexture));
        textures.addProperty("south", Utils.getOrDefault(layers, 3, defaultTexture));
        textures.addProperty("west", Utils.getOrDefault(layers, 4, defaultTexture));
        textures.addProperty("east", Utils.getOrDefault(layers, 5, defaultTexture));
    }

    private void configureOrientableTextures(JsonObject textures, String parentModel, List<String> layers, String defaultTexture) {
        textures.addProperty("front", defaultTexture);
        textures.addProperty("side", Utils.getOrDefault(layers, 1, defaultTexture));
        if (!parentModel.endsWith("vertical")) {
            textures.addProperty("top", Utils.getOrDefault(layers, 2, defaultTexture));
        }
        if (parentModel.endsWith("with_bottom")) {
            textures.addProperty("bottom", Utils.getOrDefault(layers, 3, defaultTexture));
        }
    }

    private void configureColumnTextures(JsonObject textures, List<String> layers, String defaultTexture) {
        textures.addProperty("end", defaultTexture);
        textures.addProperty("side", Utils.getOrDefault(layers, 1, defaultTexture));
    }

    private void configureBottomTopTextures(JsonObject textures, List<String> layers, String defaultTexture) {
        textures.addProperty("top", defaultTexture);
        textures.addProperty("side", Utils.getOrDefault(layers, 1, defaultTexture));
        textures.addProperty("bottom", Utils.getOrDefault(layers, 2, defaultTexture));
    }

    private void configureCubeTopTextures(JsonObject textures, List<String> layers, String defaultTexture) {
        textures.addProperty("top", defaultTexture);
        textures.addProperty("side", Utils.getOrDefault(layers, 1, defaultTexture));
    }

    private void configureStairsSlabTextures(JsonObject textures, List<String> layers, String defaultTexture) {
        textures.addProperty("bottom", defaultTexture);
        textures.addProperty("top", Utils.getOrDefault(layers, 1, defaultTexture));
        textures.addProperty("side", Utils.getOrDefault(layers, 2, defaultTexture));
    }

    private void configureDoorTextures(JsonObject textures, List<String> layers, String defaultTexture) {
        textures.addProperty("bottom", defaultTexture);
        textures.addProperty("top", Utils.getOrDefault(layers, 1, defaultTexture));
    }

    private void configureLayerTextures(JsonObject textures, List<String> layers) {
        for (int i = 0; i < layers.size(); i++) {
            textures.addProperty("layer" + i, layers.get(i));
        }
    }

    private void generateSpecialModels(OraxenMeta oraxenMeta) {
        if (oraxenMeta.hasPullingTextures()) PredicatesGenerator.generatePullingModels(oraxenMeta);
        if (oraxenMeta.hasBlockingTexture()) PredicatesGenerator.generateBlockingModels(oraxenMeta);
        if (oraxenMeta.hasChargedTexture()) PredicatesGenerator.generateChargedModels(oraxenMeta);
        if (oraxenMeta.hasCastTexture()) PredicatesGenerator.generateCastModels(oraxenMeta);
        if (oraxenMeta.hasFireworkModel()) PredicatesGenerator.generateFireworkModels(oraxenMeta);
        if (oraxenMeta.hasDamagedModels()) PredicatesGenerator.generateDamageModels(oraxenMeta);
    }

    public JsonObject getJson() {
        return this.json;
    }

    @Override
    public String toString() {
        return this.json.toString();
    }

}
