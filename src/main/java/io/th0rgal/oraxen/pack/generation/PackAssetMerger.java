package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Resource-specific composition in ascending pack priority. */
public final class PackAssetMerger {
    private static final Pattern ADDITIVE_PATH = Pattern.compile(
            "assets/[^/]+/(?:(lang|font|atlases|blockstates|equipment)/.+|sounds)\\.json");
    private static final Pattern LEGACY_EQUIPMENT_PATH = Pattern.compile("assets/[^/]+/models/equipment/.+\\.json");
    private static final Pattern EQUIPMENT_TEXTURE = Pattern.compile("(?:[a-z0-9_.-]*:)?[a-z0-9_./-]+");
    private static final Set<String> LEGACY_EQUIPMENT_LAYER_TYPES = Set.of(
            "humanoid", "humanoid_leggings", "wings", "wolf_body", "horse_body", "llama_body");
    private static final Set<String> modelFields = Set.of(
            "parent", "elements", "textures", "display", "gui_light", "ambientocclusion", "overrides");

    private PackAssetMerger() {}

    static String additiveType(String path) {
        int start = assetStart(path);
        var match = ADDITIVE_PATH.matcher(path).region(start, path.length());
        if (!match.matches()) return "";
        return match.group(1) == null ? "sounds" : match.group(1);
    }

    private static int assetStart(String path) {
        return path.startsWith("assets/") ? 0 : path.indexOf("/assets/") + 1;
    }

    static boolean isLegacyEquipmentPath(String path) {
        return LEGACY_EQUIPMENT_PATH.matcher(path).region(assetStart(path), path.length()).matches();
    }

    /**
     * The legacy equipment directory can also contain ordinary models with custom {@code layers}
     * metadata. Direct texture IDs identify armor even when invalid. Explicit model texture
     * mappings distinguish custom non-string values and texture references from malformed armor.
     */
    static boolean isLegacyEquipment(JsonObject value) {
        if (value == null || !value.has("layers")) return false;
        if (modelFields.stream().noneMatch(value::has)) return true;
        boolean hasModelTextures = value.get("textures") instanceof JsonObject textures && !textures.isEmpty();
        return hasEquipmentLayers(value) || hasLegacyEquipmentLayer(value.get("layers"), hasModelTextures);
    }

    private static boolean hasLegacyEquipmentLayer(JsonElement value, boolean hasModelTextures) {
        if (!(value instanceof JsonObject layers)) return false;
        return LEGACY_EQUIPMENT_LAYER_TYPES.stream().map(layers::get).anyMatch(layer ->
                layer instanceof JsonArray entries && entries.asList().stream().anyMatch(entry ->
                        entry instanceof JsonObject definition && definition.has("texture")
                                && (!hasModelTextures || definition.get("texture") instanceof JsonPrimitive texture
                                && texture.isString() && !texture.getAsString().startsWith("#"))));
    }

    public static boolean isAdditivePath(String path) {
        return !additiveType(path).isEmpty();
    }

