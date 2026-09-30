package io.th0rgal.oraxen.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantableMigrationTest {

    @ParameterizedTest
    @CsvSource({"true, false", "false, true"})
    void migratesLegacyValueWithInvertedMeaning(boolean disableEnchanting, boolean enchantable) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection item = config.createSection("test_item");
        item.set("disable_enchanting", disableEnchanting);

        ItemMigrator migrator = new ItemMigrator(item);

        assertFalse(item.contains("disable_enchanting"));
        assertEquals(enchantable, item.getBoolean("enchantable"));
        assertTrue(migrator.configUpdated());
        assertTrue(migrator.blockConfigMigrated());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void keepsExplicitEnchantableWhenBothOptionsExist(boolean enchantable) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection item = config.createSection("test_item");
        item.set("disable_enchanting", enchantable);
        item.set("enchantable", enchantable);

        new ItemMigrator(item);

        assertFalse(item.contains("disable_enchanting"));
        assertEquals(enchantable, item.getBoolean("enchantable"));
    }
}
