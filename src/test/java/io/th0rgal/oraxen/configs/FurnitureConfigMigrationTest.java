package io.th0rgal.oraxen.configs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FurnitureConfigMigrationTest {

    @ParameterizedTest
    @ValueSource(strings = {"display_entity_properties", "armor_stand_properties"})
    void migratesLegacyProperties(String legacySection) {
        ConfigurationSection furniture = new YamlConfiguration().createSection("item")
                .createSection("mechanics").createSection("furniture");
        furniture.set("type", legacySection.equals("armor_stand_properties") ? "ARMOR_STAND" : "DISPLAY_ENTITY");
        furniture.set(legacySection + ".scale.y", 0.5);
        furniture.set(legacySection + ".translation.x", 1.25);
        furniture.set(legacySection + ".display_transform", "FIXED");

        assertTrue(FurnitureConfigMigration.migrate(furniture.getParent().getParent()));
        assertEquals(0.5, furniture.getDouble("properties.scale.y"));
        assertEquals(1.25, furniture.getDouble("properties.translation.x"));
        assertEquals("FIXED", furniture.getString("properties.display_transform"));
        assertFalse(furniture.contains(legacySection));
        assertFalse(FurnitureConfigMigration.migrate(furniture.getParent().getParent()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISPLAY_ENTITY", "ARMOR_STAND"})
    void preservesUnifiedPropertiesAndPrefersSelectedLegacyType(String type) {
        ConfigurationSection furniture = new YamlConfiguration().createSection("item")
                .createSection("mechanics").createSection("furniture");
        furniture.set("type", type);
        furniture.set("properties.scale.x", 3.0);
        furniture.set("display_entity_properties.scale.x", 1.0);
        furniture.set("display_entity_properties.scale.y", 1.0);
        furniture.set("display_entity_properties.display_transform", "FIXED");
        furniture.set("armor_stand_properties.scale.x", 0.5);
        furniture.set("armor_stand_properties.scale.y", 0.5);
        furniture.set("armor_stand_properties.offset.z", 0.25);

        assertTrue(FurnitureConfigMigration.migrate(furniture.getParent().getParent()));
        assertEquals(3.0, furniture.getDouble("properties.scale.x"));
        assertEquals(type.equals("ARMOR_STAND") ? 0.5 : 1.0, furniture.getDouble("properties.scale.y"));
        assertEquals(type.equals("ARMOR_STAND") ? null : "FIXED", furniture.getString("properties.display_transform"));
        assertEquals(type.equals("ARMOR_STAND") ? 0.25 : 0, furniture.getDouble("properties.offset.z"));
        assertFalse(furniture.contains("display_entity_properties"));
        assertFalse(furniture.contains("armor_stand_properties"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISPLAY_ENTITY", "ARMOR_STAND"})
    void missingScaleAxesKeepLegacyDefaults(String type) {
        ConfigurationSection furniture = new YamlConfiguration().createSection("item")
                .createSection("mechanics").createSection("furniture");
        furniture.set("type", type);
        furniture.set("display_entity_properties.scale.x", 2.0);
        furniture.set("armor_stand_properties.scale.y", 0.5);
        FurnitureConfigMigration.migrate(furniture.getParent().getParent());
        assertEquals(type.equals("ARMOR_STAND") ? 1.0 : 2.0, furniture.getDouble("properties.scale.x", 1.0));
        assertEquals(type.equals("ARMOR_STAND") ? 0.5 : 1.0, furniture.getDouble("properties.scale.y", 1.0));
    }

    @Test
    void armorStandFallsBackToDisplayOnlyWhenItsLegacySectionIsAbsent() {
        ConfigurationSection furniture = new YamlConfiguration().createSection("item")
                .createSection("mechanics").createSection("furniture");
        furniture.set("type", "ARMOR_STAND");
        furniture.set("display_entity_properties.scale.x", 2.0);
        FurnitureConfigMigration.migrate(furniture.getParent().getParent());
        assertEquals(2.0, furniture.getDouble("properties.scale.x"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ARMOR_STAND", "DISPLAY_ENTITY"})
    void inheritedTypeSelectsLegacyPropertiesAndExplicitTypeOverridesIt(String inheritedType) {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        ConfigurationSection furniture = item.createSection("mechanics").createSection("furniture");
        furniture.set("armor_stand_properties.scale", 0.5);
        furniture.set("display_entity_properties.scale.x", 2.0);
        FurnitureConfigMigration.migrate(item, inheritedType);
        if (inheritedType.equals("ARMOR_STAND")) assertEquals(0.5, furniture.getDouble("properties.scale"));
        else assertEquals(2.0, furniture.getDouble("properties.scale.x"));
        furniture.set("properties", null);
        furniture.set("type", "DISPLAY_ENTITY");
        furniture.set("armor_stand_properties.scale", 0.5);
        furniture.set("display_entity_properties.scale.x", 2.0);
        FurnitureConfigMigration.migrate(item, inheritedType);
        assertEquals(2.0, furniture.getDouble("properties.scale.x"));
    }

    @Test
    void migratesLegacyFurnitureOptions() {
        ConfigurationSection furniture = new YamlConfiguration().createSection("item")
                .createSection("mechanics").createSection("furniture");
        furniture.createSection("hitbox").set("width", 1.5);
        furniture.getConfigurationSection("hitbox").set("height", 2);
        furniture.createSection("seat").set("height", 1.25);
        furniture.getConfigurationSection("seat").set("yaw", 90);
        furniture.set("light", 12);

        assertTrue(FurnitureConfigMigration.migrate(furniture.getParent().getParent()));
        assertEquals(List.of("0,0,0 1.5,2.0"), furniture.getStringList("hitboxes"));
        assertEquals(List.of("0,0.25,0 90.0"), furniture.getStringList("seats"));
        assertEquals(List.of("0,0,0 12"), furniture.getStringList("lights"));
        assertFalse(furniture.contains("hitbox"));
        assertFalse(furniture.contains("seat"));
        assertFalse(furniture.contains("light"));
        assertFalse(FurnitureConfigMigration.migrate(furniture.getParent().getParent()));
    }

    @Test
    void preservesExplicitPluralOptions() {
        ConfigurationSection furniture = new YamlConfiguration().createSection("item")
                .createSection("mechanics").createSection("furniture");
        furniture.createSection("hitbox");
        furniture.createSection("seat");
        furniture.set("light", 4);
        furniture.set("hitboxes", List.of("1,0,0 2,2"));
        furniture.set("seats", List.of("1,0,0"));
        furniture.set("lights", List.of("1,0,0 8"));

        assertTrue(FurnitureConfigMigration.migrate(furniture.getParent().getParent()));
        assertEquals(List.of("1,0,0 2,2"), furniture.getStringList("hitboxes"));
        assertEquals(List.of("1,0,0"), furniture.getStringList("seats"));
        assertEquals(List.of("1,0,0 8"), furniture.getStringList("lights"));
    }

    @Test
    void lightsEveryLegacyBarrier() {
        ConfigurationSection furniture = new YamlConfiguration().createSection("item")
                .createSection("mechanics").createSection("furniture");
        furniture.set("light", 10);
        furniture.set("barriers", List.of("origin", Map.of("x", 1, "y", 0, "z", 0), Map.of("x", 0, "y", 1, "z", 0)));

        assertTrue(FurnitureConfigMigration.migrate(furniture.getParent().getParent()));
        assertEquals(List.of("0,0,0 10", "1,0,0 10", "0,1,0 10"), furniture.getStringList("lights"));
        assertEquals(List.of("0,0,0", "1,0,0", "0,1,0"), furniture.getStringList("barriers"));
        assertFalse(FurnitureConfigMigration.migrate(furniture.getParent().getParent()));
    }

    @Test
    void singularBarrierAddsOriginAlongsideExistingBarriers() {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        ConfigurationSection furniture = item.createSection("mechanics").createSection("furniture");
        furniture.set("barrier", true);
        furniture.set("barriers", List.of(Map.of("x", 2)));
        furniture.set("properties.scale.x", 1.5);

        assertTrue(FurnitureConfigMigration.migrate(item));
        assertFalse(furniture.contains("barrier"));
        assertEquals(List.of("0,0,0", "2,0,0"), furniture.getStringList("barriers"));
        assertEquals(1.5, furniture.getDouble("properties.scale.x"));
        assertFalse(FurnitureConfigMigration.migrate(item));
    }

    @Test
    void falseSingularBarrierDoesNotAddOrigin() {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        ConfigurationSection furniture = item.createSection("mechanics").createSection("furniture");
        furniture.set("barrier", false);
        furniture.set("barriers", List.of("1,2,3"));

        assertTrue(FurnitureConfigMigration.migrate(item));
        assertFalse(furniture.contains("barrier"));
        assertEquals(List.of("1,2,3"), furniture.getStringList("barriers"));
        assertFalse(FurnitureConfigMigration.migrate(item));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void migratesStandaloneSingularBarrier(boolean barrier) {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        ConfigurationSection furniture = item.createSection("mechanics").createSection("furniture");
        furniture.set("barrier", barrier);

        assertTrue(FurnitureConfigMigration.migrate(item));
        assertFalse(furniture.contains("barrier"));
        assertEquals(barrier ? List.of("0,0,0") : List.of(), furniture.getStringList("barriers"));
        assertFalse(FurnitureConfigMigration.migrate(item));
    }

    @Test
    void migratesMixedBarrierFormatsAndMissingCoordinates() {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        ConfigurationSection furniture = item.createSection("mechanics").createSection("furniture");
        furniture.set("barriers", List.of("1,2,3", Map.of("y", -2), "origin", Map.of("z", 4), Map.of()));
        furniture.set("light", 11);

        assertTrue(FurnitureConfigMigration.migrate(item));
        assertEquals(List.of("1,2,3", "0,-2,0", "0,0,0", "0,0,4", "0,0,0"),
                furniture.getStringList("barriers"));
        assertEquals(List.of("0,0,0 11", "1,2,3 11", "0,-2,0 11", "0,0,4 11"),
                furniture.getStringList("lights"));
        assertFalse(furniture.contains("light"));
        assertFalse(FurnitureConfigMigration.migrate(item));
    }

    @Test
    void currentBarrierStringsNeedNoMigration() {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        ConfigurationSection furniture = item.createSection("mechanics").createSection("furniture");
        furniture.set("barriers", List.of("1,2,3", "0,0,0"));

        assertFalse(FurnitureConfigMigration.migrate(item));
        assertEquals(List.of("1,2,3", "0,0,0"), furniture.getStringList("barriers"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void batchMigrationCombinesTemplateSingularWithChildBarriersRegardlessOfOrder(boolean templateFirst) {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.set("mechanics.furniture.barrier", true);
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("mechanics.furniture.barriers", List.of(Map.of("x", 1)));
        List<ConfigurationSection> items = templateFirst ? List.of(template, child) : List.of(child, template);

        assertEquals(Set.of(template, child), FurnitureConfigMigration.migrateBarriers(items));
        assertEquals(List.of("0,0,0", "1,0,0"), child.getStringList("mechanics.furniture.barriers"));
        assertFalse(child.contains("mechanics.furniture.barrier"));
        assertTrue(FurnitureConfigMigration.migrateBarriers(items).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void batchMigrationPreservesTemplateListWhenChildOverridesSingularBarrier(boolean barrier) {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.set("mechanics.furniture.barriers", List.of(Map.of("x", 1)));
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("mechanics.furniture.barrier", barrier);
        List<ConfigurationSection> items = List.of(child, template);

        assertEquals(Set.of(template, child), FurnitureConfigMigration.migrateBarriers(items));
        assertEquals(barrier ? List.of("0,0,0", "1,0,0") : List.of("1,0,0"),
                child.getStringList("mechanics.furniture.barriers"));
        assertFalse(child.contains("mechanics.furniture.barrier"));
        assertTrue(FurnitureConfigMigration.migrateBarriers(items).isEmpty());
    }

    @Test
    void batchMigrationChildFalseDisablesTemplateOriginWhilePreservingItsList() {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.set("mechanics.furniture.barrier", true);
        template.set("mechanics.furniture.barriers", List.of(Map.of("x", 1)));
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("mechanics.furniture.barrier", false);
        List<ConfigurationSection> items = List.of(template, child);

        assertEquals(Set.of(template, child), FurnitureConfigMigration.migrateBarriers(items));
        assertEquals(List.of("0,0,0", "1,0,0"), template.getStringList("mechanics.furniture.barriers"));
        assertEquals(List.of("1,0,0"), child.getStringList("mechanics.furniture.barriers"));
        assertTrue(FurnitureConfigMigration.migrateBarriers(items).isEmpty());
    }

    @Test
    void batchMigrationLeavesFullyInheritedChildSparse() {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.set("mechanics.furniture.barrier", true);
        template.set("mechanics.furniture.barriers", List.of(Map.of("x", 1)));
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("mechanics.furniture.properties.scale.x", 2);
        List<ConfigurationSection> items = List.of(child, template);

        assertEquals(Set.of(template), FurnitureConfigMigration.migrateBarriers(items));
        assertEquals(List.of("0,0,0", "1,0,0"), template.getStringList("mechanics.furniture.barriers"));
        assertFalse(child.contains("mechanics.furniture.barrier"));
        assertFalse(child.contains("mechanics.furniture.barriers"));
        assertEquals(2, child.getInt("mechanics.furniture.properties.scale.x"));
        assertTrue(FurnitureConfigMigration.migrateBarriers(items).isEmpty());
    }

    @Test
    void batchMigrationNormalizesLegacySectionsBeforeResolvingTemplateBarriers() {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.set("mechanics.furniture.properties.scale.x", 2);
        template.set("Mechanics.furniture.barrier", true);
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("Mechanics.furniture.barriers", List.of(Map.of("x", 1)));

        assertEquals(Set.of(template, child), FurnitureConfigMigration.migrateBarriers(List.of(child, template)));
        assertEquals(List.of("0,0,0", "1,0,0"), child.getStringList("mechanics.furniture.barriers"));
        assertEquals(2, template.getInt("mechanics.furniture.properties.scale.x"));
        assertFalse(template.contains("Mechanics"));
        assertFalse(child.contains("Mechanics"));
        assertTrue(FurnitureConfigMigration.migrateBarriers(List.of(child, template)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"mechanics.Furniture", "Mechanics.FURNITURE"})
    void migratesLegacyBarriersInDifferentlyCapitalizedSections(String path) {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        ConfigurationSection furniture = item.createSection(path);
        furniture.set("barrier", true);
        furniture.set("barriers", List.of("origin", Map.of("x", 2)));
        furniture.set("light", 11);

        assertTrue(FurnitureConfigMigration.migrate(item));
        assertEquals(List.of("0,0,0", "0,0,0", "2,0,0"), furniture.getStringList("barriers"));
        assertFalse(furniture.contains("barrier"));
        assertEquals(List.of("0,0,0 11", "2,0,0 11"), furniture.getStringList("lights"));
        assertFalse(FurnitureConfigMigration.migrate(item));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Furniture", "FURNITURE"})
    void batchMigrationPreservesTemplateBarriersInDifferentlyCapitalizedSections(String section) {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.set("Mechanics." + section + ".barrier", true);
        template.set("Mechanics." + section + ".barriers", List.of("origin", Map.of("x", 2)));
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("mechanics." + section + ".barrier", false);
        List<ConfigurationSection> items = List.of(child, template);

        assertEquals(Set.of(template, child), FurnitureConfigMigration.migrateBarriers(items));
        assertEquals(List.of("0,0,0", "0,0,0", "2,0,0"),
                template.getStringList("mechanics." + section + ".barriers"));
        assertEquals(List.of("0,0,0", "2,0,0"), child.getStringList("mechanics." + section + ".barriers"));
        assertFalse(template.contains("mechanics." + section + ".barrier"));
        assertFalse(child.contains("mechanics." + section + ".barrier"));
        assertTrue(FurnitureConfigMigration.migrateBarriers(items).isEmpty());
    }

    @Test
    void dropsInvalidLegacyHitbox() {
        ConfigurationSection furniture = new YamlConfiguration().createSection("item")
                .createSection("mechanics").createSection("furniture");
        furniture.createSection("hitbox").set("width", 0);

        assertTrue(FurnitureConfigMigration.migrate(furniture.getParent().getParent()));
        assertTrue(furniture.isList("hitboxes"));
        assertTrue(furniture.getList("hitboxes").isEmpty());
    }
}
