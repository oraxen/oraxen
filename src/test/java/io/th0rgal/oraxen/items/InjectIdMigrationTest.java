package io.th0rgal.oraxen.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InjectIdMigrationTest {
    @Test
    void legacyFalseKeepsVanillaItemsUnidentifiedAndPreservesComments() {
        ConfigurationSection item = new YamlConfiguration().createSection("vanilla_item");
        item.set("injectID", false);
        item.setComments("injectID", List.of("Keep this item vanilla"));
        item.setInlineComments("injectID", List.of("No custom ID"));

        ItemMigrator migrator = new ItemMigrator(item);

        assertFalse(item.getBoolean("injectId", true));
        assertNull(item.get("injectID"));
        assertEquals(List.of("Keep this item vanilla"), item.getComments("injectId"));
        assertEquals(List.of("No custom ID"), item.getInlineComments("injectId"));
        assertTrue(migrator.configUpdated());
        assertTrue(migrator.blockConfigMigrated());
    }

    @Test
    void explicitCanonicalValueWinsOverLegacyValue() {
        ConfigurationSection item = new YamlConfiguration().createSection("custom_item");
        item.set("injectID", false);
        item.set("injectId", true);

        new ItemMigrator(item);

        assertTrue(item.getBoolean("injectId"));
        assertNull(item.get("injectID"));
    }

    @Test
    void absentOptionRetainsDefaultWithoutRewritingConfig() {
        ConfigurationSection item = new YamlConfiguration().createSection("custom_item");
        ItemMigrator migrator = new ItemMigrator(item);
        assertTrue(item.getBoolean("injectId", true));
        assertFalse(migrator.configUpdated());
    }
}
