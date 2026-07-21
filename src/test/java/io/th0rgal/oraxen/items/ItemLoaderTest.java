package io.th0rgal.oraxen.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemLoaderTest {

    @AfterEach
    void clearModelData() {
        ItemLoader.MODEL_DATAS_BY_ID.clear();
    }

    @Test
    void onlyResolvesExplicitlyConfiguredCustomModelData() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                configured:
                  material: PAPER
                  Pack:
                    custom_model_data: 123
                automatic:
                  material: PAPER
                  Pack:
                    model: automatic
                """);

        Method resolve = ItemProperties.class.getDeclaredMethod("resolveCustomModelData");
        resolve.setAccessible(true);

        ConfigurationSection configuredSection = config.getConfigurationSection("configured");
        ConfigurationSection automaticSection = config.getConfigurationSection("automatic");
        new ItemLoader(configuredSection);
        new ItemLoader(automaticSection);
        ItemProperties configured = new ItemProperties(configuredSection, new OraxenMeta(), ItemLoader.MODEL_DATAS_BY_ID);
        ItemProperties automatic = new ItemProperties(automaticSection, new OraxenMeta(), ItemLoader.MODEL_DATAS_BY_ID);

        assertEquals(123, resolve.invoke(configured));
        assertNull(resolve.invoke(automatic));
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
