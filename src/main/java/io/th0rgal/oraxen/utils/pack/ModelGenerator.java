package io.th0rgal.oraxen.utils.pack;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.items.PackInfos;

import java.util.List;

public class ModelGenerator {

    private JsonObject json = new JsonObject();

    public ModelGenerator(PackInfos packInfos) {

        json.addProperty("parent", packInfos.getParentModel());

        JsonObject textures = new JsonObject();
        List<String> layers = packInfos.getLayers();
        for (int i = 0; i < layers.size(); i++)
            textures.addProperty("layer" + i, layers.get(i));

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
