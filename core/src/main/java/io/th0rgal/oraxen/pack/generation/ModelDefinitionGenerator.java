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
        // Priority: pulling > firework (charged + firework) > charged > base
        // Build from outside in: check pulling first, then charged/firework
        
        JsonObject currentModel = baseModel;
        
        // Handle charged/firework models first (inner layer)
        if (oraxenMeta.hasChargedModel() || oraxenMeta.hasFireworkModel()) {
            JsonObject chargedCondition = new JsonObject();
            chargedCondition.addProperty("type", "minecraft:condition");
            chargedCondition.addProperty("property", "minecraft:charged");
            
            JsonObject chargedModelObj;
            if (oraxenMeta.hasChargedModel()) {
                chargedModelObj = new JsonObject();
                chargedModelObj.addProperty("type", "minecraft:model");
                chargedModelObj.addProperty("model", oraxenMeta.getChargedModel());
            } else {
                // No charged model, use base when charged (shouldn't happen, but fallback)
                chargedModelObj = baseModel;
            }
            
            // If firework model exists, check firework property when charged
            if (oraxenMeta.hasFireworkModel()) {
                JsonObject fireworkCondition = new JsonObject();
                fireworkCondition.addProperty("type", "minecraft:condition");
                fireworkCondition.addProperty("property", "minecraft:firework");
                
                JsonObject fireworkModelObj = new JsonObject();
                fireworkModelObj.addProperty("type", "minecraft:model");
                fireworkModelObj.addProperty("model", oraxenMeta.getFireworkModel());
                
                // on_true: firework model (when charged AND firework)
                fireworkCondition.add("on_true", fireworkModelObj);
                // on_false: charged model (when charged but no firework)
                fireworkCondition.add("on_false", chargedModelObj);
                
                chargedCondition.add("on_true", fireworkCondition);
            } else {
                // No firework model, use charged model when charged
                chargedCondition.add("on_true", chargedModelObj);
            }
            
            // on_false: base model (when not charged)
            chargedCondition.add("on_false", baseModel);
            currentModel = chargedCondition;
        }
        
        // Handle pulling models (outer layer, checked first)
        if (oraxenMeta.hasPullingModels()) {
            List<String> pullingModels = oraxenMeta.getPullingModels();
            if (pullingModels == null || pullingModels.isEmpty()) {
                // Fallback to current model if pullingModels is null/empty (shouldn't happen due to hasPullingModels check)
                return currentModel;
            }
            
            JsonObject pullingCondition = new JsonObject();
            pullingCondition.addProperty("type", "minecraft:condition");
            pullingCondition.addProperty("property", "minecraft:using_item");
            
            // on_true: range_dispatch for pulling states
            JsonObject rangeDispatch = new JsonObject();
            rangeDispatch.addProperty("type", "minecraft:range_dispatch");
            rangeDispatch.addProperty("property", "minecraft:use_duration");
            rangeDispatch.addProperty("scale", 0.05);
            
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
            
            pullingCondition.add("on_true", rangeDispatch);
            // on_false: current model (charged/firework or base)
            pullingCondition.add("on_false", currentModel);
            currentModel = pullingCondition;
        }
        
        return currentModel;
    }

}
