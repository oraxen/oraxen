package io.th0rgal.oraxen.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemNameMigrationTest {

    @ParameterizedTest
    @CsvSource({"itemname, name", "displayname, name", "customname, custom-name"})
    void migratesKeysAndCommentsWithoutChangingValues(String legacy, String canonical) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("test_item:\n  # Name comment\n  " + legacy
                + ": 'displayname: itemname customname' # Inline comment\n"
                + "  lore:\n    - 'displayname: itemname customname'\n");
        ConfigurationSection item = config.getConfigurationSection("test_item");
        List<String> comments = item.getComments(legacy);
        List<String> inlineComments = item.getInlineComments(legacy);

        ItemMigrator migrator = new ItemMigrator(item);
        YamlConfiguration saved = new YamlConfiguration();
        saved.loadFromString(config.saveToString());

        assertFalse(item.isSet(legacy));
        assertEquals("displayname: itemname customname", saved.getString("test_item." + canonical));
        assertEquals(List.of("displayname: itemname customname"), saved.getStringList("test_item.lore"));
        assertEquals(comments, saved.getComments("test_item." + canonical));
        assertEquals(inlineComments, saved.getInlineComments("test_item." + canonical));
        assertTrue(migrator.configUpdated());
        assertTrue(migrator.blockConfigMigrated());
        assertFalse(ItemMigrator.migrateNames(item));
    }

    @Test
    void preservesCanonicalNamesAndRemovesAllLegacyKeys() {
        ConfigurationSection item = new YamlConfiguration().createSection("test_item");
        item.set("name", "Canonical name");
        item.set("itemname", "Old item name");
        item.set("displayname", "Old display name");
        item.set("custom-name", "Canonical custom name");
        item.set("customname", "Old custom name");

        assertTrue(ItemMigrator.migrateNames(item));

        assertEquals("Canonical name", item.getString("name"));
        assertEquals("Canonical custom name", item.getString("custom-name"));
        assertEquals(List.of("name", "custom-name"), List.copyOf(item.getKeys(false)));
    }

    @Test
    void prefersItemNameOverDisplayNameAndKeepsCustomNameSeparate() {
        ConfigurationSection item = new YamlConfiguration().createSection("test_item");
        item.set("displayname", "Old display name");
        item.set("itemname", "Old item name");
        item.set("customname", "Anvil name");

        ItemMigrator.migrateNames(item);

        assertEquals("Old item name", item.getString("name"));
        assertEquals("Anvil name", item.getString("custom-name"));
        assertFalse(item.isSet("displayname"));
        assertFalse(item.isSet("itemname"));
        assertFalse(item.isSet("customname"));
    }
}
