package io.th0rgal.oraxen.recipes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecipeMigrationTest {
    @TempDir
    Path directory;

    @Test
    void migratesKnownKeysButPreservesIdsShapesSerializedStacksAndArbitraryData() throws IOException {
        String original = """
                # Admin recipe
                my_recipe_id:
                  cookingTime: 120
                  experience_cost: 2
                  level_change: -1
                  fluid: powder_snow
                  shape: [A_A, _A_, AAA]
                  input:
                    crucible_item: custom_sword
                  base:
                    mmoitems_type: LONG_SWORD
                    mmoitems_id: sword_id
                  addition:
                    ecoitem_id: crystal_id
                  template:
                    minecraft_type: NETHERITE_UPGRADE_SMITHING_TEMPLATE
                  result:
                    minecraft_item:
                      ==: org.bukkit.inventory.ItemStack
                      type: DIAMOND_SWORD
                      meta:
                        custom_data:
                          minecraft_type: untouched_value
                          cookingTime: 55
                  ingredients:
                    A:
                      oraxen_item: my_item_id
                      amount: 3
                  arbitrary_data:
                    oraxen_item: untouched_item
                    level_change: unchanged
                """;
        Path file = write("shaped.yml", original);
        assertTrue(RecipeMigration.migrate(file.toFile()));
        assertEquals(original, Files.readString(directory.resolve("shaped.yml.bak")));
        Map<String, Object> before = recipe(original);
        Map<String, Object> after = recipe(Files.readString(file));
        assertEquals(120, after.get("cooking-time"));
        assertEquals(2, after.get("experience-cost"));
        assertEquals(-1, after.get("level-change"));
        assertFalse(after.containsKey("cookingTime"));
        assertEquals("powder_snow", after.get("fluid"));
        assertEquals(before.get("shape"), after.get("shape"));
        assertEquals(before.get("arbitrary_data"), after.get("arbitrary_data"));
        assertEquals(section(before, "result").get("minecraft_item"), section(after, "result").get("minecraft-item"));
        assertEquals("custom_sword", section(after, "input").get("crucible-item"));
        assertEquals(Map.of("mmoitems-type", "LONG_SWORD", "mmoitems-id", "sword_id"), after.get("base"));
        assertEquals(Map.of("ecoitem-id", "crystal_id"), after.get("addition"));
        assertEquals(Map.of("minecraft-type", "NETHERITE_UPGRADE_SMITHING_TEMPLATE"), after.get("template"));
        assertEquals(Map.of("A", Map.of("oraxen-item", "my_item_id", "amount", 3)), after.get("ingredients"));
        assertTrue(Files.readString(file).contains("# Admin recipe"));
        String migrated = Files.readString(file);
        assertFalse(RecipeMigration.migrate(file.toFile()));
        assertEquals(migrated, Files.readString(file));
        assertFalse(Files.exists(directory.resolve("shaped.yml.bak.1")));
    }

    @Test
    void migratesShapelessListsWithoutChangingIngredientAmountsOrProviderData() throws IOException {
        Path file = write("shapeless.yml", """
                my_recipe_id:
                  result:
                    oraxen_item: result_id
                  ingredients:
                    - oraxen_item: first_id
                      amount: 4
                    - minecraft_type: IRON_INGOT
                      amount: 2
                    - arbitrary_data:
                        minecraft_type: do_not_change
                """);
        assertTrue(RecipeMigration.migrate(file.toFile()));
        assertEquals(List.of(Map.of("oraxen-item", "first_id", "amount", 4),
                Map.of("minecraft-type", "IRON_INGOT", "amount", 2),
                Map.of("arbitrary_data", Map.of("minecraft_type", "do_not_change"))),
                recipe(Files.readString(file)).get("ingredients"));
    }

    @Test
    void preservesModernKeysAndRetainsConflictingLegacyData() throws IOException {
        Path file = write("furnace.yml", """
                my_recipe_id:
                  cookingTime: 100
                  cooking-time: 200
                  result:
                    oraxen_item: old_id
                    oraxen-item: new_id
                  input:
                    minecraft_type: IRON_ORE
                """);
        assertTrue(RecipeMigration.migrate(file.toFile()));
        Map<String, Object> after = recipe(Files.readString(file));
        assertEquals(100, after.get("cookingTime"));
        assertEquals(200, after.get("cooking-time"));
        assertEquals(Map.of("oraxen_item", "old_id", "oraxen-item", "new_id"), after.get("result"));
        assertFalse(RecipeMigration.migrate(file.toFile()));
    }

    @Test
    void neverOverwritesAnEarlierBackup() throws IOException {
        Path file = write("anvil.yml", "my_recipe_id:\n  experience_cost: 3\n");
        write("anvil.yml.bak", "earlier backup");
        byte[] original = Files.readAllBytes(file);
        assertTrue(RecipeMigration.migrate(file.toFile()));
        assertEquals("earlier backup", Files.readString(directory.resolve("anvil.yml.bak")));
        assertArrayEquals(original, Files.readAllBytes(directory.resolve("anvil.yml.bak.1")));
        try (var files = Files.list(directory)) {
            assertEquals(3, files.count());
        }
    }

    @Test
    void malformedYamlFailsWithoutWritingOrBackingUp() throws IOException {
        Path file = write("broken.yml", "my_recipe_id:\n  result: [unclosed\n");
        byte[] original = Files.readAllBytes(file);
        assertThrows(IOException.class, () -> RecipeMigration.migrate(file.toFile()));
        assertArrayEquals(original, Files.readAllBytes(file));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void sharedYamlAliasesDoNotRenameKeysInsideArbitraryData() throws IOException {
        Path file = write("aliases.yml", """
                my_recipe_id:
                  input: &shared_item
                    oraxen_item: shared_id
                  result:
                    minecraft_item:
                      custom_data: *shared_item
                  arbitrary_data: *shared_item
                """);
        assertTrue(RecipeMigration.migrate(file.toFile()));
        Map<String, Object> after = recipe(Files.readString(file));
        assertEquals(Map.of("oraxen-item", "shared_id"), after.get("input"));
        assertEquals(Map.of("oraxen_item", "shared_id"), after.get("arbitrary_data"));
        assertEquals(Map.of("custom_data", Map.of("oraxen_item", "shared_id")),
                section(after, "result").get("minecraft-item"));
    }

    @Test
    void duplicateRecipeKeysFailWithoutAnyOverwrite() throws IOException {
        String original = "my_recipe_id:\n  cookingTime: 100\n  cookingTime: 200\n";
        Path file = write("duplicates.yml", original);
        assertThrows(IOException.class, () -> RecipeMigration.migrate(file.toFile()));
        assertEquals(original, Files.readString(file));
        assertFalse(Files.exists(directory.resolve("duplicates.yml.bak")));
    }

    @Test
    void unchangedAndEmptyFilesRemainByteIdenticalWithoutBackups() throws IOException {
        for (String contents : List.of("", "{}\n", "# Comment\nmy_recipe_id:\n  result:\n    oraxen-item: my_item_id\n")) {
            Path file = write("unchanged.yml", contents);
            assertFalse(RecipeMigration.migrate(file.toFile()));
            assertEquals(contents, Files.readString(file));
            assertFalse(Files.exists(directory.resolve("unchanged.yml.bak")));
        }
    }

    private Path write(String name, String contents) throws IOException {
        return Files.writeString(directory.resolve(name), contents);
    }

    private Map<String, Object> recipe(String contents) {
        Map<String, Object> root = new Yaml().load(contents);
        return section(root, "my_recipe_id");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> section(Map<String, Object> mapping, String key) {
        return (Map<String, Object>) mapping.get(key);
    }
}
