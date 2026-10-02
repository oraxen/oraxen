package io.th0rgal.oraxen.utils.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Merges a freshly generated schema with the previously published one so that the resulting
 * file is stable across generations.
 * <p>
 * The generated schema stays the source of truth about <em>which data exists</em> and
 * <em>which values are valid</em>. The previous schema is the source of truth about
 * <em>the order of the entries that survive</em>.
 * <p>
 * The result is that a generation with no real semantic change produces byte-identical output,
 * and a generation with a real change produces a minimal diff containing only that change.
 * <p>
 * This class deliberately has no Bukkit dependency so it can be unit tested without a server.
 */
public final class SchemaReconciler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Keys whose value changes on every generation and therefore must not, on their own,
     * count as a semantic change.
     */
    private static final Set<String> VOLATILE_KEYS = Set.of("generatedAt");

    /**
     * Array keys where element order is part of the schema contract: {@code oneOf} branches are
     * alternatives evaluated in a meaningful sequence. These arrays are taken from the
     * generated schema verbatim instead of being merged as a set.
     */
    private static final Set<String> ORDERED_ARRAY_KEYS = Set.of(
            "oneOf", "anyOf", "allOf", "prefixItems", "enumOrder", "order");

    private SchemaReconciler() {
    }

    /**
     * Reconciles a generated schema against the previously published one.
     *
     * @param previous the previously published schema, or {@code null} when there is none
     * @param generated the freshly generated schema, which is authoritative for data and values
     * @return the merged schema
     */
    public static JsonElement reconcile(JsonElement previous, JsonElement generated) {
        return reconcile(previous, generated, null);
    }

    private static JsonElement reconcile(JsonElement previous, JsonElement generated, String key) {
        if (generated == null || generated.isJsonNull())
            return generated == null ? null : generated.deepCopy();

        if (previous == null || previous.isJsonNull())
            return generated.deepCopy();

        if (generated.isJsonObject() && previous.isJsonObject())
            return mergeObject(previous.getAsJsonObject(), generated.getAsJsonObject());

        if (generated.isJsonArray() && previous.isJsonArray()) {
            return isOrderedArrayKey(key)
                    ? generated.deepCopy()
                    : mergeSetLikeArray(previous.getAsJsonArray(), generated.getAsJsonArray());
        }

        // Values and type changes: the generated element always wins.
        // Preserving order must never freeze an outdated value.
        return generated.deepCopy();
    }

    /**
     * Reconciles two objects: surviving keys keep the position and serialization they already
     * had, removed keys disappear without disturbing their neighbours and new keys are appended.
     */
    private static JsonObject mergeObject(JsonObject previous, JsonObject generated) {
        JsonObject result = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : previous.entrySet()) {
            String key = entry.getKey();
            if (!generated.has(key))
                continue; // removed from the current source of truth
            result.add(key, reconcile(entry.getValue(), generated.get(key), key));
        }

        for (Map.Entry<String, JsonElement> entry : generated.entrySet()) {
            if (!previous.has(entry.getKey()))
                result.add(entry.getKey(), entry.getValue().deepCopy());
        }

        return result;
    }

    /**
     * Merges an array that behaves as a set of values, such as {@code enums.*.values} or
     * {@code enums.Material.categories.*}: surviving elements keep their previous order, removed
     * elements disappear and new elements are appended without reordering the old ones.
     */
    private static JsonArray mergeSetLikeArray(JsonArray previous, JsonArray generated) {
        Set<JsonElement> generatedElements = new HashSet<>();
        for (JsonElement element : generated) {
            generatedElements.add(element);
        }

        List<JsonElement> merged = new ArrayList<>();
        Set<JsonElement> emitted = new HashSet<>();

        for (JsonElement element : previous) {
            if (generatedElements.contains(element) && emitted.add(element))
                merged.add(element.deepCopy());
        }

        for (JsonElement element : generated) {
            if (emitted.add(element))
                merged.add(element.deepCopy());
        }

        JsonArray result = new JsonArray();
        merged.forEach(result::add);
        return result;
    }

    /**
     * Reports whether an array under the given key is semantically ordered, meaning its element
     * order is part of the schema contract rather than an incidental iteration order.
     *
     * @param key the property name the array is stored under, may be {@code null}
     */
    static boolean isOrderedArrayKey(String key) {
        return key != null && ORDERED_ARRAY_KEYS.contains(key);
    }

    /**
     * Serializes a schema to its published, human-diffable form.
     */
    public static String serialize(JsonElement element) {
        return GSON.toJson(element);
    }

    /**
     * Compares two schemas ignoring volatile fields such as {@code generatedAt}.
     *
     * @return true when both schemas carry exactly the same information
     */
    public static boolean semanticallyEquals(JsonElement first, JsonElement second) {
        return stripVolatile(first).equals(stripVolatile(second));
    }

    private static JsonElement stripVolatile(JsonElement element) {
        if (element == null || element.isJsonNull())
            return element;

        if (element.isJsonObject()) {
            JsonObject result = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (VOLATILE_KEYS.contains(entry.getKey()))
                    continue;
                result.add(entry.getKey(), stripVolatile(entry.getValue()));
            }
            return result;
        }

        if (element.isJsonArray()) {
            JsonArray result = new JsonArray();
            for (JsonElement child : element.getAsJsonArray())
                result.add(stripVolatile(child));
            return result;
        }

        return element.deepCopy();
    }
}