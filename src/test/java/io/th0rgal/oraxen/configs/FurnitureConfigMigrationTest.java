package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FurnitureConfigMigrationTest {

    @Test
    void copiesEntirePackAndKeepsAppearanceIndependent() {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        ConfigurationSection furniture = item.createSection("mechanics.furniture");
        ConfigurationSection pack = item.createSection("pack");
        pack.set("model", "furniture/chair");
        pack.set("generate_model", false);
        pack.set("custom_model_data", 1234);
        pack.set("textures", new ArrayList<>(List.of("furniture/chair")));
        pack.set("display.gui.translation", new ArrayList<>(List.of(1, 2, 3)));
        pack.set("future_options", new ArrayList<>(List.of(new LinkedHashMap<>(Map.of("nested", "original")))));
        pack.createSection("empty_option");

        assertEquals(Set.of(item), FurnitureConfigMigration.migrateAppearances(List.of(item)));
        ConfigurationSection appearance = furniture.getConfigurationSection("appearance");
        assertNotNull(appearance);
        YamlConfiguration expected = new YamlConfiguration();
        expected.createSection("pack", pack.getValues(false));
        YamlConfiguration actual = new YamlConfiguration();
        actual.createSection("pack", appearance.getValues(false));
        assertEquals(expected.saveToString(), actual.saveToString());
        assertNotSame(pack, appearance);
        assertNotSame(pack.getList("textures"), appearance.getList("textures"));
        assertNotSame(pack.getList("future_options").getFirst(), appearance.getList("future_options").getFirst());

        appearance.set("model", "furniture/new_chair");
        appearance.set("display.gui.translation", List.of(4, 5, 6));
        appearance.getList("textures").clear();
        assertEquals("furniture/chair", pack.getString("model"));
        assertEquals(List.of(1, 2, 3), pack.getList("display.gui.translation"));
        assertEquals(List.of("furniture/chair"), pack.getList("textures"));
        pack.set("model", "inventory/chair");
        assertEquals("furniture/new_chair", appearance.getString("model"));
        assertTrue(FurnitureConfigMigration.migrateAppearances(List.of(item)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void explicitAppearanceIsAuthoritativeIncludingEmptySections(boolean empty) {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        item.set("pack.model", "inventory/chair");
        item.set("pack.generate_model", true);
        item.set("pack.textures", List.of("inventory/chair"));
        ConfigurationSection appearance = item.createSection("mechanics.furniture.appearance");
        if (!empty) appearance.set("model", "furniture/chair");

        assertTrue(FurnitureConfigMigration.migrateAppearances(List.of(item)).isEmpty());
        assertEquals(empty ? Set.of() : Set.of("model"), appearance.getKeys(false));
        if (!empty) assertEquals("furniture/chair", appearance.getString("model"));
        assertTrue(FurnitureConfigMigration.migrateAppearances(List.of(item)).isEmpty());
    }

    @Test
    void preservesAlternativeFurnitureItemAndNonFurniturePack() {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        item.set("pack.model", "inventory/chair");
        assertTrue(FurnitureConfigMigration.migrateAppearances(List.of(item)).isEmpty());
        assertFalse(item.contains("mechanics"));

        item.set("mechanics.furniture.item", "display_chair");
        assertTrue(FurnitureConfigMigration.migrateAppearances(List.of(item)).isEmpty());
        assertFalse(item.contains("mechanics.furniture.appearance"));
        assertEquals("inventory/chair", item.getString("pack.model"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void appearanceMigrationSnapshotsInheritanceBeforeMutatingTemplates(boolean templateFirst) {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.createSection("mechanics.furniture");
        template.set("pack.model", "furniture/base");
        template.set("pack.textures", List.of("furniture/base"));
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("pack.model", "furniture/child");
        List<ConfigurationSection> items = templateFirst ? List.of(template, child) : List.of(child, template);

        assertEquals(Set.of(child), FurnitureConfigMigration.migrateAppearances(items));
        assertFalse(template.contains("mechanics.furniture.appearance"));
        assertEquals("furniture/child", child.getString("mechanics.furniture.appearance.model"));
        assertEquals(List.of("furniture/base"), child.getStringList("mechanics.furniture.appearance.textures"));
        assertFalse(child.contains("mechanics.furniture.item"));
        assertTrue(FurnitureConfigMigration.migrateAppearances(items).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"alternate", "empty", "partial"})
    void skippedChildrenKeepTheirEffectiveAppearanceWhenSiblingsMigrate(String kind) {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.set("pack.model", "furniture/base");
        template.set("pack.textures", List.of("furniture/base"));
        template.createSection("mechanics.furniture");
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        if (kind.equals("alternate")) child.set("mechanics.furniture.item", "display_other");
        else {
            child.createSection("mechanics.furniture.appearance");
            if (kind.equals("partial"))
                child.set("mechanics.furniture.appearance.model", "furniture/child");
        }
        ConfigurationSection sibling = new YamlConfiguration().createSection("sibling");
        sibling.set("template", "base");
        List<ConfigurationSection> items = List.of(template, child, sibling);
        YamlConfiguration before = new YamlConfiguration();
        OraxenYaml.copyConfigurationSection(template, before);
        OraxenYaml.copyConfigurationSection(child, before);

        assertEquals(Set.of(sibling), FurnitureConfigMigration.migrateAppearances(items));

        YamlConfiguration after = new YamlConfiguration();
        OraxenYaml.copyConfigurationSection(template, after);
        OraxenYaml.copyConfigurationSection(child, after);
        assertEquals(before.saveToString(), after.saveToString());
        assertEquals("furniture/base", sibling.getString("mechanics.furniture.appearance.model"));
        assertTrue(FurnitureConfigMigration.migrateAppearances(items).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void inheritedAppearanceOrAlternativeItemPreventsMigration(boolean appearance) {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        if (appearance) template.createSection("mechanics.furniture.appearance");
        else template.set("mechanics.furniture.item", "display_chair");
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("pack.model", "inventory/chair");

        assertTrue(FurnitureConfigMigration.migrateAppearances(List.of(child, template)).isEmpty());
        assertFalse(child.contains("mechanics"));
        assertEquals("inventory/chair", child.getString("pack.model"));
    }

    @Test
    void emptyAlternativeItemStillCopiesOwnPack() {
        ConfigurationSection item = new YamlConfiguration().createSection("item");
        item.set("pack.model", "inventory/chair");
        item.set("mechanics.furniture.item", "");

        assertEquals(Set.of(item), FurnitureConfigMigration.migrateAppearances(List.of(item)));
        assertEquals("inventory/chair", item.getString("mechanics.furniture.appearance.model"));
        assertTrue(FurnitureConfigMigration.migrateAppearances(List.of(item)).isEmpty());
    }

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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void batchMigrationLightsInheritedBarriersRegardlessOfOrder(boolean templateFirst) {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.set("Mechanics.Furniture.barrier", true);
        template.set("Mechanics.Furniture.barriers", List.of("origin", Map.of("x", 2), "0,1,0"));
        template.set("Mechanics.Furniture.light", 5);
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("mechanics.Furniture.light", 11);
        List<ConfigurationSection> items = templateFirst ? List.of(template, child) : List.of(child, template);

        assertEquals(Set.of(template, child), FurnitureConfigMigration.migrateBarriers(items));
        assertEquals(List.of("0,0,0 11", "2,0,0 11", "0,1,0 11"),
                child.getStringList("mechanics.Furniture.lights"));
        assertFalse(child.contains("mechanics.Furniture.light"));
        assertFalse(child.contains("mechanics.Furniture.barriers"));
        assertTrue(FurnitureConfigMigration.migrate(template));
        assertEquals(List.of("0,0,0 5", "2,0,0 5", "0,1,0 5"),
                template.getStringList("mechanics.Furniture.lights"));
        assertFalse(FurnitureConfigMigration.migrate(child));
        assertTrue(FurnitureConfigMigration.migrateBarriers(items).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void batchMigrationLightsEffectiveBarriersWithChildOverrides(boolean overrideList) {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.set("mechanics.furniture.barrier", true);
        template.set("mechanics.furniture.barriers", List.of("2,0,0"));
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("mechanics.furniture.light", 9);
        if (overrideList) child.set("mechanics.furniture.barriers", List.of(Map.of("z", 3)));
        else child.set("mechanics.furniture.barrier", false);

        FurnitureConfigMigration.migrateBarriers(List.of(child, template));
        assertEquals(overrideList ? List.of("0,0,0 9", "0,0,3 9") : List.of("0,0,0 9", "2,0,0 9"),
                child.getStringList("mechanics.furniture.lights"));
        assertFalse(child.contains("mechanics.furniture.light"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void batchMigrationPreservesExplicitLightsWithInheritedBarriers(boolean emptyLights) {
        ConfigurationSection template = new YamlConfiguration().createSection("base");
        template.set("template", true);
        template.set("mechanics.furniture.barriers", List.of("2,0,0"));
        ConfigurationSection child = new YamlConfiguration().createSection("child");
        child.set("template", "base");
        child.set("mechanics.furniture.light", 11);
        List<String> lights = emptyLights ? List.of() : List.of("0,3,0 7");
        child.set("mechanics.furniture.lights", lights);

        assertEquals(Set.of(child), FurnitureConfigMigration.migrateBarriers(List.of(template, child)));
        assertEquals(lights, child.getStringList("mechanics.furniture.lights"));
        assertFalse(child.contains("mechanics.furniture.light"));
        assertFalse(child.contains("mechanics.furniture.barriers"));
        assertTrue(FurnitureConfigMigration.migrateBarriers(List.of(template, child)).isEmpty());
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
