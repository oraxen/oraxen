package io.th0rgal.oraxen.utils.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the reconciliation rules that keep {@code schemas/oraxen-schema.json} stable:
 * surviving entries keep their position, new entries are appended, removed entries disappear,
 * real value changes are applied, and the merge is idempotent.
 */
class SchemaReconcilerTest {

    private static JsonElement parse(String json) {
        return JsonParser.parseString(json);
    }

    private static List<String> asStrings(JsonArray array) {
        return array.asList().stream().map(JsonElement::getAsString).toList();
    }

    private static List<String> keysOf(JsonObject object) {
        return List.copyOf(object.keySet());
    }

    private static JsonElement reconcile(String previous, String generated) {
        return SchemaReconciler.reconcile(parse(previous), parse(generated));
    }

    @Test
    void preservesOrderWhenNothingChanged() {
        JsonElement result = reconcile(
                "{\"properties\":{\"x\":{},\"y\":{},\"z\":{}}}",
                "{\"properties\":{\"z\":{},\"x\":{},\"y\":{}}}");

        assertEquals(List.of("x", "y", "z"), keysOf(result.getAsJsonObject().getAsJsonObject("properties")));
    }

    @Test
    void appendsNewEntryWithoutReorderingExistingOnes() {
        JsonElement result = reconcile(
                "{\"properties\":{\"x\":{},\"y\":{},\"z\":{}}}",
                "{\"properties\":{\"z\":{},\"new_property\":{},\"x\":{},\"y\":{}}}");

        assertEquals(
                List.of("x", "y", "z", "new_property"),
                keysOf(result.getAsJsonObject().getAsJsonObject("properties")));
    }

    @Test
    void removesEntryWithoutReorderingNeighbours() {
        JsonElement result = reconcile(
                "{\"properties\":{\"x\":{},\"y\":{},\"z\":{}}}",
                "{\"properties\":{\"z\":{},\"x\":{}}}");

        assertEquals(List.of("x", "z"), keysOf(result.getAsJsonObject().getAsJsonObject("properties")));
    }

    @Test
    void updatesChangedValueInPlaceWithoutReordering() {
        JsonElement result = reconcile(
                "{\"properties\":{\"a\":{\"default\":false},\"b\":{}}}",
                "{\"properties\":{\"b\":{},\"a\":{\"default\":true}}}");

        JsonObject properties = result.getAsJsonObject().getAsJsonObject("properties");
        assertEquals(List.of("a", "b"), keysOf(properties));
        assertTrue(properties.getAsJsonObject("a").get("default").getAsBoolean());
    }

    @Test
    void reconcilesNestedObjectsRecursively() {
        String previous = "{\"mechanics\":{\"furniture\":{\"properties\":{\"display_entity_properties\":"
                + "{\"properties\":{\"scale\":{},\"translation\":{},\"display_transform\":{}}}}}}}";
        String generated = "{\"mechanics\":{\"furniture\":{\"properties\":{\"display_entity_properties\":"
                + "{\"properties\":{\"display_transform\":{},\"scale\":{},\"translation\":{}}}}}}}";

        JsonElement result = reconcile(previous, generated);

        List<String> keys = keysOf(result.getAsJsonObject()
                .getAsJsonObject("mechanics")
                .getAsJsonObject("furniture")
                .getAsJsonObject("properties")
                .getAsJsonObject("display_entity_properties")
                .getAsJsonObject("properties"));
        assertEquals(List.of("scale", "translation", "display_transform"), keys);
    }

    @Test
    void reconcilesSetLikeArraysKeepingPreviousOrder() {
        JsonElement result = reconcile(
                "{\"values\":[\"A\",\"B\",\"C\"]}",
                "{\"values\":[\"C\",\"D\",\"A\",\"B\"]}");

        assertEquals(List.of("A", "B", "C", "D"),
                asStrings(result.getAsJsonObject().getAsJsonArray("values")));
    }

    @Test
    void removesValuesThatNoLongerExistFromSetLikeArrays() {
        JsonElement result = reconcile(
                "{\"values\":[\"A\",\"B\",\"C\"]}",
                "{\"values\":[\"C\",\"A\"]}");

        assertEquals(List.of("A", "C"), asStrings(result.getAsJsonObject().getAsJsonArray("values")));
    }

