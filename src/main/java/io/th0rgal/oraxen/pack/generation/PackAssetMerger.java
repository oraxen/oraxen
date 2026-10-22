package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Resource-specific composition in ascending pack priority. */
public final class PackAssetMerger {
    private PackAssetMerger() {}

    public static boolean isAdditivePath(String path) {
        return path.matches("(?:.*/)?assets/[^/]+/(?:lang|font|atlases|blockstates)/.+\\.json")
                || path.matches("(?:.*/)?assets/[^/]+/sounds\\.json");
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
                    JsonObject appended = previous.getAsJsonObject().deepCopy();
                    JsonArray sounds = new JsonArray();
                    if (appended.has("sounds") && appended.get("sounds").isJsonArray())
                        appended.getAsJsonArray("sounds").forEach(sound -> sounds.add(sound.deepCopy()));
                    if (event.has("sounds")) event.getAsJsonArray("sounds").forEach(sound -> sounds.add(sound.deepCopy()));
                    appended.add("sounds", sounds);
                    event = appended;
                }
                result.add(entry.getKey(), event.deepCopy());
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
        JsonObject result = lower.deepCopy();
        higher.entrySet().forEach(entry -> result.add(entry.getKey(), entry.getValue().deepCopy()));
        JsonArray values = new JsonArray();
        for (JsonObject source : highFirst ? List.of(higher, lower) : List.of(lower, higher)) {
            if (source.has(key) && source.get(key).isJsonArray())
                source.getAsJsonArray(key).forEach(value -> values.add(value.deepCopy()));
        }
        result.add(key, values);
        return result;
    }

    private static JsonObject blockstates(JsonObject lower, JsonObject higher) {
        if (higher.has("multipart")) return higher.get("multipart").isJsonArray() ? higher.deepCopy() : lower.deepCopy();
        if (!higher.has("variants") || !higher.get("variants").isJsonObject()) return lower.deepCopy();
        if ((!lower.has("variants") || !lower.get("variants").isJsonObject())
                && (!lower.has("multipart") || !lower.get("multipart").isJsonArray())) return higher.deepCopy();
        JsonArray parts = new JsonArray();
        List<JsonObject> exclusions = new ArrayList<>();
        List<JsonObject> sources = new ArrayList<>();
        sources.add(higher.getAsJsonObject("variants"));
        if (lower.has("variants") && lower.get("variants").isJsonObject()) sources.add(lower.getAsJsonObject("variants"));
        for (JsonObject source : sources) {
            for (var entry : source.entrySet()) {
                JsonObject selector = selector(entry.getKey());
                if (selector == null || !entry.getValue().isJsonObject() && !entry.getValue().isJsonArray()) continue;
                JsonArray conditions = new JsonArray();
                if (!selector.isEmpty()) conditions.add(selector);
                boolean unreachable = false;
                for (JsonObject exclusion : exclusions) {
                    if (exclusion.isEmpty()) { unreachable = true; break; }
                    JsonArray alternatives = new JsonArray();
                    exclusion.entrySet().forEach(property -> {
                        JsonObject inverse = new JsonObject();
                        String value = property.getValue().getAsString();
                        inverse.addProperty(property.getKey(), value.startsWith("!") ? value.substring(1) : "!" + value);
                        alternatives.add(inverse);
                    });
                    JsonObject inverse = new JsonObject();
                    inverse.add("OR", alternatives);
                    conditions.add(inverse);
                }
                if (!unreachable) {
                    JsonObject part = new JsonObject();
                    if (!conditions.isEmpty()) {
                        JsonObject when = new JsonObject();
                        when.add("AND", conditions);
                        part.add("when", when);
                    }
                    part.add("apply", entry.getValue().deepCopy());
                    parts.add(part);
                }
                exclusions.add(selector);
            }
        }
        if (lower.has("multipart") && lower.get("multipart").isJsonArray()) {
            for (JsonElement value : lower.getAsJsonArray("multipart")) {
                if (!value.isJsonObject()) continue;
                JsonObject part = value.getAsJsonObject().deepCopy();
                JsonArray conditions = new JsonArray();
                if (part.has("when")) conditions.add(part.get("when").deepCopy());
                boolean unreachable = false;
                for (JsonObject exclusion : exclusions) {
                    if (exclusion.isEmpty()) { unreachable = true; break; }
                    JsonArray alternatives = new JsonArray();
                    exclusion.entrySet().forEach(property -> {
                        JsonObject inverse = new JsonObject();
                        String valueString = property.getValue().getAsString();
                        inverse.addProperty(property.getKey(), valueString.startsWith("!") ? valueString.substring(1) : "!" + valueString);
                        alternatives.add(inverse);
                    });
                    JsonObject inverse = new JsonObject();
                    inverse.add("OR", alternatives);
                    conditions.add(inverse);
                }
                if (!unreachable) {
                    JsonObject when = new JsonObject();
                    when.add("AND", conditions);
                    part.add("when", when);
                    parts.add(part);
                }
            }
        }
        JsonObject result = new JsonObject();
        result.add("multipart", parts);
        return result;
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
