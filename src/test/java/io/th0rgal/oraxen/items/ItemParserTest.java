package io.th0rgal.oraxen.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemParserTest {

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

        ItemParser parser = new ItemParser(itemSection);
        Method migrate = ItemParser.class.getDeclaredMethod("migrateLegacyBlockMechanics", ConfigurationSection.class);
        migrate.setAccessible(true);
        migrate.invoke(parser, mechanicsSection);

        ConfigurationSection blockSection = mechanicsSection.getConfigurationSection("block");
        assertNotNull(blockSection);
        assertEquals(expectedType, blockSection.getString("type"));
        assertEquals(1, blockSection.getInt("custom_variation"));
        assertFalse(mechanicsSection.contains(legacyMechanic));
        assertTrue(mechanicsSection.contains("block"));
    }
}
