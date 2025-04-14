package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import io.th0rgal.oraxen.items.OraxenMeta;

import org.bukkit.Bukkit;

import com.google.gson.JsonObject;
import org.bukkit.Material;

public class ModelDefinitionGenerator {
    private final OraxenMeta oraxenMeta;
    private  Material material;
    public ModelDefinitionGenerator(OraxenMeta oraxenMeta) {
        this.oraxenMeta = oraxenMeta;
    }
    public ModelDefinitionGenerator(OraxenMeta oraxenMeta, Material material) {
        this.oraxenMeta = oraxenMeta;
        this.material=material;
    }
    public JsonObject toJSON() {
        JsonObject root = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", oraxenMeta.getModelName());

        //maybe need consider that an itemmodel has multiple tint source
        if (material.toString().startsWith("LEATHER_")){
            JsonArray tints = new JsonArray();
            JsonObject dye= new JsonObject();
            dye.addProperty("type","minecraft:dye");
            //default color is white
            dye.addProperty("default",16578808);
            tints.add(dye);
            model.add("tints",tints);
        }
        if (Material.POTION==material||Material.SPLASH_POTION==material||Material.LINGERING_POTION==material){
            JsonArray tints = new JsonArray();
            JsonObject dye= new JsonObject();
            dye.addProperty("type","minecraft:potion");
            //default color is white
            dye.addProperty("default",16578808);
            tints.add(dye);
            model.add("tints",tints);
        }

        root.add("model", model);
        return root;
    }
}
