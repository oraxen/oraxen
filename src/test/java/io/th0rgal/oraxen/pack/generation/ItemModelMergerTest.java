package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.junit.jupiter.api.Assertions.*;

class ItemModelMergerTest {
    private static JsonObject json(String value) { return JsonParser.parseString(value).getAsJsonObject(); }

    @Test
    void lowerCustomModelCasesRemainAvailableAtEveryNativeBowDrawThreshold() {
        JsonObject higher = VanillaItemDefinitionGenerator.createVanillaFallback(org.bukkit.Material.BOW);
        JsonObject lower = json("""
            {"type":"minecraft:select","property":"minecraft:custom_model_data","cases":[
             {"when":"custom","model":{"type":"minecraft:model","model":"custom_bow"}}],
             "fallback":{"type":"minecraft:model","model":"lower_default"}}
            """);
        JsonObject merged = ItemModelMerger.mergeModel(higher, lower);
        JsonObject originalDraw = higher.getAsJsonObject("on_true");
        JsonObject mergedDraw = merged.getAsJsonObject("on_true");
        assertEquals(originalDraw.get("property"), mergedDraw.get("property"));
        assertEquals(originalDraw.get("scale"), mergedDraw.get("scale"));
        for (int i = 0; i < originalDraw.getAsJsonArray("entries").size(); i++) {
            JsonObject originalEntry = originalDraw.getAsJsonArray("entries").get(i).getAsJsonObject();
            JsonObject mergedEntry = mergedDraw.getAsJsonArray("entries").get(i).getAsJsonObject();
            assertEquals(originalEntry.get("threshold"), mergedEntry.get("threshold"));
            assertEquals(lower.get("cases"), mergedEntry.getAsJsonObject("model").get("cases"));
            assertEquals(originalEntry.get("model"), mergedEntry.getAsJsonObject("model").get("fallback"));
        }
        assertEquals(originalDraw.get("fallback"), mergedDraw.getAsJsonObject("fallback").get("fallback"));
        assertEquals(higher.get("on_false"), merged.getAsJsonObject("on_false").get("fallback"));
    }

    @Test
    void nestedStateBranchKeepsHigherCustomRuleAtSameThreshold() {
        JsonObject higher = json("""
            {"type":"minecraft:range_dispatch","property":"minecraft:use_duration","scale":0.05,
             "entries":[{"threshold":0.65,"model":{"type":"minecraft:range_dispatch","property":"minecraft:custom_model_data",
             "entries":[{"threshold":2,"model":{"type":"minecraft:model","model":"higher_drawn"}}],
             "fallback":{"type":"minecraft:model","model":"drawn_default"}}}],
             "fallback":{"type":"minecraft:model","model":"idle"}}
            """);
        JsonObject lower = json("""
            {"type":"minecraft:range_dispatch","property":"minecraft:custom_model_data",
             "entries":[{"threshold":2,"model":{"type":"minecraft:model","model":"lower"}},
             {"threshold":3,"model":{"type":"minecraft:model","model":"lower_extra"}}],
             "fallback":{"type":"minecraft:model","model":"lower_default"}}
            """);
        JsonObject branch = ItemModelMerger.mergeModel(higher, lower).getAsJsonArray("entries").get(0)
                .getAsJsonObject().getAsJsonObject("model");
        assertEquals(2, branch.getAsJsonArray("entries").size());
        assertEquals("higher_drawn", branch.getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("model").get("model").getAsString());
        assertEquals("drawn_default", branch.getAsJsonObject("fallback").get("model").getAsString());
    }

    @Test
    void mergesFractionalThresholdsAndKeepsSelectorSettingsAndPriority() {
        JsonObject higher = json("""
            {"type":"minecraft:range_dispatch","property":"minecraft:custom_model_data","index":2,"scale":0.5,
             "entries":[{"threshold":1.25,"model":{"type":"minecraft:model","model":"high"}}],
             "fallback":{"type":"minecraft:model","model":"default"}}
            """);
        JsonObject lower = higher.deepCopy();
        lower.getAsJsonArray("entries").add(json("""
            {"threshold":1.75,"model":{"type":"minecraft:model","model":"low"}}
            """));
        JsonObject merged = ItemModelMerger.mergeModel(higher, lower);
        assertEquals(2, merged.getAsJsonArray("entries").size());
        assertEquals(1.75, merged.getAsJsonArray("entries").get(1).getAsJsonObject().get("threshold").getAsDouble());
        assertEquals(higher.get("scale"), merged.get("scale"));
        assertEquals(higher.get("index"), merged.get("index"));
        assertEquals(higher.getAsJsonArray("entries").get(0), merged.getAsJsonArray("entries").get(0));
    }

