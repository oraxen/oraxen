package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.items.ItemMigrator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulboundConfigMigrationTest {

    @ParameterizedTest
    @CsvSource({"0.25, 25", "0.5, 50", "0.075, 7.5", "0.07, 7", "0.001, 1"})
    void convertsFractionsToPercentagesOnce(double fraction, String percent) {
        YamlConfiguration config = itemConfig(fraction);

        ItemMigrator migrator = new ItemMigrator(config.getConfigurationSection("test_item"));

        assertTrue(migrator.configUpdated());
        assertTrue(migrator.blockConfigMigrated());
        assertEquals(percent, String.valueOf(loseChance(config)));

        YamlConfiguration reloaded = reload(config);
        assertFalse(SoulboundConfigMigration.migrateItem(reloaded.getConfigurationSection("test_item")));
        assertEquals(percent, String.valueOf(loseChance(reloaded)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0", "1", "1.0", "30", "100"})
    void keepsValuesThatAreAlreadyPercentages(String value) {
        YamlConfiguration config = reload("test_item:\n  mechanics:\n    soulbound:\n      lose_chance: " + value + "\n");

        ItemMigrator migrator = new ItemMigrator(config.getConfigurationSection("test_item"));

        assertFalse(migrator.configUpdated());
        assertEquals(value, String.valueOf(loseChance(config)));
    }

    private static YamlConfiguration itemConfig(double loseChance) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection item = config.createSection("test_item");
        item.set("mechanics.soulbound.lose_chance", loseChance);
        return config;
    }

    private static Object loseChance(YamlConfiguration config) {
        return config.get("test_item.mechanics.soulbound.lose_chance");
    }

    private static YamlConfiguration reload(YamlConfiguration config) {
        return reload(config.saveToString());
    }

    private static YamlConfiguration reload(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return config;
    }
}
