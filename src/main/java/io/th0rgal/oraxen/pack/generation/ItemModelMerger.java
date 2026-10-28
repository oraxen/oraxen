package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import io.th0rgal.oraxen.utils.logs.Logs;

public final class ItemModelMerger {
    private ItemModelMerger() {}

    public static JsonObject convertLegacy(JsonObject legacy, String itemName) {
        return convertLegacy(legacy, itemName, null);
    }

    public static Map<String, byte[]> convertLegacyLayer(Map<String, byte[]> active, Map<String, byte[]> lower) {
        return convertLegacyLayer(active, lower, true);
    }

    public static Map<String, byte[]> convertLegacyLayer(Map<String, byte[]> active, Map<String, byte[]> lower,
            boolean mergeLowerModernDefinitions) {
        Map<String, byte[]> result = new LinkedHashMap<>(active);
        active.forEach((path, bytes) -> {
            if (!path.matches("assets/minecraft/models/item/[^/]+\\.json")) return;
            String name = path.substring(path.lastIndexOf('/') + 1, path.length() - 5);
            String modernPath = "assets/minecraft/items/" + name + ".json";
            try {
                JsonObject ownModern = object(active.get(modernPath));
                JsonObject lowerModern = ownModern == null ? object(lower.get(modernPath)) : null;
                JsonObject baselineDefinition = ownModern != null ? ownModern
                        : mergeLowerModernDefinitions ? lowerModern : null;
                JsonObject baseline = baselineDefinition != null
                        ? fallbackBaseline(baselineDefinition)
                        : !mergeLowerModernDefinitions ? lowerModern : null;
                JsonObject converted = convertLegacy(object(bytes), name, baseline);
                if (converted != null) {
                    if (ownModern != null) converted = mergeDefinition(ownModern, converted, modernPath);
                    result.put(modernPath, converted.toString().getBytes(StandardCharsets.UTF_8));
                }
            } catch (RuntimeException exception) {
                Logs.logWarning("Cannot convert legacy item rules for " + path);
                Logs.debug(exception);
            }
        });
        return result;
    }

    private static JsonObject fallbackBaseline(JsonObject itemDefinition) {
        if (!itemDefinition.has("model") || !itemDefinition.get("model").isJsonObject()) return null;
        JsonElement fallback = withoutCustomModelDataSelectors(itemDefinition.get("model"));
        if (fallback == null || !fallback.isJsonObject()) return null;
        JsonObject baseline = new JsonObject();
        baseline.add("model", fallback);
        return baseline;
    }

