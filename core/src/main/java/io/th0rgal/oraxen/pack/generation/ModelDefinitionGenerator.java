package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.Utils;

import com.google.gson.JsonObject;
import org.bukkit.Material;

import java.util.List;

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
        
        // Create base model object
        JsonObject baseModel = new JsonObject();
        baseModel.addProperty("type", "minecraft:model");
        baseModel.addProperty("model", oraxenMeta.getModelName());

        //maybe need consider that an itemmodel has multiple tint source
        if (material.toString().startsWith("LEATHER_")){
            JsonArray tints = new JsonArray();
            JsonObject dye= new JsonObject();
            dye.addProperty("type","minecraft:dye");
            //default color is white
            dye.addProperty("default",16578808);
            tints.add(dye);
            baseModel.add("tints",tints);
        }
        if (Material.POTION==material||Material.SPLASH_POTION==material||Material.LINGERING_POTION==material){
            JsonArray tints = new JsonArray();
            JsonObject dye= new JsonObject();
            dye.addProperty("type","minecraft:potion");
            //default color is white
            dye.addProperty("default",16578808);
            tints.add(dye);
            baseModel.add("tints",tints);
        }

        // If we have pulling models, wrap the base model in a condition structure
        if (oraxenMeta.hasPullingModels() && material == Material.BOW) {
            JsonObject conditionModel = new JsonObject();
            conditionModel.addProperty("type", "minecraft:condition");
            conditionModel.addProperty("property", "using_item");
            
            // on_false: base model (when not pulling)
            conditionModel.add("on_false", baseModel);
            
            // on_true: range_dispatch for pulling states
            JsonObject rangeDispatch = new JsonObject();
            rangeDispatch.addProperty("type", "minecraft:range_dispatch");
            rangeDispatch.addProperty("property", "use_duration");
            rangeDispatch.addProperty("scale", 0.05);
            
            List<String> pullingModels = oraxenMeta.getPullingModels();
            JsonArray entries = new JsonArray();
            
            // Add entries for pulling_1 and pulling_2 with thresholds
            if (pullingModels.size() >= 2) {
                JsonObject entry1 = new JsonObject();
                entry1.addProperty("threshold", 0.65f);
                JsonObject model1 = new JsonObject();
                model1.addProperty("type", "minecraft:model");
                model1.addProperty("model", pullingModels.get(1));
                entry1.add("model", model1);
                entries.add(entry1);
            }
            
            if (pullingModels.size() >= 3) {
                JsonObject entry2 = new JsonObject();
                entry2.addProperty("threshold", 0.9f);
                JsonObject model2 = new JsonObject();
                model2.addProperty("type", "minecraft:model");
                model2.addProperty("model", pullingModels.get(2));
                entry2.add("model", model2);
                entries.add(entry2);
            }
            
            rangeDispatch.add("entries", entries);
            
            // fallback: pulling_0 (first pulling state)
            if (!pullingModels.isEmpty()) {
                JsonObject fallback = new JsonObject();
                fallback.addProperty("type", "minecraft:model");
                fallback.addProperty("model", pullingModels.get(0));
                rangeDispatch.add("fallback", fallback);
            }
            
            conditionModel.add("on_true", rangeDispatch);
            root.add("model", conditionModel);
        } else {
            // No pulling models, use base model directly
            root.add("model", baseModel);
        }

        // Add item model definition properties (1.21.4+)
        if (oraxenMeta.isOversizedInGui()) {
            root.addProperty("oversized_in_gui", true);
        }
        if (!oraxenMeta.isHandAnimationOnSwap()) {
            root.addProperty("hand_animation_on_swap", false);
        }
        if (oraxenMeta.getSwapAnimationScale() != 1.0f) {
            root.addProperty("swap_animation_scale", oraxenMeta.getSwapAnimationScale());
        }

        return root;
    }
}
