package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Resource-specific composition in ascending pack priority. */
public final class PackAssetMerger {
    private static final Pattern ADDITIVE_PATH = Pattern.compile(
            "(?:.*/)?assets/[^/]+/(?:(?:lang|font|atlases|blockstates)/.+|sounds)\\.json");

    private PackAssetMerger() {}

    public static boolean isAdditivePath(String path) {
        return ADDITIVE_PATH.matcher(path).matches();
    }

    public static JsonObject mergeJson(String path, JsonObject lower, JsonObject higher) {
        if (path.contains("/lang/")) {
            JsonObject result = lower.deepCopy();
            higher.entrySet().forEach(entry -> {
                if (entry.getValue().isJsonPrimitive()) result.add(entry.getKey(), entry.getValue().deepCopy());
            });
            return result;
        }
        if (path.contains("/font/")) return arrays(lower, higher, "providers", true);
        if (path.contains("/atlases/")) return arrays(lower, higher, "sources", false);
        if (path.endsWith("/sounds.json")) {
            JsonObject result = lower.deepCopy();
            higher.entrySet().forEach(entry -> {
                JsonElement value = entry.getValue();
                if (!value.isJsonObject()) return;
                JsonObject event = value.getAsJsonObject();
                if (event.has("sounds") && !event.get("sounds").isJsonArray()) return;
                JsonElement previous = result.get(entry.getKey());
                if (previous != null && previous.isJsonObject()
                        && !(event.has("replace") && event.get("replace").isJsonPrimitive()
                        && event.get("replace").getAsBoolean())) {
                    JsonObject appended = previous.getAsJsonObject();
                    JsonArray sounds = new JsonArray();
                    if (appended.has("sounds") && appended.get("sounds").isJsonArray())
                        appended.getAsJsonArray("sounds").forEach(sounds::add);
                    if (event.has("sounds")) event.getAsJsonArray("sounds").forEach(sound -> sounds.add(sound.deepCopy()));
                    appended.add("sounds", sounds);
                } else {
                    result.add(entry.getKey(), event.deepCopy());
                }
            });
            return result;
        }
        if (path.contains("/blockstates/")) return blockstates(lower, higher);
        return higher.deepCopy();
    }

    private static JsonObject arrays(JsonObject lower, JsonObject higher, String key, boolean highFirst) {
        if (higher.has(key) && !higher.get(key).isJsonArray()) return lower.deepCopy();
        if (higher.has(key) && higher.getAsJsonArray(key).asList().stream().anyMatch(value -> !value.isJsonObject()))
            return lower.deepCopy();
        JsonObject result = new JsonObject();
        for (JsonObject source : List.of(lower, higher)) {
            source.entrySet().forEach(entry -> {
                if (!entry.getKey().equals(key)) result.add(entry.getKey(), entry.getValue().deepCopy());
            });
        }
        JsonArray values = new JsonArray();
        for (JsonObject source : highFirst ? List.of(higher, lower) : List.of(lower, higher)) {
            if (source.has(key) && source.get(key).isJsonArray())
                source.getAsJsonArray(key).forEach(value -> values.add(value.deepCopy()));
        }
        result.add(key, values);
        return result;
    }

    private static JsonObject blockstates(JsonObject lower, JsonObject higher) {
        if (higher.has("multipart") && !higher.get("multipart").isJsonArray()) return lower.deepCopy();
        if (!higher.has("multipart") && higher.has("variants") && !higher.get("variants").isJsonObject())
            return lower.deepCopy();
        if (!hasBlockstateRules(higher)) return lower.deepCopy();
        if (!hasBlockstateRules(lower)) return higher.deepCopy();

        JsonArray parts = new JsonArray();

        List<JsonObject> higherExclusions = new ArrayList<>();
        List<JsonObject> higherVariantExclusions = new ArrayList<>();
        addVariants(higher, parts, higherExclusions, higherVariantExclusions);
        higherExclusions.addAll(higherVariantExclusions);
        addMultipart(higher, parts, List.of());
        addMultipartExclusions(higher, higherExclusions);

        List<JsonObject> lowerVariantExclusions = new ArrayList<>();
        addVariants(lower, parts, higherExclusions, lowerVariantExclusions);
        addMultipart(lower, parts, higherExclusions);

        JsonObject result = new JsonObject();
        result.add("multipart", parts);
        return result;
    }

    private static boolean hasBlockstateRules(JsonObject blockstate) {
        if (blockstate.has("variants") && blockstate.get("variants").isJsonObject()) {
            for (var entry : blockstate.getAsJsonObject("variants").entrySet()) {
                if (selector(entry.getKey()) != null
                        && (entry.getValue().isJsonObject() || entry.getValue().isJsonArray())) return true;
            }
        }
        if (blockstate.has("multipart") && blockstate.get("multipart").isJsonArray()) {
            for (JsonElement value : blockstate.getAsJsonArray("multipart")) {
                if (value.isJsonObject()) return true;
            }
        }
        return false;
    }

