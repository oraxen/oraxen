package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.items.OraxenMeta;

import org.bukkit.Bukkit;

import com.google.gson.JsonObject;

public class ModelDefinitionGenerator {
    private final OraxenMeta oraxenMeta;

    public ModelDefinitionGenerator(OraxenMeta oraxenMeta) {
        this.oraxenMeta = oraxenMeta;
    }

    public JsonObject toJSON() {
        JsonObject root = new JsonObject();
        JsonObject model = new JsonObject();

        model.addProperty("type", "minecraft:model");
        model.addProperty("model", oraxenMeta.getModelName());

        root.add("model", model);
        return root;
    }
}
