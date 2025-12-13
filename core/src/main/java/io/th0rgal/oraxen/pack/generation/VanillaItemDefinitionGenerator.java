package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Generates vanilla item model definitions (assets/minecraft/items/*.json) for 1.21.4+.
 * <p>
 * These files use the modern item model definition format with {@code range_dispatch}
 * for CustomModelData switching, replacing the legacy predicate overrides system.
 * <p>
 * Structure: The item definition dispatches based on custom_model_data values to select
 * the appropriate model, with special handling for items that have state-based models
 * (bow pulling, crossbow charged, shield blocking, fishing rod cast).
 */
public class VanillaItemDefinitionGenerator {

    // Threshold constants for bow pulling states
    private static final float BOW_PULL_THRESHOLD_1 = 0.65f;
    private static final float BOW_PULL_THRESHOLD_2 = 0.9f;
    private static final double BOW_USE_DURATION_SCALE = 0.05;

    // Threshold constants for crossbow pulling states
    private static final float CROSSBOW_PULL_THRESHOLD_1 = 0.58f;
    private static final float CROSSBOW_PULL_THRESHOLD_2 = 1.0f;

    // Default tint color (white)
    private static final int DEFAULT_TINT_COLOR = 16578808;

    private final Material material;
    private final List<ItemBuilder> items;
    private final PredicatesGenerator predicatesHelper;

    public VanillaItemDefinitionGenerator(@NotNull Material material, @NotNull List<ItemBuilder> items) {
        this.material = material;
        this.items = new ArrayList<>(items);
        this.predicatesHelper = new PredicatesGenerator(material, items);

        // Sort items by CustomModelData for consistent ordering
        this.items.sort(Comparator.comparingInt(item -> {
            OraxenMeta meta = item.getOraxenMeta();
            return meta != null && meta.getCustomModelData() != null ? meta.getCustomModelData() : Integer.MAX_VALUE;
        }));
    }

    /**
     * Returns the file path for this item definition (e.g., "map" for Material.MAP).
     */
    public String getFileName() {
        return material.name().toLowerCase(Locale.ROOT) + ".json";
    }

    /**
     * Generates the JSON for the vanilla item model definition.
     */
    public JsonObject toJSON() {
        JsonObject root = new JsonObject();

        // Build the base vanilla model reference
        JsonObject vanillaModel = createVanillaModelReference();

        // Build the CMD range_dispatch with all custom items
        JsonObject itemModel = createCmdRangeDispatch(vanillaModel);

        root.add("model", itemModel);
        return root;
    }

    /**
     * Creates a reference to the vanilla model for this material.
     * Handles special cases like bow pulling, crossbow states, etc.
     */
    private JsonObject createVanillaModelReference() {
        String vanillaModelPath = "minecraft:" + predicatesHelper.getVanillaModelName(material);
        JsonObject baseModel = createModelObject(vanillaModelPath);

        // Add tints for dyeable/potion items
        addTintsIfNeeded(baseModel);

        // Wrap with special state handling for specific materials
        return wrapWithStateHandling(baseModel, vanillaModelPath);
    }

    /**
     * Wraps the base model with state-based conditions for special items.
     */
    private JsonObject wrapWithStateHandling(JsonObject baseModel, String vanillaModelPath) {
        return switch (material) {
            case BOW -> createVanillaBowModel(baseModel, vanillaModelPath);
            case CROSSBOW -> createVanillaCrossbowModel(baseModel, vanillaModelPath);
            case FISHING_ROD -> createVanillaFishingRodModel(baseModel, vanillaModelPath);
            case SHIELD -> createVanillaShieldModel(baseModel, vanillaModelPath);
            default -> baseModel;
        };
    }

    /**
     * Creates a range_dispatch model that switches based on custom_model_data.
     */
    private JsonObject createCmdRangeDispatch(JsonObject fallbackModel) {
        // Collect all items with valid CMD values
        List<CmdEntry> cmdEntries = new ArrayList<>();
        for (ItemBuilder item : items) {
            OraxenMeta meta = item.getOraxenMeta();
            if (meta == null || meta.getCustomModelData() == null) continue;
            if (!meta.hasPackInfos()) continue;

            int cmd = meta.getCustomModelData();
            JsonObject itemModel = createItemModel(meta);
            cmdEntries.add(new CmdEntry(cmd, itemModel));
        }

        // If no CMD entries, just return the vanilla model
        if (cmdEntries.isEmpty()) {
            return fallbackModel;
        }

        // Build the range_dispatch
        JsonObject rangeDispatch = new JsonObject();
        rangeDispatch.addProperty("type", "minecraft:range_dispatch");
        rangeDispatch.addProperty("property", "minecraft:custom_model_data");

        JsonArray entries = new JsonArray();
        for (CmdEntry entry : cmdEntries) {
            JsonObject entryObj = new JsonObject();
            entryObj.addProperty("threshold", entry.cmd);
            entryObj.add("model", entry.model);
            entries.add(entryObj);
        }
        rangeDispatch.add("entries", entries);
        rangeDispatch.add("fallback", fallbackModel);

        return rangeDispatch;
    }

    /**
     * Creates the model object for a custom Oraxen item, including any state handling.
     */
    private JsonObject createItemModel(OraxenMeta meta) {
        JsonObject baseModel = createModelObject(meta.getModelName());

        // Add tints for dyeable/potion materials
        addTintsIfNeeded(baseModel);

        // Handle state-based models for this specific item
        return wrapItemWithStateHandling(baseModel, meta);
    }

    /**
     * Wraps an item's model with state-based conditions if configured.
     */
    private JsonObject wrapItemWithStateHandling(JsonObject baseModel, OraxenMeta meta) {
        return switch (material) {
            case BOW -> meta.hasPullingModels() ? createBowPullingModel(baseModel, meta) : baseModel;
            case CROSSBOW -> createCrossbowModel(baseModel, meta);
            case FISHING_ROD -> meta.hasCastModel() ? createFishingRodModel(baseModel, meta) : baseModel;
            case SHIELD -> meta.hasBlockingModel() ? createShieldModel(baseModel, meta) : baseModel;
            default -> baseModel;
        };
    }

    // =====================================================================
    // Vanilla State-Based Models
    // =====================================================================

    private JsonObject createVanillaBowModel(JsonObject baseModel, String vanillaModelPath) {
        JsonObject conditionModel = new JsonObject();
        conditionModel.addProperty("type", "minecraft:condition");
        conditionModel.addProperty("property", "minecraft:using_item");
        conditionModel.add("on_false", baseModel);

        // on_true: range_dispatch for pulling states
        JsonObject rangeDispatch = new JsonObject();
        rangeDispatch.addProperty("type", "minecraft:range_dispatch");
        rangeDispatch.addProperty("property", "minecraft:use_duration");
        rangeDispatch.addProperty("scale", BOW_USE_DURATION_SCALE);

        JsonArray entries = new JsonArray();
        JsonObject entry1 = new JsonObject();
        entry1.addProperty("threshold", BOW_PULL_THRESHOLD_1);
        entry1.add("model", createModelObject("minecraft:item/bow_pulling_1"));
        entries.add(entry1);

        JsonObject entry2 = new JsonObject();
        entry2.addProperty("threshold", BOW_PULL_THRESHOLD_2);
        entry2.add("model", createModelObject("minecraft:item/bow_pulling_2"));
        entries.add(entry2);

        rangeDispatch.add("entries", entries);
        rangeDispatch.add("fallback", createModelObject("minecraft:item/bow_pulling_0"));

        conditionModel.add("on_true", rangeDispatch);
        return conditionModel;
    }

    private JsonObject createVanillaCrossbowModel(JsonObject baseModel, String vanillaModelPath) {
        // Build charged model selection
        JsonObject selectModel = new JsonObject();
        selectModel.addProperty("type", "minecraft:select");
        selectModel.addProperty("property", "minecraft:charge_type");

        JsonArray cases = new JsonArray();
        JsonObject arrowCase = new JsonObject();
        arrowCase.addProperty("when", "arrow");
        arrowCase.add("model", createModelObject("minecraft:item/crossbow_arrow"));
        cases.add(arrowCase);

        JsonObject rocketCase = new JsonObject();
        rocketCase.addProperty("when", "rocket");
        rocketCase.add("model", createModelObject("minecraft:item/crossbow_firework"));
        cases.add(rocketCase);

        selectModel.add("cases", cases);
        selectModel.add("fallback", baseModel);

        // Wrap with pulling condition
        JsonObject pullingCondition = new JsonObject();
        pullingCondition.addProperty("type", "minecraft:condition");
        pullingCondition.addProperty("property", "minecraft:using_item");
        pullingCondition.add("on_false", selectModel);

        // on_true: range_dispatch for pulling states
        JsonObject rangeDispatch = new JsonObject();
        rangeDispatch.addProperty("type", "minecraft:range_dispatch");
        rangeDispatch.addProperty("property", "minecraft:crossbow/pull");

        JsonArray entries = new JsonArray();
        JsonObject entry1 = new JsonObject();
        entry1.addProperty("threshold", CROSSBOW_PULL_THRESHOLD_1);
        entry1.add("model", createModelObject("minecraft:item/crossbow_pulling_1"));
        entries.add(entry1);

        JsonObject entry2 = new JsonObject();
        entry2.addProperty("threshold", CROSSBOW_PULL_THRESHOLD_2);
        entry2.add("model", createModelObject("minecraft:item/crossbow_pulling_2"));
        entries.add(entry2);

        rangeDispatch.add("entries", entries);
        rangeDispatch.add("fallback", createModelObject("minecraft:item/crossbow_pulling_0"));

        pullingCondition.add("on_true", rangeDispatch);
        return pullingCondition;
    }

    private JsonObject createVanillaFishingRodModel(JsonObject baseModel, String vanillaModelPath) {
        JsonObject conditionModel = new JsonObject();
        conditionModel.addProperty("type", "minecraft:condition");
        conditionModel.addProperty("property", "minecraft:fishing_rod/cast");
        conditionModel.add("on_false", baseModel);
        conditionModel.add("on_true", createModelObject("minecraft:item/fishing_rod_cast"));
        return conditionModel;
    }

    private JsonObject createVanillaShieldModel(JsonObject baseModel, String vanillaModelPath) {
        JsonObject conditionModel = new JsonObject();
        conditionModel.addProperty("type", "minecraft:condition");
        conditionModel.addProperty("property", "minecraft:using_item");
        conditionModel.add("on_false", baseModel);
        conditionModel.add("on_true", createModelObject("minecraft:item/shield_blocking"));
        return conditionModel;
    }

    // =====================================================================
    // Custom Item State-Based Models (from ModelDefinitionGenerator)
    // =====================================================================

    private JsonObject createBowPullingModel(JsonObject baseModel, OraxenMeta meta) {
        JsonObject conditionModel = new JsonObject();
        conditionModel.addProperty("type", "minecraft:condition");
        conditionModel.addProperty("property", "minecraft:using_item");
        conditionModel.add("on_false", baseModel);

        JsonObject rangeDispatch = new JsonObject();
        rangeDispatch.addProperty("type", "minecraft:range_dispatch");
        rangeDispatch.addProperty("property", "minecraft:use_duration");
        rangeDispatch.addProperty("scale", BOW_USE_DURATION_SCALE);

        List<String> pullingModels = meta.getPullingModels();
        if (pullingModels == null || pullingModels.isEmpty()) {
            return baseModel;
        }

        JsonArray entries = new JsonArray();
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
        rangeDispatch.add("fallback", createModelObject(pullingModels.get(0)));
        conditionModel.add("on_true", rangeDispatch);
        return conditionModel;
    }

    private JsonObject createCrossbowModel(JsonObject baseModel, OraxenMeta meta) {
        JsonObject onFalseModel = baseModel;

        if (meta.hasChargedModel() || meta.hasFireworkModel()) {
            JsonObject selectModel = new JsonObject();
            selectModel.addProperty("type", "minecraft:select");
            selectModel.addProperty("property", "minecraft:charge_type");

            JsonArray cases = new JsonArray();
            if (meta.hasChargedModel()) {
                JsonObject arrowCase = new JsonObject();
                arrowCase.addProperty("when", "arrow");
                arrowCase.add("model", createModelObject(meta.getChargedModel()));
                cases.add(arrowCase);
            }
            if (meta.hasFireworkModel()) {
                JsonObject rocketCase = new JsonObject();
                rocketCase.addProperty("when", "rocket");
                rocketCase.add("model", createModelObject(meta.getFireworkModel()));
                cases.add(rocketCase);
            }

            selectModel.add("cases", cases);
            selectModel.add("fallback", baseModel);
            onFalseModel = selectModel;
        }

        if (!meta.hasPullingModels()) {
            return onFalseModel;
        }

        List<String> pullingModels = meta.getPullingModels();
        if (pullingModels == null || pullingModels.isEmpty()) {
            return onFalseModel;
        }

        JsonObject pullingCondition = new JsonObject();
        pullingCondition.addProperty("type", "minecraft:condition");
        pullingCondition.addProperty("property", "minecraft:using_item");
        pullingCondition.add("on_false", onFalseModel);

        JsonObject rangeDispatch = new JsonObject();
        rangeDispatch.addProperty("type", "minecraft:range_dispatch");
        rangeDispatch.addProperty("property", "minecraft:crossbow/pull");

        JsonArray entries = new JsonArray();
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
        rangeDispatch.add("fallback", createModelObject(pullingModels.get(0)));
        pullingCondition.add("on_true", rangeDispatch);
        return pullingCondition;
    }

    private JsonObject createFishingRodModel(JsonObject baseModel, OraxenMeta meta) {
        JsonObject conditionModel = new JsonObject();
        conditionModel.addProperty("type", "minecraft:condition");
        conditionModel.addProperty("property", "minecraft:fishing_rod/cast");
        conditionModel.add("on_false", baseModel);
        conditionModel.add("on_true", createModelObject(meta.getCastModel()));
        return conditionModel;
    }

    private JsonObject createShieldModel(JsonObject baseModel, OraxenMeta meta) {
        JsonObject conditionModel = new JsonObject();
        conditionModel.addProperty("type", "minecraft:condition");
        conditionModel.addProperty("property", "minecraft:using_item");
        conditionModel.add("on_false", baseModel);
        conditionModel.add("on_true", createModelObject(meta.getBlockingModel()));
        return conditionModel;
    }

    // =====================================================================
    // Utility Methods
    // =====================================================================

    private void addTintsIfNeeded(JsonObject model) {
        if (material.name().startsWith("LEATHER_")) {
            addDyeTint(model);
        } else if (isPotionMaterial(material)) {
            addPotionTint(model);
        }
    }

    private boolean isPotionMaterial(Material material) {
        return material == Material.POTION
            || material == Material.SPLASH_POTION
            || material == Material.LINGERING_POTION;
    }

    private void addDyeTint(JsonObject model) {
        JsonArray tints = new JsonArray();
        JsonObject dye = new JsonObject();
        dye.addProperty("type", "minecraft:dye");
        dye.addProperty("default", DEFAULT_TINT_COLOR);
        tints.add(dye);
        model.add("tints", tints);
    }

    private void addPotionTint(JsonObject model) {
        JsonArray tints = new JsonArray();
        JsonObject potion = new JsonObject();
        potion.addProperty("type", "minecraft:potion");
        potion.addProperty("default", DEFAULT_TINT_COLOR);
        tints.add(potion);
        model.add("tints", tints);
    }

    private JsonObject createModelObject(String modelPath) {
        JsonObject modelObj = new JsonObject();
        modelObj.addProperty("type", "minecraft:model");
        modelObj.addProperty("model", modelPath);
        return modelObj;
    }

    /**
     * Helper record to store CMD value and its associated model.
     */
    private record CmdEntry(int cmd, JsonObject model) {}
}

