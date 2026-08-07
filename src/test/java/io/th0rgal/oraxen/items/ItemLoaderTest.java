package io.th0rgal.oraxen.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemLoaderTest {

    @AfterEach
    void clearModelData() {
        ItemLoader.MODEL_DATAS_BY_ID.clear();
        ModelData.DATAS.clear();
        ItemTemplate.getItemTemplates().clear();
    }

    @Test
    void registersExplicitlyConfiguredCustomModelData() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                configured:
                  material: PAPER
                  Pack:
                    custom_model_data: 123
                """);

        ConfigurationSection configuredSection = config.getConfigurationSection("configured");
        assertNotNull(configuredSection);
        new ItemLoader(configuredSection);

        Method resolve = ItemProperties.class.getDeclaredMethod("resolveCustomModelData");
        resolve.setAccessible(true);
        ItemProperties configured = new ItemProperties(configuredSection, org.bukkit.Material.PAPER,
                new OraxenMeta(), ItemLoader.MODEL_DATAS_BY_ID);

        // An explicit number short-circuits before automatic assignment, so this needs no plugin.
        assertEquals(123, resolve.invoke(configured));
    }

    @Test
    void templateChildrenDoNotInheritTheTemplateCustomModelData() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                base:
                  material: PAPER
                  Pack:
                    model: base
                    custom_model_data: 321
                templated:
                  template: base
                """);

        ConfigurationSection baseSection = config.getConfigurationSection("base");
        ConfigurationSection templatedSection = config.getConfigurationSection("templated");
        assertNotNull(baseSection);
        assertNotNull(templatedSection);
        ItemTemplate.register(baseSection);
        ItemLoader templated = new ItemLoader(templatedSection);

        // The child still inherits the template's pack info...
        assertEquals("base", getMeta(templated).getModelName());
        // ...but not its number, otherwise every sibling would collide on the same one.
        assertEquals(321, ItemLoader.MODEL_DATAS_BY_ID.get("base").getModelData());
        assertNull(ItemLoader.MODEL_DATAS_BY_ID.get("templated"));
    }

    @Test
    void templateChildrenHaveIndependentPackMetadata() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                base:
                  material: PAPER
                  Pack:
                    model: base
                    custom_model_data: 321
                first:
                  template: base
                  Pack:
                    model: first
                second:
                  template: base
                  Pack:
                    model: second
                """);

        ConfigurationSection baseSection = config.getConfigurationSection("base");
        assertNotNull(baseSection);
        ItemTemplate.register(baseSection);
        ItemLoader first = new ItemLoader(config.getConfigurationSection("first"));
        ItemLoader second = new ItemLoader(config.getConfigurationSection("second"));
        OraxenMeta firstMeta = getMeta(first);
        OraxenMeta secondMeta = getMeta(second);

        assertNotSame(firstMeta, secondMeta);
        assertEquals("first", firstMeta.getModelName());
        assertEquals("second", secondMeta.getModelName());
    }

    private static OraxenMeta getMeta(ItemLoader loader) throws Exception {
        Field field = ItemLoader.class.getDeclaredField("oraxenMeta");
        field.setAccessible(true);
        return (OraxenMeta) field.get(loader);
    }

    @ParameterizedTest
    @CsvSource({
            "noteblock,FULL",
            "stringblock,STRING",
            "chorusblock,CHORUS",
            "shaped_block,STAIR"
    })
    void migratesLegacyBlockMechanics(String legacyMechanic, String expectedType) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                test_item:
                  material: PAPER
                  Mechanics:
                    %s:
                      custom_variation: 1
                """.formatted(legacyMechanic));

        ConfigurationSection itemSection = config.getConfigurationSection("test_item");
        assertNotNull(itemSection);
        ConfigurationSection mechanicsSection = itemSection.getConfigurationSection("Mechanics");
        assertNotNull(mechanicsSection);

        ItemMigrator migrator = new ItemMigrator(itemSection);
        migrator.migrateLegacyBlockMechanics(mechanicsSection);

        ConfigurationSection blockSection = mechanicsSection.getConfigurationSection("block");
        assertNotNull(blockSection);
        assertEquals(expectedType, blockSection.getString("type"));
        assertEquals(1, blockSection.getInt("custom_variation"));
        assertFalse(mechanicsSection.contains(legacyMechanic));
        assertTrue(mechanicsSection.contains("block"));
        assertTrue(migrator.configUpdated());
        assertTrue(migrator.blockConfigMigrated());
    }
}
