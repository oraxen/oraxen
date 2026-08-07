package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import io.th0rgal.oraxen.items.OraxenMeta;

import com.google.gson.JsonObject;
import org.bukkit.Material;

import java.util.List;

public class ModelDefinitionGenerator {
    // Threshold constants for bow pulling states
    private static final float BOW_PULL_THRESHOLD_1 = 0.65f;
    private static final float BOW_PULL_THRESHOLD_2 = 0.9f;
    private static final double BOW_USE_DURATION_SCALE = 0.05;
    
    // Threshold constants for crossbow pulling states
    private static final float CROSSBOW_PULL_THRESHOLD_1 = 0.58f;
    private static final float CROSSBOW_PULL_THRESHOLD_2 = 1.0f;
    
    // Default tint color (white)
    private static final int DEFAULT_TINT_COLOR = 16578808;
    
    private final OraxenMeta oraxenMeta;
    private final Material material;
    
    public ModelDefinitionGenerator(OraxenMeta oraxenMeta) {
        this.oraxenMeta = oraxenMeta;
        this.material = null;
    }
    
    public ModelDefinitionGenerator(OraxenMeta oraxenMeta, Material material) {
        this.oraxenMeta = oraxenMeta;
        this.material = material;
    }
    public JsonObject toJSON() {
        JsonObject root = new JsonObject();
        
        JsonObject baseModel = createBaseModel();
        JsonObject itemModel = createItemSpecificModel(baseModel);
        
        // If gui_model is specified, wrap in display_context selector
        if (oraxenMeta.hasGuiModel()) {
            itemModel = createDisplayContextModel(itemModel);
        }
        
        root.add("model", itemModel);
        
        addItemModelProperties(root);
        return root;
    }
    
    private JsonObject createBaseModel() {
        JsonObject baseModel = new JsonObject();
        baseModel.addProperty("type", "minecraft:model");
        baseModel.addProperty("model", oraxenMeta.getModelName());
        
        addTintsIfNeeded(baseModel);
        return baseModel;
    }
    
    private void addTintsIfNeeded(JsonObject baseModel) {
        if (material == null) {
            return;
        }
        
        if (material.name().startsWith("LEATHER_")) {
            addDyeTint(baseModel);
        } else if (isPotionMaterial(material)) {
            addPotionTint(baseModel);
        }
    }
    
    private boolean isPotionMaterial(Material material) {
        return material == Material.POTION 
            || material == Material.SPLASH_POTION 
            || material == Material.LINGERING_POTION;
    }
    
    private void addDyeTint(JsonObject baseModel) {
        JsonArray tints = new JsonArray();
        JsonObject dye = new JsonObject();
        dye.addProperty("type", "minecraft:dye");
        dye.addProperty("default", DEFAULT_TINT_COLOR);
        tints.add(dye);
        baseModel.add("tints", tints);
    }
    
    private void addPotionTint(JsonObject baseModel) {
        JsonArray tints = new JsonArray();
        JsonObject dye = new JsonObject();
        dye.addProperty("type", "minecraft:potion");
        dye.addProperty("default", DEFAULT_TINT_COLOR);
        tints.add(dye);
        baseModel.add("tints", tints);
    }
    
    private JsonObject createItemSpecificModel(JsonObject baseModel) {
        if (material == null) {
            return baseModel;
        }
        
        if (material == Material.BOW && oraxenMeta.hasPullingModels()) {
            return createBowPullingModel(baseModel, oraxenMeta);
        }
        
        if (material == Material.CROSSBOW && hasCrossbowSpecialModels()) {
            return createCrossbowModel(baseModel, oraxenMeta);
        }
        
        if (material == Material.FISHING_ROD && oraxenMeta.hasCastModel()) {
            return createFishingRodModel(baseModel, oraxenMeta);
        }
        
        if (material == Material.SHIELD && oraxenMeta.hasBlockingModel()) {
            return createShieldModel(baseModel, oraxenMeta);
        }
        
        return baseModel;
    }
    
    private boolean hasCrossbowSpecialModels() {
        return oraxenMeta.hasPullingModels() 
            || oraxenMeta.hasChargedModel() 
            || oraxenMeta.hasFireworkModel();
    }
    
