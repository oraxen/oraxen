package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import io.th0rgal.oraxen.items.OraxenMeta;

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
        if (material != null && material.toString().startsWith("LEATHER_")){
            JsonArray tints = new JsonArray();
            JsonObject dye= new JsonObject();
            dye.addProperty("type","minecraft:dye");
            //default color is white
            dye.addProperty("default",16578808);
            tints.add(dye);
            baseModel.add("tints",tints);
        }
        if (material != null && (Material.POTION==material||Material.SPLASH_POTION==material||Material.LINGERING_POTION==material)){
            JsonArray tints = new JsonArray();
            JsonObject dye= new JsonObject();
            dye.addProperty("type","minecraft:potion");
            //default color is white
            dye.addProperty("default",16578808);
            tints.add(dye);
            baseModel.add("tints",tints);
        }

        // Handle bows with pulling models
        if (oraxenMeta.hasPullingModels() && material != null && material == Material.BOW) {
            JsonObject conditionModel = createBowPullingModel(baseModel, oraxenMeta);
            root.add("model", conditionModel);
        } 
        // Handle crossbows with pulling models, charged model, and firework model
        else if (material != null && material == Material.CROSSBOW) {
            JsonObject crossbowModel = createCrossbowModel(baseModel, oraxenMeta);
            root.add("model", crossbowModel);
        } 
        else {
            // No special handling, use base model directly
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

    private JsonObject createBowPullingModel(JsonObject baseModel, OraxenMeta oraxenMeta) {
        JsonObject conditionModel = new JsonObject();
        conditionModel.addProperty("type", "minecraft:condition");
        conditionModel.addProperty("property", "minecraft:using_item");
        
        // on_false: base model (when not pulling)
        conditionModel.add("on_false", baseModel);
        
        // on_true: range_dispatch for pulling states
        JsonObject rangeDispatch = new JsonObject();
        rangeDispatch.addProperty("type", "minecraft:range_dispatch");
        rangeDispatch.addProperty("property", "minecraft:use_duration");
        rangeDispatch.addProperty("scale", 0.05);
        
        List<String> pullingModels = oraxenMeta.getPullingModels();
        if (pullingModels == null || pullingModels.isEmpty()) {
            // Fallback to base model if pullingModels is null/empty (shouldn't happen due to hasPullingModels check)
            return baseModel;
        }
        
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
        JsonObject fallback = new JsonObject();
        fallback.addProperty("type", "minecraft:model");
        fallback.addProperty("model", pullingModels.get(0));
        rangeDispatch.add("fallback", fallback);
        
        conditionModel.add("on_true", rangeDispatch);
        return conditionModel;
    }

    private JsonObject createCrossbowModel(JsonObject baseModel, OraxenMeta oraxenMeta) {
        // Structure based on craft-engine implementation:
        // Outer: condition on "using_item" (pulling)
        //   - on_false: select on "charge_type" (arrow/rocket) or base model (when not pulling)
        //   - on_true: range_dispatch on "crossbow/pull" (when pulling)
        
        // Build the on_false model (charged/firework or base)
        JsonObject onFalseModel = baseModel;
        if (oraxenMeta.hasChargedModel() || oraxenMeta.hasFireworkModel()) {
            JsonObject selectModel = new JsonObject();
            selectModel.addProperty("type", "minecraft:select");
            selectModel.addProperty("property", "minecraft:charge_type");
            
            JsonArray cases = new JsonArray();
            
            // Arrow case (charged with arrow)
            if (oraxenMeta.hasChargedModel()) {
                JsonObject arrowCase = new JsonObject();
                arrowCase.addProperty("when", "arrow");
                JsonObject arrowModelObj = new JsonObject();
                arrowModelObj.addProperty("type", "minecraft:model");
                arrowModelObj.addProperty("model", oraxenMeta.getChargedModel());
                arrowCase.add("model", arrowModelObj);
                cases.add(arrowCase);
            }
            
            // Rocket case (charged with firework)
            if (oraxenMeta.hasFireworkModel()) {
                JsonObject rocketCase = new JsonObject();
                rocketCase.addProperty("when", "rocket");
                JsonObject fireworkModelObj = new JsonObject();
                fireworkModelObj.addProperty("type", "minecraft:model");
                fireworkModelObj.addProperty("model", oraxenMeta.getFireworkModel());
                rocketCase.add("model", fireworkModelObj);
                cases.add(rocketCase);
            }
            
            selectModel.add("cases", cases);
            selectModel.add("fallback", baseModel);
            onFalseModel = selectModel;
        }
        
        // If no pulling models, return the on_false model directly
        if (!oraxenMeta.hasPullingModels()) {
            return onFalseModel;
        }
        
        List<String> pullingModels = oraxenMeta.getPullingModels();
        if (pullingModels == null || pullingModels.isEmpty()) {
            return onFalseModel;
        }
        
        // Build the outer condition on "using_item"
        JsonObject pullingCondition = new JsonObject();
        pullingCondition.addProperty("type", "minecraft:condition");
        pullingCondition.addProperty("property", "minecraft:using_item");
        
        // on_false: select on charge_type or base model (when not pulling)
        pullingCondition.add("on_false", onFalseModel);
        
        // on_true: range_dispatch for pulling states using crossbow/pull property
        JsonObject rangeDispatch = new JsonObject();
        rangeDispatch.addProperty("type", "minecraft:range_dispatch");
        rangeDispatch.addProperty("property", "minecraft:crossbow/pull");
        // No scale needed for crossbow/pull property
        
        JsonArray entries = new JsonArray();
        
        // Add entries for pulling_1 and pulling_2 with thresholds (matching craft-engine: 0.58 and 1.0)
        if (pullingModels.size() >= 2) {
            JsonObject entry1 = new JsonObject();
            entry1.addProperty("threshold", 0.58f);
            JsonObject model1 = new JsonObject();
            model1.addProperty("type", "minecraft:model");
            model1.addProperty("model", pullingModels.get(1));
            entry1.add("model", model1);
            entries.add(entry1);
        }
        
        if (pullingModels.size() >= 3) {
            JsonObject entry2 = new JsonObject();
            entry2.addProperty("threshold", 1.0f);
            JsonObject model2 = new JsonObject();
            model2.addProperty("type", "minecraft:model");
            model2.addProperty("model", pullingModels.get(2));
            entry2.add("model", model2);
            entries.add(entry2);
        }
        
        rangeDispatch.add("entries", entries);
        
        // fallback: pulling_0 (first pulling state)
        JsonObject fallback = new JsonObject();
        fallback.addProperty("type", "minecraft:model");
        fallback.addProperty("model", pullingModels.get(0));
        rangeDispatch.add("fallback", fallback);
        
        pullingCondition.add("on_true", rangeDispatch);
        return pullingCondition;
    }

}
