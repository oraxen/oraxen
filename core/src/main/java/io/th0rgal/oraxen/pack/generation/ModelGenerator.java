package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.items.OraxenMeta;

import java.util.List;

public class ModelGenerator {

    private final JsonObject json = new JsonObject();

    public ModelGenerator(OraxenMeta oraxenMeta) {
        JsonObject textures = new JsonObject();
        String parentModel = oraxenMeta.getParentModel();

        json.addProperty("parent", parentModel);
        if (oraxenMeta.hasLayersMap()) { //Check if oraxen meta uses new texture system
            oraxenMeta.getLayersMap().forEach(textures::addProperty);
        } else if (oraxenMeta.hasLayers()) { //Use old texture system
            List<String> layers = oraxenMeta.getLayers();
            if (parentModel.equals("block/cube") || parentModel.equals("block/cube_directional") || parentModel.equals("block/cube_mirrored")) {
                textures.addProperty("particle", layers.get(2));
                textures.addProperty("down", layers.get(0));
                textures.addProperty("up", layers.get(1));
                textures.addProperty("north", layers.get(2));
                textures.addProperty("south", layers.get(3));
                textures.addProperty("west", layers.get(4));
                textures.addProperty("east", layers.get(5));
            } else if (parentModel.equals("block/cube_all") || parentModel.equals("block/cube_mirrored_all")) {
                textures.addProperty("all", layers.get(0));
            } else if (parentModel.equals("block/cross")) {
                textures.addProperty("cross", layers.get(0));
            } else if (parentModel.startsWith("block/orientable")) {
                textures.addProperty("front", layers.get(0));
                textures.addProperty("side", layers.get(1));
                if (!parentModel.endsWith("vertical"))
                    textures.addProperty("top", layers.get(2));
                if (parentModel.endsWith("with_bottom"))
                    textures.addProperty("bottom", layers.get(3));
            } else if (parentModel.startsWith("block/cube_column")) {
                textures.addProperty("end", layers.get(0));
                textures.addProperty("side", layers.get(1));
            } else if (parentModel.equals("block/cube_bottom_top")) {
                textures.addProperty("top", layers.get(0));
                textures.addProperty("side", layers.get(1));
                textures.addProperty("bottom", layers.get(2));
            } else if (parentModel.equals("block/cube_top")) {
                textures.addProperty("top", layers.get(0));
                textures.addProperty("side", layers.get(1));
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