    private void addItemModelProperties(JsonObject root) {
        if (oraxenMeta.isOversizedInGui()) {
            root.addProperty("oversized_in_gui", true);
        }
        if (!oraxenMeta.isHandAnimationOnSwap()) {
            root.addProperty("hand_animation_on_swap", false);
        }
        if (oraxenMeta.getSwapAnimationScale() != 1.0f) {
            root.addProperty("swap_animation_scale", oraxenMeta.getSwapAnimationScale());
        }
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
        rangeDispatch.addProperty("scale", BOW_USE_DURATION_SCALE);
        
        List<String> pullingModels = oraxenMeta.getPullingModels();
        if (pullingModels == null || pullingModels.isEmpty()) {
            // Fallback to base model if pullingModels is null/empty (shouldn't happen due to hasPullingModels check)
            return baseModel;
        }
        
        JsonArray entries = new JsonArray();
        
        // Add entries for pulling_1 and pulling_2 with thresholds
        if (pullingModels.size() >= 2) {
            JsonObject entry1 = new JsonObject();
            entry1.addProperty("threshold", BOW_PULL_THRESHOLD_1);
            entry1.add("model", createModelObject(pullingModels.get(1)));
            entries.add(entry1);
        }
        
        if (pullingModels.size() >= 3) {
            JsonObject entry2 = new JsonObject();
            entry2.addProperty("threshold", BOW_PULL_THRESHOLD_2);
            entry2.add("model", createModelObject(pullingModels.get(2)));
            entries.add(entry2);
        }
        
        rangeDispatch.add("entries", entries);
        
        // fallback: pulling_0 (first pulling state)
        rangeDispatch.add("fallback", createModelObject(pullingModels.get(0)));
        
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
                arrowCase.add("model", createModelObject(oraxenMeta.getChargedModel()));
                cases.add(arrowCase);
            }
            
            // Rocket case (charged with firework)
            if (oraxenMeta.hasFireworkModel()) {
                JsonObject rocketCase = new JsonObject();
                rocketCase.addProperty("when", "rocket");
                rocketCase.add("model", createModelObject(oraxenMeta.getFireworkModel()));
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
            entry1.addProperty("threshold", CROSSBOW_PULL_THRESHOLD_1);
            entry1.add("model", createModelObject(pullingModels.get(1)));
            entries.add(entry1);
        }
        
        if (pullingModels.size() >= 3) {
            JsonObject entry2 = new JsonObject();
            entry2.addProperty("threshold", CROSSBOW_PULL_THRESHOLD_2);
            entry2.add("model", createModelObject(pullingModels.get(2)));
            entries.add(entry2);
        }
        
        rangeDispatch.add("entries", entries);
        
        // fallback: pulling_0 (first pulling state)
        rangeDispatch.add("fallback", createModelObject(pullingModels.get(0)));
        
        pullingCondition.add("on_true", rangeDispatch);
        return pullingCondition;
    }

    private JsonObject createFishingRodModel(JsonObject baseModel, OraxenMeta oraxenMeta) {
        // Structure based on craft-engine implementation:
        // Condition on "fishing_rod/cast" property
        //   - on_false: Base model (when not casting)
        //   - on_true: Cast model (when casting)
        
        if (!oraxenMeta.hasCastModel()) {
            return baseModel;
        }
        
        JsonObject conditionModel = new JsonObject();
        conditionModel.addProperty("type", "minecraft:condition");
        conditionModel.addProperty("property", "minecraft:fishing_rod/cast");
        
        // on_false: base model (when not casting)
        conditionModel.add("on_false", baseModel);
        
        // on_true: cast model (when casting)
        conditionModel.add("on_true", createModelObject(oraxenMeta.getCastModel()));
        
        return conditionModel;
    }

    private JsonObject createShieldModel(JsonObject baseModel, OraxenMeta oraxenMeta) {
        // Structure based on craft-engine implementation:
        // Condition on "using_item" property
        //   - on_false: Base model (when not blocking)
        //   - on_true: Blocking model (when blocking)
        
        if (!oraxenMeta.hasBlockingModel()) {
            return baseModel;
        }
        
        JsonObject conditionModel = new JsonObject();
        conditionModel.addProperty("type", "minecraft:condition");
        conditionModel.addProperty("property", "minecraft:using_item");
        
        // on_false: base model (when not blocking)
        conditionModel.add("on_false", baseModel);
        
        // on_true: blocking model (when blocking)
        conditionModel.add("on_true", createModelObject(oraxenMeta.getBlockingModel()));
        
        return conditionModel;
    }
    
    /**
     * Creates a display_context selector model that shows different models in GUI vs other contexts.
     * This wraps the main item model (which could be a bow, crossbow, etc.) with a GUI-specific icon.
     *
     * @param fallbackModel The model to use in all contexts except GUI (equipped, in-hand, etc.)
     * @return A JsonObject representing a minecraft:select type with display_context property
     */
    private JsonObject createDisplayContextModel(JsonObject fallbackModel) {
        JsonObject selectModel = new JsonObject();
        selectModel.addProperty("type", "minecraft:select");
        selectModel.addProperty("property", "minecraft:display_context");
        
        // Create the GUI case
        JsonArray cases = new JsonArray();
        JsonObject guiCase = new JsonObject();
        guiCase.addProperty("when", "gui");
        guiCase.add("model", createModelObject(oraxenMeta.getGuiModel()));
        cases.add(guiCase);
        
        selectModel.add("cases", cases);
        selectModel.add("fallback", fallbackModel);
        
        return selectModel;
    }
    
    /**
     * Creates a model JsonObject with the given model name.
     *
     * @param modelName The name of the model
     * @return A JsonObject representing a minecraft:model type
     */
    private JsonObject createModelObject(String modelName) {
        JsonObject modelObj = new JsonObject();
        modelObj.addProperty("type", "minecraft:model");
        modelObj.addProperty("model", modelName);
        return modelObj;
    }

}