    public static JsonObject mergeJson(String path, JsonObject lower, JsonObject higher) {
        String type = additiveType(path);
        if (type.equals("lang")) {
            JsonObject result = lower.deepCopy();
            higher.entrySet().forEach(entry -> {
                if (entry.getValue().isJsonPrimitive()) result.add(entry.getKey(), entry.getValue().deepCopy());
            });
            return result;
        }
        if (type.equals("font")) return arrays(lower, higher, "providers", true);
        if (type.equals("atlases")) return arrays(lower, higher, "sources", false);
        if (type.equals("equipment") || (isLegacyEquipmentPath(path) && isLegacyEquipment(higher)))
            return equipment(lower, higher);
        if (type.equals("sounds")) {
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
                    boolean lowerReplaces = appended.has("replace") && appended.get("replace").isJsonPrimitive()
                            && appended.get("replace").getAsBoolean();
                    event.entrySet().forEach(field -> {
                        if (field.getKey().equals("sounds")) return;
                        if (field.getKey().equals("replace") && lowerReplaces
                                && field.getValue().isJsonPrimitive() && !field.getValue().getAsBoolean()) return;
                        appended.add(field.getKey(), field.getValue().deepCopy());
                    });
                    JsonArray sounds = new JsonArray();
                    if (appended.has("sounds") && appended.get("sounds").isJsonArray())
                        appended.getAsJsonArray("sounds").forEach(sounds::add);
                    if (event.has("sounds")) event.getAsJsonArray("sounds").forEach(sound -> sounds.add(sound.deepCopy()));
                    appended.add("sounds", sounds);
                    result.add(entry.getKey(), appended);
                } else {
                    result.add(entry.getKey(), event.deepCopy());
                }
            });
            return result;
        }
        if (type.equals("blockstates")) return blockstates(lower, higher);
        return higher.deepCopy();
    }

    static boolean hasEquipmentLayers(JsonObject value) {
        if (value == null || !(value.get("layers") instanceof JsonObject layers) || layers.size() == 0) return false;
        return layers.entrySet().stream().allMatch(entry -> entry.getValue() instanceof JsonArray list
                && !list.isEmpty() && list.asList().stream().allMatch(layer -> layer instanceof JsonObject definition
                && definition.get("texture") instanceof JsonPrimitive texture && texture.isString()
                && EQUIPMENT_TEXTURE.matcher(texture.getAsString()).matches()));
    }

    private static JsonObject equipment(JsonObject lower, JsonObject higher) {
        if (!hasEquipmentLayers(higher)) return lower.deepCopy();
        if (!hasEquipmentLayers(lower)) return higher.deepCopy();
        JsonObject result = lower.deepCopy();
        higher.entrySet().forEach(entry -> {
            if (!entry.getKey().equals("layers")) result.add(entry.getKey(), entry.getValue().deepCopy());
        });
        JsonObject layers = result.getAsJsonObject("layers");
        higher.getAsJsonObject("layers").entrySet().forEach(entry -> {
            if (!layers.has(entry.getKey())) layers.add(entry.getKey(), entry.getValue().deepCopy());
            else entry.getValue().getAsJsonArray().forEach(layer -> layers.getAsJsonArray(entry.getKey()).add(layer.deepCopy()));
        });
        return result;
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
        JsonArray low = lower.get(key) instanceof JsonArray array ? array : new JsonArray();
        JsonArray high = higher.get(key) instanceof JsonArray array ? array : new JsonArray();
        result.add(key, distinctEntries(low, high, highFirst));
        return result;
    }

    static JsonArray distinctEntries(JsonArray lower, JsonArray higher, boolean highFirst) {
        // Font providers are tried first to last. Atlas operations overwrite or remove earlier sprites.
        Set<JsonElement> seen = new HashSet<>();
        List<JsonElement> entries = new ArrayList<>();
        for (JsonArray source : List.of(higher, lower)) {
            for (JsonElement entry : highFirst ? source.asList() : source.asList().reversed()) {
                if (seen.add(entry)) entries.add(entry);
            }
        }
        JsonArray result = new JsonArray();
        for (JsonElement entry : highFirst ? entries : entries.reversed()) result.add(entry.deepCopy());
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
            currentExclusions.removeIf(exclusion -> disjointSelectors(selector, exclusion));
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

    private static boolean disjointSelectors(JsonObject first, JsonObject second) {
        for (var entry : first.entrySet()) {
            String property = entry.getKey();
            if (property.equals("AND") || property.equals("OR") || !second.has(property)) continue;
            JsonElement other = second.get(property);
            if (!entry.getValue().isJsonPrimitive() || !other.isJsonPrimitive()) continue;
            String value = entry.getValue().getAsString();
            String otherValue = other.getAsString();
            if (value.startsWith("!") || otherValue.startsWith("!")) continue;
            var values = Arrays.asList(value.split("\\|"));
            if (Arrays.stream(otherValue.split("\\|")).noneMatch(values::contains)) return true;
        }
        return false;
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
