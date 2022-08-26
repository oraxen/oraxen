package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.items.OraxenMeta;

import java.util.List;

public class ModelGenerator {

    private final JsonObject json = new JsonObject();

    public ModelGenerator(OraxenMeta oraxenMeta) {

        json.addProperty("parent", oraxenMeta.getParentModel());

        JsonObject textures = new JsonObject();
        List<String> layers = oraxenMeta.getLayers();
        String parentModel = oraxenMeta.getParentModel();

        if (parentModel.equals("block/cube_all")) {
            textures.addProperty("all", layers.get(0));
        } else if (parentModel.contains("cross")) {
            textures.addProperty("cross", layers.get(0));
        } else if (parentModel.contains("block/orientable")) {
            textures.addProperty("front", layers.get(0));
            textures.addProperty("side", layers.get(1));
            if (!parentModel.endsWith("vertical"))
                textures.addProperty("top", layers.get(2));
        } else if (parentModel.contains("cube_column")) {
            textures.addProperty("end", layers.get(0));
            textures.addProperty("side", layers.get(1));
        }else {
            for (int i = 0; i < layers.size(); i++)
                textures.addProperty("layer" + i, layers.get(i));
        }
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
