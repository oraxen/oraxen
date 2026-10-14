package io.th0rgal.oraxen.utils.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashSet;
import java.util.Set;

/** Preserves published entry order while taking all data and values from the generated schema. */
public final class SchemaReconciler {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> setArrayKeys = Set.of("values", "enum", "suggestions");

    private SchemaReconciler() {
    }

    public static JsonElement reconcile(JsonElement previous, JsonElement generated) {
        return reconcile(previous, generated, null, null);
    }

    private static JsonElement reconcile(JsonElement previous, JsonElement generated, String key, String parent) {
        if (previous != null && previous.isJsonObject() && generated.isJsonObject()) {
            JsonObject oldObject = previous.getAsJsonObject();
            JsonObject newObject = generated.getAsJsonObject();
            JsonObject result = new JsonObject();
            for (String name : oldObject.keySet()) {
                if (newObject.has(name))
                    result.add(name, reconcile(oldObject.get(name), newObject.get(name), name, key));
            }
            for (String name : newObject.keySet()) {
                if (!oldObject.has(name))
                    result.add(name, newObject.get(name).deepCopy());
            }
            return result;
        }
        // Only known value lists are sets. Defaults, tuples and schema alternatives retain
        // generated order and duplicates; treating arbitrary arrays as sets can change their meaning.
        if (previous != null && previous.isJsonArray() && generated.isJsonArray()
                && (setArrayKeys.contains(key == null ? "" : key) || "categories".equals(parent))) {
            Set<JsonElement> remaining = new LinkedHashSet<>(generated.getAsJsonArray().asList());
            JsonArray result = new JsonArray();
            for (JsonElement value : previous.getAsJsonArray()) {
                if (remaining.remove(value))
                    result.add(value.deepCopy());
            }
            remaining.forEach(value -> result.add(value.deepCopy()));
            return result;
        }
        return generated.deepCopy();
    }

    public static String serialize(JsonElement schema) {
        return gson.toJson(schema);
    }

    /** Only the root generation timestamp is volatile; nested property values remain meaningful. */
    public static boolean semanticallyEquals(JsonObject first, JsonObject second) {
        JsonObject oldSchema = first.deepCopy();
        JsonObject newSchema = second.deepCopy();
        oldSchema.remove("generatedAt");
        newSchema.remove("generatedAt");
        return oldSchema.equals(newSchema);
    }
}
