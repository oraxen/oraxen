package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackAssetMergerTest {
    private static final String PATH = "assets/test/blockstates/test.json";

    @Test
    void higherMultipartRulesOverrideOnlyConflictingLowerMultipartRules() {
        JsonObject higher = json("""
                {"multipart":[
                  {"when":{"facing":"north"},"apply":{"model":"test:high_north"}},
                  {"when":{"AND":[{"facing":"south"},{"powered":"true"}]},"apply":{"model":"test:high_powered_south"}}
                ]}
                """);
        JsonObject lower = json("""
                {"multipart":[
                  {"when":{"facing":"north"},"apply":{"model":"test:low_north"}},
                  {"when":{"facing":"south"},"apply":{"model":"test:low_south"}},
                  {"when":{"facing":"west"},"apply":{"model":"test:low_west"}}
                ]}
                """);

        JsonObject merged = PackAssetMerger.mergeJson(PATH, lower, higher);

        assertEquals(List.of("test:high_north"), modelsFor(merged, Map.of("facing", "north", "powered", "false")));
        assertEquals(List.of("test:low_south"), modelsFor(merged, Map.of("facing", "south", "powered", "false")));
        assertEquals(List.of("test:high_powered_south"), modelsFor(merged, Map.of("facing", "south", "powered", "true")));
        assertEquals(List.of("test:low_west"), modelsFor(merged, Map.of("facing", "west", "powered", "false")));
    }

    @Test
    void booleanMultipartPropertiesPreserveNonconflictingLowerRules() {
        JsonObject higher = json("""
                {"multipart":[
                  {"when":{"powered":true},"apply":{"model":"test:high_powered"}}
                ]}
                """);
        JsonObject lower = json("""
                {"multipart":[
                  {"when":{"powered":false},"apply":{"model":"test:low_unpowered"}},
                  {"when":{"AND":[{"powered":false},{"facing":"west"}]},"apply":{"model":"test:low_west_unpowered"}}
                ]}
                """);

        JsonObject merged = PackAssetMerger.mergeJson(PATH, lower, higher);

        assertEquals(List.of("test:high_powered"), modelsFor(merged, Map.of("powered", "true", "facing", "west")));
        assertEquals(List.of("test:low_unpowered"), modelsFor(merged, Map.of("powered", "false", "facing", "east")));
        assertEquals(List.of("test:low_unpowered", "test:low_west_unpowered"),
                modelsFor(merged, Map.of("powered", "false", "facing", "west")));
    }

    @Test
    void numericMultipartPropertiesPreserveNonconflictingLowerRules() {
        JsonObject higher = json("""
                {"multipart":[
                  {"when":{"age":1},"apply":{"model":"test:high_age_one"}}
                ]}
                """);
        JsonObject lower = json("""
                {"multipart":[
                  {"when":{"age":2},"apply":{"model":"test:low_age_two"}},
                  {"when":{"AND":[{"age":2},{"facing":"west"}]},"apply":{"model":"test:low_west_age_two"}}
                ]}
                """);

        JsonObject merged = PackAssetMerger.mergeJson(PATH, lower, higher);

        assertEquals(List.of("test:high_age_one"), modelsFor(merged, Map.of("age", "1", "facing", "west")));
        assertEquals(List.of("test:low_age_two"), modelsFor(merged, Map.of("age", "2", "facing", "east")));
        assertEquals(List.of("test:low_age_two", "test:low_west_age_two"),
                modelsFor(merged, Map.of("age", "2", "facing", "west")));
    }

    @Test
    void higherMultipartConditionExcludesMatchingLowerVariants() {
        JsonObject higher = json("""
                {"multipart":[
                  {"when":{"AND":[{"facing":"north"},{"powered":"true"}]},"apply":{"model":"test:high"}}
                ]}
                """);
        JsonObject lower = json("""
                {"variants":{
                  "facing=north":{"model":"test:low_north"},
                  "facing=south":{"model":"test:low_south"}
                }}
                """);

        JsonObject merged = PackAssetMerger.mergeJson(PATH, lower, higher);

        assertEquals(List.of("test:high"), modelsFor(merged, Map.of("facing", "north", "powered", "true")));
        assertEquals(List.of("test:low_north"), modelsFor(merged, Map.of("facing", "north", "powered", "false")));
        assertEquals(List.of("test:low_south"), modelsFor(merged, Map.of("facing", "south", "powered", "true")));
    }

    @Test
    void higherVariantsStillOverrideMatchingLowerMultipartRules() {
        JsonObject higher = json("""
                {"variants":{"facing=north":{"model":"test:high_north"}}}
                """);
        JsonObject lower = json("""
                {"multipart":[
                  {"when":{"facing":"north"},"apply":{"model":"test:low_north"}},
                  {"when":{"facing":"south"},"apply":{"model":"test:low_south"}}
                ]}
                """);

        JsonObject merged = PackAssetMerger.mergeJson(PATH, lower, higher);

        assertEquals(List.of("test:high_north"), modelsFor(merged, Map.of("facing", "north")));
        assertEquals(List.of("test:low_south"), modelsFor(merged, Map.of("facing", "south")));
    }

    @Test
    void samePriorityVariantsAndMultipartRulesCanBothApply() {
        JsonObject higher = json("""
                {"multipart":[
                  {"when":{"facing":"south"},"apply":{"model":"test:high_south"}}
                ]}
                """);
        JsonObject lower = json("""
                {"variants":{"facing=north":{"model":"test:low_north"}},
                 "multipart":[
                   {"when":{"powered":"true"},"apply":{"model":"test:low_powered"}}
                 ]}
                """);

        JsonObject merged = PackAssetMerger.mergeJson(PATH, lower, higher);

        assertEquals(List.of("test:low_north", "test:low_powered"),
                modelsFor(merged, Map.of("facing", "north", "powered", "true")));
        assertEquals(List.of("test:high_south"), modelsFor(merged, Map.of("facing", "south", "powered", "true")));
    }

    @Test
    void unconditionalHigherMultipartRuleBlocksAllConflictingLowerRules() {
        JsonObject higher = json("""
                {"multipart":[{"apply":{"model":"test:high"}}]}
                """);
        JsonObject lower = json("""
                {"variants":{"facing=north":{"model":"test:low_north"}}}
                """);

        JsonObject merged = PackAssetMerger.mergeJson(PATH, lower, higher);

        assertEquals(List.of("test:high"), modelsFor(merged, Map.of("facing", "north")));
        assertEquals(List.of("test:high"), modelsFor(merged, Map.of("facing", "south")));
    }

    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    private static List<String> modelsFor(JsonObject blockstate, Map<String, String> properties) {
        List<String> matches = new ArrayList<>();
        for (JsonElement value : blockstate.getAsJsonArray("multipart")) {
            JsonObject part = value.getAsJsonObject();
            if (!part.has("when") || matches(part.getAsJsonObject("when"), properties)) {
                matches.add(part.getAsJsonObject("apply").get("model").getAsString());
            }
        }
        return matches;
    }

    private static boolean matches(JsonObject condition, Map<String, String> properties) {
        if (condition.has("AND")) {
            return condition.getAsJsonArray("AND").asList().stream()
                    .allMatch(value -> matches(value.getAsJsonObject(), properties));
        }
        if (condition.has("OR")) {
            return condition.getAsJsonArray("OR").asList().stream()
                    .anyMatch(value -> matches(value.getAsJsonObject(), properties));
        }
        return condition.entrySet().stream().allMatch(entry -> {
            String expected = entry.getValue().getAsString();
            boolean negated = expected.startsWith("!");
            String[] values = (negated ? expected.substring(1) : expected).split("\\|");
            return Arrays.asList(values).contains(properties.get(entry.getKey())) != negated;
        });
    }
}