    private static void addVariants(JsonObject blockstate, JsonArray parts, List<JsonObject> exclusions,
                                    List<JsonObject> variantExclusions) {
        if (!blockstate.has("variants") || !blockstate.get("variants").isJsonObject()) return;
        for (var entry : blockstate.getAsJsonObject("variants").entrySet()) {
            JsonObject selector = selector(entry.getKey());
            if (selector == null || !entry.getValue().isJsonObject() && !entry.getValue().isJsonArray()) continue;

            JsonArray conditions = new JsonArray();
            if (!selector.isEmpty()) conditions.add(selector);
            List<JsonObject> currentExclusions = new ArrayList<>(exclusions);
            currentExclusions.addAll(variantExclusions);
            if (addExclusions(conditions, currentExclusions)) {
                JsonObject part = new JsonObject();
                if (!conditions.isEmpty()) {
                    JsonObject when = new JsonObject();
                    when.add("AND", conditions);
                    part.add("when", when);
                }
                part.add("apply", entry.getValue().deepCopy());
                parts.add(part);
            }
            variantExclusions.add(selector);
        }
    }

    private static void addMultipart(JsonObject blockstate, JsonArray parts, List<JsonObject> exclusions) {
        if (!blockstate.has("multipart") || !blockstate.get("multipart").isJsonArray()) return;
        for (JsonElement value : blockstate.getAsJsonArray("multipart")) {
            if (!value.isJsonObject()) continue;
            JsonObject part = value.getAsJsonObject().deepCopy();
            if (!exclusions.isEmpty()) {
                JsonArray conditions = new JsonArray();
                if (part.has("when")) conditions.add(part.get("when").deepCopy());
                if (!addExclusions(conditions, exclusions)) continue;
                JsonObject when = new JsonObject();
                when.add("AND", conditions);
                part.add("when", when);
            }
            parts.add(part);
        }
    }

    private static void addMultipartExclusions(JsonObject blockstate, List<JsonObject> exclusions) {
        if (!blockstate.has("multipart") || !blockstate.get("multipart").isJsonArray()) return;
        for (JsonElement value : blockstate.getAsJsonArray("multipart")) {
            if (!value.isJsonObject()) continue;
            JsonObject part = value.getAsJsonObject();
            JsonObject condition = part.has("when") && part.get("when").isJsonObject()
                    ? part.getAsJsonObject("when") : part.has("when") ? null : new JsonObject();
            if (condition != null) exclusions.add(condition);
        }
    }

    /** Adds conditions excluding earlier rules, or returns false if an earlier rule matches every state. */
    private static boolean addExclusions(JsonArray conditions, List<JsonObject> exclusions) {
        for (JsonObject exclusion : exclusions) {
            JsonObject inverse = negateCondition(exclusion);
            if (inverse == null) return false;
            conditions.add(inverse);
        }
        return true;
    }

    /** Returns the inverse of a blockstate condition, or null when it can never match. */
    private static JsonObject negateCondition(JsonObject condition) {
        if (condition.isEmpty()) return null;

        JsonArray alternatives = new JsonArray();
        for (var entry : condition.entrySet()) {
            JsonObject inverse;
            if (entry.getKey().equals("AND") || entry.getKey().equals("OR")) {
                if (!entry.getValue().isJsonArray()) return null;
                inverse = negateGroup(entry.getKey(), entry.getValue().getAsJsonArray());
            } else {
                if (!entry.getValue().isJsonPrimitive()) return null;
                String value = entry.getValue().getAsString();
                inverse = new JsonObject();
                inverse.addProperty(entry.getKey(), value.startsWith("!") ? value.substring(1) : "!" + value);
            }
            if (inverse == null) continue;
            if (inverse.isEmpty()) return new JsonObject();
            alternatives.add(inverse);
        }
        if (alternatives.isEmpty()) return null;
        if (alternatives.size() == 1) return alternatives.get(0).getAsJsonObject();
        JsonObject inverse = new JsonObject();
        inverse.add("OR", alternatives);
        return inverse;
    }

    private static JsonObject negateGroup(String operator, JsonArray conditions) {
        JsonArray terms = new JsonArray();
        boolean and = operator.equals("AND");
        for (JsonElement value : conditions) {
            if (!value.isJsonObject()) return null;
            JsonObject inverse = negateCondition(value.getAsJsonObject());
            if (and) {
                if (inverse == null) continue;
                if (inverse.isEmpty()) return new JsonObject();
                terms.add(inverse);
            } else {
                if (inverse == null) return null;
                terms.add(inverse);
            }
        }
        if (and) {
            if (terms.isEmpty()) return null;
            if (terms.size() == 1) return terms.get(0).getAsJsonObject();
            JsonObject inverse = new JsonObject();
            inverse.add("OR", terms);
            return inverse;
        }
        if (terms.isEmpty()) return new JsonObject();
        if (terms.size() == 1) return terms.get(0).getAsJsonObject();
        JsonObject inverse = new JsonObject();
        inverse.add("AND", terms);
        return inverse;
    }

    private static JsonObject selector(String key) {
        JsonObject result = new JsonObject();
        if (key.isEmpty()) return result;
        for (String property : key.split(",")) {
            String[] pair = property.split("=", -1);
            if (pair.length != 2 || pair[0].isEmpty() || pair[1].isEmpty()) return null;
            result.addProperty(pair[0], pair[1]);
        }
        return result;
    }
}