    @Test
    void keepsGeneratedOrderForSemanticallyOrderedArrays() {
        JsonElement result = reconcile(
                "{\"oneOf\":[{\"type\":\"integer\"},{\"type\":\"string\"}]}",
                "{\"oneOf\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}");

        JsonArray oneOf = result.getAsJsonObject().getAsJsonArray("oneOf");
        assertEquals("string", oneOf.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("integer", oneOf.get(1).getAsJsonObject().get("type").getAsString());
    }

    @Test
    void treatsMaterialCategoriesAsSetLike() {
        JsonElement result = reconcile(
                "{\"categories\":{\"items\":[\"A\",\"B\",\"C\"]}}",
                "{\"categories\":{\"items\":[\"C\",\"D\",\"A\",\"B\"]}}");

        JsonArray items = result.getAsJsonObject().getAsJsonObject("categories").getAsJsonArray("items");
        assertEquals(List.of("A", "B", "C", "D"), asStrings(items));
    }

    @Test
    void dropsEntriesMissingFromGeneratedSourceOfTruth() {
        JsonElement result = reconcile(
                "{\"mechanics\":{\"mining\":{},\"removed_mechanic\":{}}}",
                "{\"mechanics\":{\"mining\":{}}}");

        assertEquals(List.of("mining"), keysOf(result.getAsJsonObject().getAsJsonObject("mechanics")));
    }

    @Test
    void returnsGeneratedSchemaWhenThereIsNoPreviousOne() {
        JsonElement generated = parse("{\"mechanics\":{\"mining\":{}}}");

        assertEquals(generated, SchemaReconciler.reconcile(null, generated));
    }

    @Test
    void ignoresGeneratedAtWhenComparingSchemas() {
        JsonElement first = parse("{\"oraxenVersion\":\"1.0.0\",\"generatedAt\":\"2026-01-01T00:00:00Z\"}");
        JsonElement second = parse("{\"oraxenVersion\":\"1.0.0\",\"generatedAt\":\"2026-09-30T21:26:09Z\"}");

        assertTrue(SchemaReconciler.semanticallyEquals(first, second));
    }

    @Test
    void detectsRealVersionChangesDespiteVolatileTimestamp() {
        JsonElement first = parse("{\"oraxenVersion\":\"1.0.0\",\"generatedAt\":\"2026-01-01T00:00:00Z\"}");
        JsonElement second = parse("{\"oraxenVersion\":\"1.1.0\",\"generatedAt\":\"2026-01-01T00:00:00Z\"}");

        assertFalse(SchemaReconciler.semanticallyEquals(first, second));
    }

    @Test
    void isIdempotentAcrossRepeatedReconciliations() {
        String previous = "{\"values\":[\"A\",\"B\",\"C\"],\"properties\":{\"x\":{},\"y\":{}}}";
        String generated = "{\"properties\":{\"y\":{},\"x\":{},\"z\":{}},\"values\":[\"C\",\"D\",\"A\"]}";

        JsonElement once = reconcile(previous, generated);
        JsonElement twice = SchemaReconciler.reconcile(once, parse(generated));

        assertEquals(SchemaReconciler.serialize(once), SchemaReconciler.serialize(twice));
    }

    @Test
    void producesIdenticalOutputForShuffledGenerationOrder() {
        String generatedA = "{\"values\":[\"A\",\"B\",\"C\"],\"properties\":{\"x\":{},\"y\":{},\"z\":{}}}";
        String generatedB = "{\"properties\":{\"z\":{},\"x\":{},\"y\":{}},\"values\":[\"C\",\"B\",\"A\"]}";

        JsonElement previous = parse("{\"values\":[\"A\",\"B\",\"C\"],\"properties\":{\"x\":{},\"y\":{},\"z\":{}}}");

        assertEquals(
                SchemaReconciler.serialize(SchemaReconciler.reconcile(previous, parse(generatedA))),
                SchemaReconciler.serialize(SchemaReconciler.reconcile(previous, parse(generatedB))));
    }

    @Test
    void serializesToStablePrettyPrintedOutput() {
        JsonElement element = parse("{\"b\":1,\"a\":2}");

        assertEquals("{\n  \"b\": 1,\n  \"a\": 2\n}", SchemaReconciler.serialize(element));
    }

    @Test
    void dedupesRepeatedValuesInSetLikeArrays() {
        JsonElement result = reconcile(
                "{\"values\":[\"A\",\"B\"]}",
                "{\"values\":[\"A\",\"B\",\"A\"]}");

        assertEquals(List.of("A", "B"), asStrings(result.getAsJsonObject().getAsJsonArray("values")));
    }

    /**
     * Full-scale check against the committed artifact: whatever order the generator emits entries
     * in, reconciling must reproduce the published file byte for byte. This is the invariant that
     * keeps CI from committing a reordering of the whole schema.
     */
    @Test
    void reproducesTheCommittedSchemaRegardlessOfGenerationOrder() throws IOException {
        Path schemaFile = Path.of("schemas/oraxen-schema.json");
        assertTrue(Files.isRegularFile(schemaFile), "expected the committed schema at " + schemaFile);

        JsonElement published = JsonParser.parseString(Files.readString(schemaFile));

        String expected = SchemaReconciler.serialize(SchemaReconciler.reconcile(published, published));
        String fromShuffled = SchemaReconciler.serialize(
                SchemaReconciler.reconcile(published, scrambleGenerationOrder(published, null, new Random(1234L))));

        assertEquals(expected, fromShuffled);
    }

    /**
     * Simulates a generator whose iteration order is arbitrary: every object and every set-like
     * array is emitted in a different order than the published file, at every nesting level.
     * Semantically ordered arrays such as {@code oneOf} are left alone, because the generator
     * emits those in a meaningful sequence and the reconciler takes them verbatim.
     */
    private static JsonElement scrambleGenerationOrder(JsonElement element, String key, Random random) {
        if (element.isJsonObject()) {
            List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(element.getAsJsonObject().entrySet());
            Collections.shuffle(entries, random);

            JsonObject result = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : entries) {
                result.add(entry.getKey(),
                        scrambleGenerationOrder(entry.getValue(), entry.getKey(), random));
            }
            return result;
        }

        if (element.isJsonArray() && SchemaReconciler.isOrderedArrayKey(key)) {
            // Ordered arrays are emitted verbatim and deterministically by the generator,
            // so the reconciler must reproduce them unchanged.
            return element.deepCopy();
        }

        if (element.isJsonArray()) {
            List<JsonElement> children = new ArrayList<>(element.getAsJsonArray().asList());
            Collections.shuffle(children, random);

            JsonArray result = new JsonArray();
            for (JsonElement child : children)
                result.add(scrambleGenerationOrder(child, key, random));
            return result;
        }

        return element.deepCopy();
    }
}