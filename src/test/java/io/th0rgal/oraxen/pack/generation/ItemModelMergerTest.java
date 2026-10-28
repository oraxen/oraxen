package io.th0rgal.oraxen.pack.generation;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
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
        JsonObject merged = ItemModelMerger.mergeLegacy(high, low, "assets/minecraft/models/item/paper.json");
        for (String key : new String[]{"parent", "display", "gui_light", "elements"}) assertEquals(high.get(key), merged.get(key));
        assertEquals(10, merged.getAsJsonArray("overrides").get(0).getAsJsonObject().getAsJsonObject("predicate").get("custom_model_data").getAsInt());
    }

    @Test
    void legacyMergeKeepsUnconditionalOverridePositionAndPriority() {
        JsonObject lower = json("""
            {"overrides":[{"predicate":{"custom_model_data":5},"model":"lower_specific"}]}
            """);
        JsonObject higher = json("""
            {"overrides":[
             {"predicate":{"custom_model_data":3},"model":"higher_specific"},
             {"predicate":{},"model":"higher_unconditional"},
             {"predicate":{"custom_model_data":8},"model":"higher_later_specific"}]}
            """);

        JsonObject merged = ItemModelMerger.mergeLegacy(higher, lower, "assets/minecraft/models/item/paper.json");

        assertEquals("higher_unconditional", legacyModel(merged, 5, 0, false, 0));
        assertEquals("higher_later_specific", legacyModel(merged, 8, 0, false, 0));
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
    void leatherHorseArmorConversionUsesTintedVanillaFallback() {
        JsonObject legacy = json("""
            {"overrides":[{"predicate":{"custom_model_data":17},"model":"oraxen:item/custom_horse_armor"}]}
            """);

        JsonObject converted;
        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            converted = ItemModelMerger.convertLegacy(legacy, "leather_horse_armor");
        }

        assertNotNull(converted);
        JsonObject model = converted.getAsJsonObject("model");
        JsonObject entry = model.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals(17, entry.get("threshold").getAsInt());
        assertEquals("oraxen:item/custom_horse_armor",
                entry.getAsJsonObject("model").get("model").getAsString());
        JsonObject fallback = model.getAsJsonObject("fallback");
        assertEquals("minecraft:model", fallback.get("type").getAsString());
        assertEquals("minecraft:item/leather_horse_armor", fallback.get("model").getAsString());
        assertEquals(0xFFA06540, fallback.getAsJsonArray("tints").get(0).getAsJsonObject().get("default").getAsInt());
    }

    @Test
    void staticVanillaItemFallbacksUseTheirDefaultModels() {
        JsonObject legacy = json("""
            {"overrides":[{"predicate":{"custom_model_data":17},"model":"test:item/custom"}]}
            """);

        for (String itemName : List.of("diamond_horse_armor", "golden_horse_armor", "iron_horse_armor",
                "totem_of_undying", "honey_bottle", "glow_ink_sac", "heart_of_the_sea", "nautilus_shell",
                "beef", "cooked_beef", "rabbit_stew", "written_book", "armor_stand", "axolotl_bucket",
                "acacia_boat", "music_disc_13", "angler_pottery_sherd", "rib_armor_trim_smithing_template")) {
            JsonObject converted = ItemModelMerger.convertLegacy(legacy, itemName);

            assertNotNull(converted, itemName);
            JsonObject fallback = converted.getAsJsonObject("model").getAsJsonObject("fallback");
            assertEquals("minecraft:model", fallback.get("type").getAsString(), itemName);
            assertEquals("minecraft:item/" + itemName, fallback.get("model").getAsString(), itemName);
            assertFalse(fallback.has("tints"), itemName);
        }
    }

    @Test
    void conversionGroupsTwentyCompoundRulesWithoutExponentialOutput() {
        JsonObject baseline = json("""
            {"hand_animation_on_swap":false,"model":{"type":"minecraft:model","model":"baseline"}}
            """);
        for (String item : new String[]{"paper", "bow"}) {
            JsonArray overrides = new JsonArray();
            for (int i = 1; i <= 20; i++) {
                JsonObject predicates = new JsonObject();
                predicates.addProperty("custom_model_data", i);
                if (item.equals("bow")) {
                    predicates.addProperty("pulling", 1);
                    predicates.addProperty("pull", i / 20.0);
                } else predicates.addProperty("damage", i / 20.0);
                JsonObject override = new JsonObject();
                override.add("predicate", predicates);
                override.addProperty("model", "custom_" + i);
                overrides.add(override);
            }
            JsonObject legacy = new JsonObject();
            legacy.add("overrides", overrides);
            JsonObject converted = ItemModelMerger.convertLegacy(legacy, item, baseline);
            assertTrue(converted.toString().length() < 100_000, item + " conversion must keep shared selector branches compact");
            assertEquals(baseline.get("hand_animation_on_swap"), converted.get("hand_animation_on_swap"));
            for (int customModelData = 0; customModelData <= 21; customModelData++) {
                for (int state = 0; state <= 21; state++) {
                    for (int using = 0; using <= 1; using++) {
                        double value = state / 20.0;
                        assertEquals(legacyModel(legacy, customModelData, value, using == 1, value),
                                convertedModel(converted.getAsJsonObject("model"), customModelData, value, using == 1, value),
                                item + " custom_model_data=" + customModelData + " state=" + state + " using=" + using);
                    }
                }
            }
            assertEquals("baseline", baseline.getAsJsonObject("model").get("model").getAsString());
        }
    }

    @Test
    void missingModernSelectorFallbacksSurviveLegacyConversion() {
        String modernPath = "assets/minecraft/items/paper.json";
        String legacyPath = "assets/minecraft/models/item/paper.json";
        String legacy = """
                {"overrides":[{"predicate":{"custom_model_data":17},"model":"test:legacy"}]}
                """;
        List<JsonObject> modernModels = List.of(
                json("""
                    {"type":"minecraft:range_dispatch","property":"minecraft:custom_model_data","index":0,
                     "entries":[{"threshold":5,"model":{"type":"minecraft:model","model":"test:modern"}}]}
                    """),
                json("""
                    {"type":"minecraft:select","property":"minecraft:custom_model_data","index":0,
                     "cases":[{"when":"existing","model":{"type":"minecraft:model","model":"test:modern"}}]}
                    """),
                json("""
                    {"type":"minecraft:condition","property":"minecraft:using_item",
                     "on_true":{"type":"minecraft:range_dispatch","property":"minecraft:custom_model_data","index":0,
                       "entries":[{"threshold":5,"model":{"type":"minecraft:model","model":"test:modern"}}]},
                     "on_false":{"type":"minecraft:model","model":"test:idle"}}
                    """));

        for (JsonObject modernModel : modernModels) {
            JsonObject modernDefinition = new JsonObject();
            modernDefinition.add("model", modernModel);
            for (boolean ownModern : List.of(true, false)) {
                Map<String, byte[]> active = new LinkedHashMap<>();
                active.put(legacyPath, legacy.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                Map<String, byte[]> lower = new LinkedHashMap<>();
                if (ownModern) active.put(modernPath, modernDefinition.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                else lower.put(modernPath, modernDefinition.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

                Map<String, byte[]> converted = ItemModelMerger.convertLegacyLayer(active, lower);
                JsonObject result = json(new String(converted.get(modernPath), java.nio.charset.StandardCharsets.UTF_8));
                if (!ownModern) result = ItemModelMerger.mergeDefinition(result, modernDefinition, modernPath);
                JsonObject model = result.getAsJsonObject("model");
                String context = (ownModern ? "own" : "lower") + " modern " + modernModel.get("type").getAsString();

                assertEquals("<missing>", resolvedModel(model, 0, null, true), context + " unmatched value");
                assertEquals("test:legacy", resolvedModel(model, 17, null, true), context + " legacy override");
                if (modernModel.get("type").getAsString().equals("minecraft:select")) {
                    assertEquals("test:modern", resolvedModel(model, 0, "existing", true), context + " existing case");
                } else {
                    assertEquals("test:modern", resolvedModel(model, 5, null, true), context + " existing threshold");
                }
                if (modernModel.get("type").getAsString().equals("minecraft:condition"))
                    assertEquals("test:idle", resolvedModel(model, 0, null, false), context + " false condition branch");
                assertFalse(result.toString().contains("minecraft:item/paper"), context + " must not synthesize a vanilla fallback");
            }
        }
    }

    private static String resolvedModel(JsonObject model, double customModelData, String selectValue, boolean using) {
        String type = model.has("type") ? model.get("type").getAsString().replace("minecraft:", "") : "";
        if (type.equals("model")) return model.get("model").getAsString();
        if (type.equals("condition"))
            return resolvedModel(model.getAsJsonObject(using ? "on_true" : "on_false"), customModelData, selectValue, using);
        if (type.equals("select")) {
            for (JsonElement element : model.getAsJsonArray("cases")) {
                JsonObject entry = element.getAsJsonObject();
                JsonElement when = entry.get("when");
                boolean matches = when.isJsonArray()
                        ? when.getAsJsonArray().asList().stream().anyMatch(value -> value.getAsString().equals(selectValue))
                        : selectValue != null && when.getAsString().equals(selectValue);
                if (matches) return resolvedModel(entry.getAsJsonObject("model"), customModelData, selectValue, using);
            }
            return model.has("fallback")
                    ? resolvedModel(model.getAsJsonObject("fallback"), customModelData, selectValue, using)
                    : "<missing>";
        }
        if (!type.equals("range_dispatch") || !model.get("property").getAsString().equals("minecraft:custom_model_data"))
            return "<unsupported>";
        JsonObject selected = null;
        double threshold = Double.NEGATIVE_INFINITY;
        for (JsonElement element : model.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            double candidate = entry.get("threshold").getAsDouble();
            if (candidate <= customModelData && candidate >= threshold) {
                selected = entry.getAsJsonObject("model");
                threshold = candidate;
            }
        }
        if (selected != null) return resolvedModel(selected, customModelData, selectValue, using);
        return model.has("fallback")
                ? resolvedModel(model.getAsJsonObject("fallback"), customModelData, selectValue, using)
                : "<missing>";
    }

    @Test
    void conversionKeepsLastMatchingRuleAcrossOverlappingCompoundPredicates() {
        JsonObject legacy = json("""
            {"overrides":[
             {"predicate":{"custom_model_data":5,"damage":0.2},"model":"first"},
             {"predicate":{"pulling":1,"pull":0.5,"custom_model_data":2},"model":"drawn"},
             {"predicate":{"custom_model_data":3,"damage":0.6},"model":"damaged"},
             {"predicate":{"custom_model_data":1,"damage":0.8,"pulling":0},"model":"later_lower_threshold"},
             {"predicate":{"damage":0.4,"pulling":1,"pull":0.7},"model":"last"}]}
            """);
        JsonObject baseline = json("""
            {"model":{"type":"minecraft:model","model":"baseline"}}
            """);
        JsonObject converted = ItemModelMerger.convertLegacy(legacy, "bow", baseline);
        for (int customModelData = 0; customModelData <= 6; customModelData++) {
            for (double damage : new double[]{0, 0.2, 0.4, 0.6, 0.8}) {
                for (boolean using : new boolean[]{false, true}) {
                    for (double pull : new double[]{0, 0.5, 0.7, 1}) {
                        assertEquals(legacyModel(legacy, customModelData, damage, using, pull),
                                convertedModel(converted.getAsJsonObject("model"), customModelData, damage, using, pull));
                    }
                }
            }
        }
    }

    @Test
    void conversionDiscardsEarlierRulesAfterUnconditionalOverride() {
        JsonObject legacy = json("""
            {"overrides":[
             {"predicate":{"custom_model_data":1,"damage":0.2},"model":"earlier"},
             {"predicate":{},"model":"unconditional"},
             {"predicate":{"custom_model_data":2,"damage":0.6},"model":"later"}]}
            """);
        JsonObject baseline = json("""
            {"model":{"type":"minecraft:model","model":"baseline"}}
            """);
        JsonObject converted = ItemModelMerger.convertLegacy(legacy, "paper", baseline);
        assertEquals("unconditional", convertedModel(converted.getAsJsonObject("model"), 1, 0.8, false, 0));
        assertEquals("unconditional", convertedModel(converted.getAsJsonObject("model"), 3, 0.4, false, 0));
        assertEquals("later", convertedModel(converted.getAsJsonObject("model"), 3, 0.8, false, 0));
        legacy.getAsJsonArray("overrides").add(json("""
            {"predicate":{},"model":"last"}
            """));
        assertEquals(json("""
            {"type":"minecraft:model","model":"last"}
            """), ItemModelMerger.convertLegacy(legacy, "paper", baseline).get("model"));
    }

    private static String legacyModel(JsonObject legacy, double customModelData, double damage, boolean using, double pull) {
        String model = "baseline";
        for (JsonElement element : legacy.getAsJsonArray("overrides")) {
            JsonObject override = element.getAsJsonObject();
            boolean matches = true;
            for (var predicate : override.getAsJsonObject("predicate").entrySet()) {
                double value = switch (predicate.getKey()) {
                    case "custom_model_data" -> customModelData;
                    case "damage" -> damage;
                    case "pulling" -> using ? 1 : 0;
                    case "pull" -> pull;
                    default -> throw new AssertionError("Unexpected test predicate " + predicate.getKey());
                };
                if (value < predicate.getValue().getAsDouble()) matches = false;
            }
            if (matches) model = override.get("model").getAsString();
        }
        return model;
    }

    private static String convertedModel(JsonObject node, double customModelData, double damage, boolean using, double pull) {
        if (node.get("type").getAsString().equals("minecraft:model")) return node.get("model").getAsString();
        String property = node.get("property").getAsString();
        if (property.equals("minecraft:using_item"))
            return convertedModel(node.getAsJsonObject(using ? "on_true" : "on_false"), customModelData, damage, using, pull);
        double value = switch (property) {
            case "minecraft:custom_model_data" -> customModelData;
            case "minecraft:damage" -> damage;
            case "minecraft:use_duration" -> pull;
            default -> throw new AssertionError("Unexpected test property " + property);
        };
        JsonObject selected = node.getAsJsonObject("fallback");
        for (JsonElement element : node.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            if (value >= entry.get("threshold").getAsDouble()) selected = entry.getAsJsonObject("model");
        }
        return convertedModel(selected, customModelData, damage, using, pull);
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
        JsonObject merged = ItemModelMerger.mergeDefinition(modern, lowerModern, "assets/minecraft/items/paper.json");
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

    @Test
    void malformedModernDuplicateKeepsHigherPriorityDefinition() {
        String resourcePath = "assets/minecraft/items/paper.json";
        JsonObject higher = json("""
            {"model":{"type":"minecraft:select","property":"minecraft:custom_model_data",
             "cases":[{"when":"high","model":{"type":"minecraft:model","model":"high_model"}}],
             "fallback":{"type":"minecraft:model","model":"high_default"}},"higher":"kept"}
            """);
        JsonObject malformedLower = json("""
            {"model":{"type":"minecraft:select","property":"minecraft:custom_model_data",
             "cases":[{"model":{"type":"minecraft:model","model":"broken"}}],
             "fallback":{"type":"minecraft:model","model":"low_default"}},"lower":"discarded"}
            """);

        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            assertEquals(higher, ItemModelMerger.mergeDefinition(higher, malformedLower, resourcePath));
            logs.verify(() -> Logs.logWarning(contains(resourcePath)), times(1));
        }
    }

    @Test
    void malformedLegacyDuplicateKeepsHigherPriorityModel() {
        String resourcePath = "assets/minecraft/models/item/paper.json";
        JsonObject higher = json("""
            {"parent":"item/handheld","overrides":[{"predicate":{"custom_model_data":2},"model":"high_model"}]}
            """);
        JsonObject malformedLower = json("""
            {"parent":"item/generated","overrides":{"invalid":"shape"}}
            """);

        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            assertEquals(higher, ItemModelMerger.mergeLegacy(higher, malformedLower, resourcePath));
            logs.verify(() -> Logs.logWarning(contains(resourcePath)), times(1));
        }
    }

    @Test
    void malformedHigherPriorityModernModelKeepsTheHigherDefinitionUnchanged() {
        String resourcePath = "assets/minecraft/items/paper.json";
        JsonObject higher = json("""
            {"model":"broken","higher":"kept"}
            """);
        JsonObject lower = json("""
            {"model":{"type":"minecraft:model","model":"lower_model"},"lower":"discarded"}
            """);

        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            assertEquals(higher, ItemModelMerger.mergeDefinition(higher, lower, resourcePath));
            logs.verify(() -> Logs.logWarning(contains(resourcePath)), times(1));
        }
    }

    @Test
    void malformedHigherPriorityLegacyModelKeepsTheHigherModelUnchanged() {
        String resourcePath = "assets/minecraft/models/item/paper.json";
        JsonObject higher = json("""
            {"parent":"item/handheld","overrides":{"invalid":"shape"}}
            """);
        JsonObject lower = json("""
            {"parent":"item/generated","overrides":[{"predicate":{"custom_model_data":1},"model":"lower_model"}]}
            """);

        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            assertEquals(higher, ItemModelMerger.mergeLegacy(higher, lower, resourcePath));
            logs.verify(() -> Logs.logWarning(contains(resourcePath)), times(1));
        }
    }
}
