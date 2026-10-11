package io.th0rgal.oraxen.utils.schema;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.mechanics.MechanicConfigProperty;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaReconcilerTest {
    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    void scalarOrObjectScaleKeepsAxisConstraintsInGeneratedSchema() {
        var scale = MechanicConfigProperty.builder("scale", MechanicConfigProperty.Type.OBJECT_OR_NUMBER)
                .nested(Map.of("x", MechanicConfigProperty.decimal("x", "X-axis scale", 1.0))).build();
        JsonObject json = SchemaGenerator.convertPropertyToJson(scale);
        assertEquals(JsonParser.parseString("[\"object\",\"number\"]"), json.get("type"));
        assertEquals("number", json.getAsJsonObject("properties").getAsJsonObject("x").get("type").getAsString());
        assertEquals(1.0, json.getAsJsonObject("properties").getAsJsonObject("x").get("default").getAsDouble());
    }

    @Test
    void appliesChangesRecursivelyWithoutMovingSurvivors() {
        JsonObject previous = parse("""
                {"properties":{"z":{"default":false},"removed":{},"a":{}},
                 "values":["C","A","removed"],"categories":{"items":["B","A"]}}
                """);
        JsonObject generated = parse("""
                {"categories":{"items":["A","C","B"]},"values":["A","B","C"],
                 "properties":{"a":null,"z":{"default":true},"new":{"type":"string"}}}
                """);
        JsonObject expected = parse("""
                {"properties":{"z":{"default":true},"a":null,"new":{"type":"string"}},
                 "values":["C","A","B"],"categories":{"items":["B","A","C"]}}
                """);
        var result = SchemaReconciler.reconcile(previous, generated);
        assertEquals(SchemaReconciler.serialize(expected), SchemaReconciler.serialize(result));
        assertEquals(SchemaReconciler.serialize(result),
                SchemaReconciler.serialize(SchemaReconciler.reconcile(result, generated)));
        assertFalse(previous.getAsJsonObject("properties").getAsJsonObject("z").get("default").getAsBoolean());
    }

    @Test
    void preservesGeneratedOrderAndDuplicatesForOtherArrays() {
        JsonObject previous = parse("{\"default\":[1,2],\"oneOf\":[{\"type\":\"string\"},{\"type\":\"number\"}]}");
        JsonObject generated = parse("{\"default\":[2,1,2],\"oneOf\":[{\"type\":\"number\"},{\"type\":\"string\"}]}");
        assertEquals(generated, SchemaReconciler.reconcile(previous, generated));
        assertEquals(generated, SchemaReconciler.reconcile(null, generated));
    }

    @Test
    void ignoresOnlyTheRootTimestamp() {
        JsonObject previous = parse("{\"generatedAt\":\"old\",\"oraxenVersion\":\"1\",\"properties\":{\"generatedAt\":1}}");
        JsonObject generated = previous.deepCopy();
        generated.addProperty("generatedAt", "new");
        assertTrue(SchemaReconciler.semanticallyEquals(previous, generated));
        generated.addProperty("oraxenVersion", "2");
        assertFalse(SchemaReconciler.semanticallyEquals(previous, generated));
        generated.addProperty("oraxenVersion", "1");
        generated.getAsJsonObject("properties").addProperty("generatedAt", 2);
        assertFalse(SchemaReconciler.semanticallyEquals(previous, generated));
    }

    @Test
    void newNestedFactoryPropertiesAreStableAcrossTraversalOrders() {
        JsonObject previous = parse("{\"type\":\"object\",\"properties\":{\"z\":{\"type\":\"integer\",\"description\":\"z\"}}}");
        String expected = null;
        for (List<String> names : List.of(List.of("z", "y", "x"), List.of("x", "z", "y"), List.of("y", "x", "z"))) {
            Map<String, MechanicConfigProperty> coordinates = new LinkedHashMap<>();
            names.forEach(name -> coordinates.put(name, MechanicConfigProperty.integer(name, name)));
            var subtree = MechanicConfigProperty.object("new", "new", coordinates);
            Map<String, MechanicConfigProperty> properties = new LinkedHashMap<>(coordinates);
            properties.put("new", subtree);
            JsonObject generated = SchemaGenerator.convertPropertyToJson(
                    MechanicConfigProperty.object("root", null, properties));
            assertEquals(List.of("new", "x", "y", "z"),
                    List.copyOf(generated.getAsJsonObject("properties").keySet()));
            assertEquals(List.of("x", "y", "z"), List.copyOf(generated.getAsJsonObject("properties")
                    .getAsJsonObject("new").getAsJsonObject("properties").keySet()));
            String result = SchemaReconciler.serialize(SchemaReconciler.reconcile(previous, generated));
            if (expected == null) expected = result;
            assertEquals(expected, result);
            assertEquals(List.of("z", "new", "x", "y"), List.copyOf(JsonParser.parseString(result)
                    .getAsJsonObject().getAsJsonObject("properties").keySet()));
        }
    }
}