    @Test
    void mergesStringCasesPerValueAndKeepsNumericChannelWithHigherDefault() {
        JsonObject higher = json("""
            {"type":"minecraft:select","property":"minecraft:custom_model_data","cases":[
             {"when":["a","b"],"model":{"type":"minecraft:model","model":"high"}}],
             "fallback":{"type":"minecraft:model","model":"high_default"}}
            """);
        JsonObject lower = json("""
            {"type":"minecraft:select","property":"minecraft:custom_model_data","cases":[
             {"when":["b","c"],"model":{"type":"minecraft:model","model":"low"}}],
             "fallback":{"type":"minecraft:range_dispatch","property":"minecraft:custom_model_data","index":3,
             "entries":[{"threshold":4.5,"model":{"type":"minecraft:model","model":"numeric"}}],
             "fallback":{"type":"minecraft:model","model":"low_default"}}}
            """);
        JsonObject merged = ItemModelMerger.mergeModel(higher, lower);
        assertEquals("c", merged.getAsJsonArray("cases").get(1).getAsJsonObject().get("when").getAsString());
        assertEquals(3, merged.getAsJsonObject("fallback").get("index").getAsInt());
        assertEquals(higher.get("fallback"), merged.getAsJsonObject("fallback").get("fallback"));
    }

    @Test
    void keepsConditionTreeAndMakesLowerSelectorsAvailableInBranches() {
        JsonObject high = json("""
            {"type":"minecraft:condition","property":"minecraft:using_item",
             "on_true":{"type":"minecraft:model","model":"active"},"on_false":{"type":"minecraft:model","model":"idle"}}
            """);
        JsonObject low = json("""
            {"type":"minecraft:select","property":"minecraft:custom_model_data","cases":[],
             "fallback":{"type":"minecraft:model","model":"low"}}
            """);
        JsonObject merged = ItemModelMerger.mergeModel(high, low);
        assertEquals(high.get("property"), merged.get("property"));
        assertEquals(high.get("on_true"), merged.getAsJsonObject("on_true").get("fallback"));
        assertEquals(high.get("on_false"), merged.getAsJsonObject("on_false").get("fallback"));
    }

    @Test
    void legacyMergeKeepsAppearanceAndOrdersImportedThresholds() {
        JsonObject high = json("""
            {"parent":"item/handheld","display":{"gui":{"scale":[2,2,2]}},"gui_light":"front","elements":[],
             "overrides":[{"predicate":{"custom_model_data":20},"model":"high"}]}
            """);
        JsonObject low = json("""
            {"parent":"item/generated","overrides":[{"predicate":{"custom_model_data":10},"model":"low"}]}
            """);
        JsonObject merged = ItemModelMerger.mergeLegacy(high, low);
        for (String key : new String[]{"parent", "display", "gui_light", "elements"}) assertEquals(high.get(key), merged.get(key));
        assertEquals(10, merged.getAsJsonArray("overrides").get(0).getAsJsonObject().getAsJsonObject("predicate").get("custom_model_data").getAsInt());
    }

