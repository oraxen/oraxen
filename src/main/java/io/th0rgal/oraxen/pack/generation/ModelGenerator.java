package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.util.List;

public class ModelGenerator {

    private final JsonObject json = new JsonObject();

    public ModelGenerator(OraxenMeta oraxenMeta) {

        json.addProperty("parent", oraxenMeta.getParentModel());

        JsonObject textures = new JsonObject();
        List<String> layers = oraxenMeta.getLayers();

        if (oraxenMeta.getParentModel().equals("block/cube_all")) {
            textures.addProperty("all", layers.get(0));
        } else {
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