    private static JsonElement withoutCustomModelDataSelectors(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject model = element.getAsJsonObject();
            if (selector(model)) {
                JsonElement fallback = model.get("fallback");
                return fallback != null && fallback.isJsonObject()
                        ? withoutCustomModelDataSelectors(fallback)
                        : null;
            }
            JsonObject copy = model.deepCopy();
            for (Map.Entry<String, JsonElement> entry : model.entrySet()) {
                if (entry.getValue().isJsonObject() || entry.getValue().isJsonArray()) {
                    JsonElement nested = withoutCustomModelDataSelectors(entry.getValue());
                    if (nested == null) return null;
                    copy.add(entry.getKey(), nested);
                }
            }
            return copy;
        }
        if (element.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                JsonElement nested = child.isJsonObject() || child.isJsonArray()
                        ? withoutCustomModelDataSelectors(child)
                        : child.deepCopy();
                if (nested == null) return null;
                copy.add(nested);
            }
            return copy;
        }
        return element.deepCopy();
    }

    private static JsonObject object(byte[] bytes) {
        return bytes == null ? null : JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    public static JsonObject convertLegacy(JsonObject legacy, String itemName, JsonObject baseline) {
        if (legacy == null || !legacy.has("overrides")) return null;
        JsonObject fallback;
        if (baseline != null && baseline.has("model")) fallback = baseline.getAsJsonObject("model").deepCopy();
        else {
            org.bukkit.Material material = org.bukkit.Material.getMaterial(itemName.toUpperCase(Locale.ROOT));
            if (material == null) return null;
            fallback = VanillaItemDefinitionGenerator.createLegacyConversionFallback(material);
            if (fallback == null) {
                Logs.logWarning("Cannot convert legacy rules for " + itemName + " without an existing modern item definition. Its vanilla item rendering is not supported by the fallback generator");
                return null;
            }
        }
        List<LegacyRule> rules = new ArrayList<>();
        List<String> properties = new ArrayList<>();
        for (JsonElement element : legacy.getAsJsonArray("overrides")) {
            if (!element.isJsonObject()) continue;
            JsonObject override = element.getAsJsonObject();
            if (!override.has("predicate") || !override.has("model")) continue;
            JsonObject predicates = override.getAsJsonObject("predicate");
            Map<String, java.math.BigDecimal> thresholds = new LinkedHashMap<>();
            boolean supported = true;
            for (Map.Entry<String, JsonElement> predicate : predicates.entrySet()) {
                String property = switch (predicate.getKey()) {
                    case "custom_model_data" -> "minecraft:custom_model_data";
                    case "damage" -> "minecraft:damage";
                    case "pull" -> itemName.equals("crossbow") ? "minecraft:crossbow/pull" : "minecraft:use_duration";
                    case "pulling", "blocking" -> "minecraft:using_item";
                    case "cast" -> "minecraft:fishing_rod/cast";
                    default -> null;
                };
                if (property == null) {
                    supported = false;
                    break;
                }
                java.math.BigDecimal threshold = predicate.getValue().getAsBigDecimal();
                if (legacyCondition(property)) {
                    if (threshold.signum() <= 0) continue;
                    if (threshold.compareTo(java.math.BigDecimal.ONE) > 0) {
                        supported = false;
                        break;
                    }
                    threshold = java.math.BigDecimal.ONE;
                }
                thresholds.merge(property, threshold, java.math.BigDecimal::max);
            }
            if (supported) {
                JsonObject model = new JsonObject();
                model.addProperty("type", "minecraft:model");
                model.add("model", override.get("model").deepCopy());
                rules.add(new LegacyRule(thresholds, model));
                for (String property : thresholds.keySet()) if (!properties.contains(property)) properties.add(property);
            } else Logs.logWarning("Cannot convert legacy item rule for " + itemName + " with predicates " + predicates.keySet());
        }
        if (rules.isEmpty()) return null;
        Collections.reverse(properties);
        if (rules.size() > 1 && properties.remove("minecraft:custom_model_data"))
            properties.addFirst("minecraft:custom_model_data");
        JsonObject result = baseline == null ? new JsonObject() : baseline.deepCopy();
        result.add("model", legacySelectors(rules, properties, 0, fallback));
        return result;
    }

    private record LegacyRule(Map<String, java.math.BigDecimal> thresholds, JsonObject model) {}

    private static boolean legacyCondition(String property) {
        return property.equals("minecraft:using_item") || property.equals("minecraft:fishing_rod/cast");
    }

    private static JsonObject legacySelectors(List<LegacyRule> rules, List<String> properties, int index, JsonObject fallback) {
        // A matching rule replaces every earlier rule, including the original baseline.
        for (int i = rules.size() - 1; i >= 0; i--) {
            if (rules.get(i).thresholds().isEmpty()) {
                fallback = rules.get(i).model();
                rules = rules.subList(i + 1, rules.size());
                break;
            }
        }
        if (rules.isEmpty()) return fallback;
        String property = properties.get(index);
        SortedSet<java.math.BigDecimal> thresholds = new TreeSet<>();
        for (LegacyRule rule : rules) {
            java.math.BigDecimal threshold = rule.thresholds().get(property);
            if (threshold != null) thresholds.add(threshold);
        }
        if (thresholds.isEmpty()) return legacySelectors(rules, properties, index + 1, fallback);
        JsonObject unmatched = legacySelectors(legacyBranch(rules, property, null), properties, index + 1, fallback);
        JsonObject node = new JsonObject();
        node.addProperty("property", property);
        if (legacyCondition(property)) {
            JsonObject matched = legacySelectors(legacyBranch(rules, property, java.math.BigDecimal.ONE), properties, index + 1, fallback);
            if (matched.equals(unmatched)) return unmatched;
            node.addProperty("type", "minecraft:condition");
            node.add("on_true", matched);
            node.add("on_false", unmatched);
        } else {
            JsonArray entries = new JsonArray();
            JsonObject previous = unmatched;
            for (java.math.BigDecimal threshold : thresholds) {
                JsonObject matched = legacySelectors(legacyBranch(rules, property, threshold), properties, index + 1, fallback);
                if (matched.equals(previous)) continue;
                JsonObject entry = new JsonObject();
                entry.addProperty("threshold", threshold);
                entry.add("model", matched);
                entries.add(entry);
                previous = matched;
            }
            if (entries.isEmpty()) return unmatched;
            node.addProperty("type", "minecraft:range_dispatch");
            if (property.equals("minecraft:custom_model_data")) node.addProperty("index", 0);
            if (property.equals("minecraft:use_duration")) node.addProperty("scale", 0.05);
            node.add("entries", entries);
            node.add("fallback", unmatched);
        }
        return node;
    }

    private static List<LegacyRule> legacyBranch(List<LegacyRule> rules, String property, java.math.BigDecimal value) {
        List<LegacyRule> branch = new ArrayList<>();
        for (LegacyRule rule : rules) {
            java.math.BigDecimal threshold = rule.thresholds().get(property);
            if (threshold == null) branch.add(rule);
            else if (value != null && value.compareTo(threshold) >= 0) {
                Map<String, java.math.BigDecimal> remaining = new LinkedHashMap<>(rule.thresholds());
                remaining.remove(property);
                branch.add(new LegacyRule(remaining, rule.model()));
            }
        }
        return branch;
    }

    @Deprecated(forRemoval = false)
    public static JsonObject mergeDefinition(JsonObject higher, JsonObject lower) {
        return mergeDefinition(higher, lower, null);
    }

    public static JsonObject mergeDefinition(JsonObject higher, JsonObject lower, String resourcePath) {
        try {
            JsonObject result = lower.deepCopy();
            higher.entrySet().forEach(e -> result.add(e.getKey(), e.getValue().deepCopy()));
            if (higher.has("model") && lower.has("model")) {
                if (!higher.get("model").isJsonObject() || !lower.get("model").isJsonObject()) {
                    Logs.logWarning("Cannot merge malformed duplicate modern item definitions" + pathContext(resourcePath) + ". Keeping the higher priority definition.");
                    return higher.deepCopy();
                }
                result.add("model", mergeModel(higher.getAsJsonObject("model"), lower.getAsJsonObject("model"), resourcePath));
            }
            return result;
        } catch (RuntimeException exception) {
            Logs.logWarning("Cannot merge malformed duplicate modern item definitions" + pathContext(resourcePath) + ". Keeping the higher priority definition.");
            return higher.deepCopy();
        }
    }

    private static boolean selector(JsonObject model) {
        String type = model.has("type") ? model.get("type").getAsString().replace("minecraft:", "") : "";
        return (type.equals("select") || type.equals("range_dispatch")) && model.has("property")
                && model.get("property").getAsString().replace("minecraft:", "").equals("custom_model_data");
    }

    private static JsonObject settings(JsonObject model) {
        JsonObject result = model.deepCopy();
        result.remove("entries");
        result.remove("cases");
        result.remove("fallback");
        if (!result.has("index")) result.addProperty("index", 0);
        return result;
    }

    public static JsonObject mergeModel(JsonObject higher, JsonObject lower) {
        return mergeModel(higher, lower, null);
    }

    private static JsonObject mergeModel(JsonObject higher, JsonObject lower, String resourcePath) {
        JsonObject result = higher.deepCopy();
        if (!selector(higher)) {
            if (higher.has("entries") || higher.has("cases")) {
                String key = higher.has("entries") ? "entries" : "cases";
                JsonArray branches = new JsonArray();
                for (JsonElement element : higher.getAsJsonArray(key)) {
                    JsonObject branch = element.getAsJsonObject().deepCopy();
                    if (branch.has("model")) branch.add("model", mergeModel(branch.getAsJsonObject("model"), lower, resourcePath));
                    branches.add(branch);
                }
                result.add(key, branches);
                if (higher.has("fallback")) result.add("fallback", mergeModel(higher.getAsJsonObject("fallback"), lower, resourcePath));
                return result;
            }
            if (higher.has("on_true") && higher.has("on_false")) {
                result.add("on_true", mergeModel(higher.getAsJsonObject("on_true"), lower, resourcePath));
                result.add("on_false", mergeModel(higher.getAsJsonObject("on_false"), lower, resourcePath));
                return result;
            }
            if (higher.has("models")) {
                if (lower.has("models") && higher.getAsJsonArray("models").size() == lower.getAsJsonArray("models").size()) {
                    JsonArray parts = new JsonArray();
                    for (int i = 0; i < higher.getAsJsonArray("models").size(); i++)
                        parts.add(mergeModel(higher.getAsJsonArray("models").get(i).getAsJsonObject(), lower.getAsJsonArray("models").get(i).getAsJsonObject(), resourcePath));
                    result.add("models", parts);
                } else if (selector(lower) && hasCompatibleSelector(higher, lower)) {
                    result = mergeNestedSelector(higher, lower, resourcePath);
                } else Logs.logWarning("Cannot combine imported item rules with this composite model"
                        + (resourcePath == null ? "" : " at " + resourcePath) + ". Keeping the higher priority composite model.");
                return result;
            }
            return selector(lower) ? withFallback(lower, higher) : result;
        }
        if (selector(lower) && settings(higher).equals(settings(lower))) {
            boolean range = higher.has("entries");
            String key = range ? "entries" : "cases";
            List<JsonObject> entries = new ArrayList<>();
            Set<JsonElement> seen = new HashSet<>();
            for (JsonObject source : List.of(higher, lower)) {
                if (!source.has(key)) continue;
                for (JsonElement element : source.getAsJsonArray(key)) {
                    JsonObject entry = element.getAsJsonObject().deepCopy();
                    if (range) {
                        if (seen.add(new JsonPrimitive(entry.get("threshold").getAsBigDecimal().stripTrailingZeros()))) entries.add(entry);
                    } else {
                        JsonElement when = entry.get("when");
                        JsonArray values = new JsonArray();
                        if (when.isJsonArray()) values = when.getAsJsonArray();
                        else values.add(when);
                        JsonArray remaining = new JsonArray();
                        for (JsonElement value : values) if (seen.add(value)) remaining.add(value.deepCopy());
                        if (!remaining.isEmpty()) {
                            entry.add("when", remaining.size() == 1 ? remaining.get(0) : remaining);
                            entries.add(entry);
                        }
                    }
                }
            }
            if (range) entries.sort(Comparator.comparing(e -> e.get("threshold").getAsBigDecimal()));
            JsonArray merged = new JsonArray();
            entries.forEach(merged::add);
            result.add(key, merged);
            if (higher.has("fallback") && lower.has("fallback"))
                result.add("fallback", mergeModel(higher.getAsJsonObject("fallback"), lower.getAsJsonObject("fallback"), resourcePath));
            else if (lower.has("fallback")) result.add("fallback", lower.get("fallback").deepCopy());
        } else {
            JsonObject fallback = higher.has("fallback") ? higher.getAsJsonObject("fallback") : null;
            result.add("fallback", fallback != null ? mergeModel(fallback, lower, resourcePath) : lower.deepCopy());
        }
        return result;
    }

    private static boolean hasCompatibleSelector(JsonElement tree, JsonObject lower) {
        if (tree.isJsonObject()) {
            JsonObject object = tree.getAsJsonObject();
            if (selector(object) && settings(object).equals(settings(lower))) return true;
            for (Map.Entry<String, JsonElement> entry : object.entrySet())
                if (hasCompatibleSelector(entry.getValue(), lower)) return true;
        } else if (tree.isJsonArray()) {
            for (JsonElement element : tree.getAsJsonArray()) if (hasCompatibleSelector(element, lower)) return true;
        }
        return false;
    }

    private static JsonObject mergeNestedSelector(JsonObject tree, JsonObject lower, String resourcePath) {
        if (selector(tree) && settings(tree).equals(settings(lower))) return mergeModel(tree, lower, resourcePath);
        JsonObject result = tree.deepCopy();
        for (Map.Entry<String, JsonElement> entry : tree.entrySet()) {
            if (entry.getValue().isJsonObject()) result.add(entry.getKey(), mergeNestedSelector(entry.getValue().getAsJsonObject(), lower, resourcePath));
            else if (entry.getValue().isJsonArray()) {
                JsonArray array = new JsonArray();
                for (JsonElement value : entry.getValue().getAsJsonArray())
                    array.add(value.isJsonObject() ? mergeNestedSelector(value.getAsJsonObject(), lower, resourcePath) : value.deepCopy());
                result.add(entry.getKey(), array);
            }
        }
        return result;
    }

    private static JsonObject withFallback(JsonObject model, JsonObject fallback) {
        JsonObject result = model.deepCopy();
        if (selector(model)) {
            result.add("fallback", model.has("fallback") && selector(model.getAsJsonObject("fallback"))
                    ? withFallback(model.getAsJsonObject("fallback"), fallback) : fallback.deepCopy());
        }
        return result;
    }

    @Deprecated(forRemoval = false)
    public static JsonObject mergeLegacy(JsonObject higher, JsonObject lower) {
        return mergeLegacy(higher, lower, null);
    }

    public static JsonObject mergeLegacy(JsonObject higher, JsonObject lower, String resourcePath) {
        try {
            JsonObject result = lower.deepCopy();
            higher.entrySet().forEach(e -> result.add(e.getKey(), e.getValue().deepCopy()));
            if (lower.has("textures") && higher.has("textures")) {
                JsonObject textures = lower.getAsJsonObject("textures").deepCopy();
                higher.getAsJsonObject("textures").entrySet().forEach(e -> textures.add(e.getKey(), e.getValue().deepCopy()));
                result.add("textures", textures);
            }
            List<JsonObject> overrides = new ArrayList<>();
            for (JsonObject source : List.of(lower, higher)) if (source.has("overrides"))
                for (JsonElement entry : source.getAsJsonArray("overrides"))
                    if (entry.isJsonObject() && entry.getAsJsonObject().has("predicate")) {
                        overrides.removeIf(existing -> existing.get("predicate").equals(entry.getAsJsonObject().get("predicate")));
                        overrides.add(entry.getAsJsonObject().deepCopy());
                    }
            // Sorting by CMD threshold preserves legacy last-matching behavior for
            // threshold-only rules. Compound and unconditional rules depend on their
            // source order, so keep that order intact.
            if (overrides.stream().allMatch(ItemModelMerger::hasOnlyCustomModelDataPredicate)) {
                overrides.sort(Comparator.comparing(e -> e.getAsJsonObject("predicate")
                        .get("custom_model_data").getAsBigDecimal()));
            }
            if (!overrides.isEmpty()) {
                JsonArray array = new JsonArray();
                overrides.forEach(array::add);
                result.add("overrides", array);
            }
            return result;
        } catch (RuntimeException exception) {
            Logs.logWarning("Cannot merge malformed duplicate legacy item models" + pathContext(resourcePath) + ". Keeping the higher priority model.");
            return higher.deepCopy();
        }
    }

    private static boolean hasOnlyCustomModelDataPredicate(JsonObject override) {
        JsonObject predicate = override.getAsJsonObject("predicate");
        return predicate.size() == 1 && predicate.has("custom_model_data");
    }

    private static String pathContext(String resourcePath) {
        return resourcePath == null ? " because the caller did not provide a resource path" : " at " + resourcePath;
    }
}
