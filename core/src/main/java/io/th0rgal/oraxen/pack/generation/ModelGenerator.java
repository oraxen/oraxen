package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ModelGenerator {

    private final JsonObject json = new JsonObject();

    public ModelGenerator(OraxenMeta oraxenMeta) {
        JsonObject textures = new JsonObject();
        String parentModel = oraxenMeta.getParentModel();
        List<String> layers = new ArrayList<>(oraxenMeta.getLayers());
        String defaultTexture = Utils.getOrDefault(layers, 0, "");

        json.addProperty("parent", parentModel);
        if (oraxenMeta.hasLayersMap()) { //Check if oraxen meta uses new texture system
            oraxenMeta.getLayersMap().forEach(textures::addProperty);
        } else if (oraxenMeta.hasLayers()) { //Use old texture system

            if (parentModel.equals("block/cube") || parentModel.equals("block/cube_directional") || parentModel.equals("block/cube_mirrored")) {
                textures.addProperty("particle", Utils.getOrDefault(layers, 2, defaultTexture));
                textures.addProperty("down", defaultTexture);
                textures.addProperty("up", Utils.getOrDefault(layers, 1, defaultTexture));
                textures.addProperty("north", Utils.getOrDefault(layers, 2, defaultTexture));
                textures.addProperty("south", Utils.getOrDefault(layers, 3, defaultTexture));
                textures.addProperty("west", Utils.getOrDefault(layers, 4, defaultTexture));
                textures.addProperty("east", Utils.getOrDefault(layers, 5, defaultTexture));
            } else if (parentModel.equals("block/cube_all") || parentModel.equals("block/cube_mirrored_all")) {
                textures.addProperty("all", defaultTexture);
            } else if (parentModel.equals("block/cross")) {
                textures.addProperty("cross", defaultTexture);
            } else if (parentModel.startsWith("block/orientable")) {
                textures.addProperty("front", defaultTexture);
                textures.addProperty("side", Utils.getOrDefault(layers, 1, defaultTexture));
                if (!parentModel.endsWith("vertical"))
                    textures.addProperty("top", Utils.getOrDefault(layers, 2, defaultTexture));
                if (parentModel.endsWith("with_bottom"))
                    textures.addProperty("bottom", Utils.getOrDefault(layers, 3, defaultTexture));
            } else if (parentModel.startsWith("block/cube_column")) {
                textures.addProperty("end", defaultTexture);
                textures.addProperty("side", Utils.getOrDefault(layers, 1, defaultTexture));
            } else if (parentModel.equals("block/cube_bottom_top")) {
                textures.addProperty("top", defaultTexture);
                textures.addProperty("side", Utils.getOrDefault(layers, 1, defaultTexture));
                textures.addProperty("bottom", Utils.getOrDefault(layers, 2, defaultTexture));
            } else if (parentModel.equals("block/cube_top")) {
                textures.addProperty("top", defaultTexture);
                textures.addProperty("side", Utils.getOrDefault(layers, 1, defaultTexture));
            } else {
                for (int i = 0; i < layers.size(); i++)
                    textures.addProperty("layer" + i, layers.get(i));
            }
        }

        if (oraxenMeta.hasPullingTextures()) PredicatesGenerator.generatePullingModels(oraxenMeta);
        if (oraxenMeta.hasBlockingTexture()) PredicatesGenerator.generateBlockingModels(oraxenMeta);
        if (oraxenMeta.hasChargedTexture()) PredicatesGenerator.generateChargedModels(oraxenMeta);
        if (oraxenMeta.hasCastTexture()) PredicatesGenerator.generateCastModels(oraxenMeta);
        if (oraxenMeta.hasFireworkModel()) PredicatesGenerator.generateFireworkModels(oraxenMeta);
        if (oraxenMeta.hasDamagedModels()) PredicatesGenerator.generateDamageModels(oraxenMeta);

        json.add("textures", textures);

    }

    public JsonObject getJson() {
        return this.json;
    }

    @Override
    public String toString() {
        return this.json.toString();
    }

}