    @Test
    void conversionKeepsCompoundConditionsAndRejectsUnknownPredicates() {
        JsonObject legacy = json("""
            {"overrides":[{"predicate":{"custom_model_data":1.5,"pulling":1,"pull":0.65},"model":"drawn"}]}
            """);
        JsonObject converted = ItemModelMerger.convertLegacy(legacy, "bow");
        JsonObject pull = converted.getAsJsonObject("model");
        assertEquals("minecraft:use_duration", pull.get("property").getAsString());
        assertEquals(0.65, pull.getAsJsonArray("entries").get(0).getAsJsonObject().get("threshold").getAsDouble(), 0.000001);
        assertEquals(0.05, pull.get("scale").getAsDouble(), 0.000001);
        JsonObject using = pull.getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("model");
        assertEquals("minecraft:using_item", using.get("property").getAsString());
        assertEquals(1.5, using.getAsJsonObject("on_true").getAsJsonArray("entries").get(0).getAsJsonObject().get("threshold").getAsDouble());
        legacy.getAsJsonArray("overrides").get(0).getAsJsonObject().getAsJsonObject("predicate").addProperty("unknown", 1);
        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            assertNull(ItemModelMerger.convertLegacy(legacy, "bow"));
        }
    }

    @Test
    void conversionKeepsNativeCrossbowStateFallback() {
        JsonObject legacy = json("""
            {"overrides":[{"predicate":{"custom_model_data":2},"model":"custom"}]}
            """);
        JsonObject converted = ItemModelMerger.convertLegacy(legacy, "crossbow");
        assertEquals(VanillaItemDefinitionGenerator.createVanillaFallback(org.bukkit.Material.CROSSBOW),
                converted.getAsJsonObject("model").get("fallback"));
        assertTrue(converted.toString().contains("minecraft:charge_type"));
        assertTrue(converted.toString().contains("minecraft:crossbow/pull"));
    }

    @Test
    void armorConversionNeedsExistingTrimDefinitionAndPreservesIt() {
        JsonObject legacy = json("""
            {"overrides":[{"predicate":{"custom_model_data":2},"model":"custom_armor"}]}
            """);
        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            assertNull(ItemModelMerger.convertLegacy(legacy, "diamond_chestplate"));
            logs.verify(() -> Logs.logWarning(org.mockito.ArgumentMatchers.contains("diamond_chestplate")));
        }
        JsonObject baseline = json("""
            {"hand_animation_on_swap":false,"model":{"type":"minecraft:select","property":"minecraft:trim_material",
             "cases":[{"when":"quartz","model":{"type":"minecraft:model","model":"trimmed_armor"}}],
             "fallback":{"type":"minecraft:model","model":"plain_armor"}}}
            """);
        JsonObject converted = ItemModelMerger.convertLegacy(legacy, "diamond_chestplate", baseline);
        assertEquals(baseline.get("model"), converted.getAsJsonObject("model").get("fallback"));
        assertEquals(baseline.get("hand_animation_on_swap"), converted.get("hand_animation_on_swap"));
        assertEquals("custom_armor", converted.getAsJsonObject("model").getAsJsonArray("entries").get(0)
                .getAsJsonObject().getAsJsonObject("model").get("model").getAsString());
    }

    @Test
    void unsupportedVanillaFamiliesNeverPublishStaticReplacementWithoutBaseline() {
        JsonObject legacy = json("""
            {"overrides":[{"predicate":{"custom_model_data":2},"model":"custom"}]}
            """);
        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            for (String name : new String[]{"elytra", "goat_horn", "white_shulker_box", "white_banner", "chest", "red_bed", "pig_spawn_egg", "tipped_arrow", "firework_star"})
                assertNull(ItemModelMerger.convertLegacy(legacy, name), name);
        }
    }

    @Test
    void layerConversionGivesHigherLegacyPriorityOverLowerModern() {
        JsonObject legacy = json("""
            {"overrides":[{"predicate":{"custom_model_data":2},"model":"high_custom"}]}
            """);
        JsonObject lowerModern = json("""
            {"model":{"type":"minecraft:range_dispatch","property":"minecraft:custom_model_data","index":0,
             "entries":[{"threshold":2,"model":{"type":"minecraft:model","model":"low_custom"}}],
             "fallback":{"type":"minecraft:model","model":"default"}}}
            """);
        java.util.Map<String, byte[]> lower = java.util.Map.of("assets/minecraft/items/paper.json", lowerModern.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.util.Map<String, byte[]> high = java.util.Map.of("assets/minecraft/models/item/paper.json", legacy.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.util.Map<String, byte[]> converted = ItemModelMerger.convertLegacyLayer(high, lower);
        JsonObject modern = json(new String(converted.get("assets/minecraft/items/paper.json"), java.nio.charset.StandardCharsets.UTF_8));
        JsonObject merged = ItemModelMerger.mergeDefinition(modern, lowerModern);
        assertEquals("high_custom", merged.getAsJsonObject("model").getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("model").get("model").getAsString());
    }

    @Test
    void compositeKeepsPartsAndMergesCompatibleNestedSelector() {
        JsonObject high = json("""
            {"type":"minecraft:composite","models":[{"type":"minecraft:model","model":"decoration"},
             {"type":"minecraft:select","property":"minecraft:custom_model_data","cases":[],
              "fallback":{"type":"minecraft:model","model":"high_default"}}]}
            """);
        JsonObject low = json("""
            {"type":"minecraft:select","property":"minecraft:custom_model_data","cases":[
             {"when":"new","model":{"type":"minecraft:model","model":"custom"}}],
             "fallback":{"type":"minecraft:model","model":"low_default"}}
            """);
        JsonObject merged = ItemModelMerger.mergeModel(high, low);
        assertEquals(high.getAsJsonArray("models").get(0), merged.getAsJsonArray("models").get(0));
        assertEquals(1, merged.getAsJsonArray("models").get(1).getAsJsonObject().getAsJsonArray("cases").size());
        assertEquals("high_default", merged.getAsJsonArray("models").get(1).getAsJsonObject().getAsJsonObject("fallback").get("model").getAsString());
    }
}
