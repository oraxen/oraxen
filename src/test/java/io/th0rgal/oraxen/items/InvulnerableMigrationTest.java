package io.th0rgal.oraxen.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvulnerableMigrationTest {

    @Test
    void migratesLegacyDamageFlagsOntoTheItemProperty() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection item = config.createSection("test_item");
        item.set("invulnerable", List.of("lightning", "lava"));
        item.set("components.fire_resistant", true);
        item.set("mechanics.misc.burns_in_fire", false);
        item.set("mechanics.misc.burns_in_lava", false);
        item.set("mechanics.misc.breaks_from_cactus", false);
        item.set("mechanics.misc.prevent_renaming", true);
        item.set("mechanics.invulnerable", List.of("entity_explosion"));

        ItemMigrator migrator = new ItemMigrator(item);

        List<String> causes = item.getStringList("invulnerable");
        assertEquals(6, causes.size());
        assertTrue(causes.containsAll(List.of(
                "lightning", "lava", "fire", "fire_tick", "contact", "entity_explosion")));
        assertTrue(item.getBoolean("mechanics.misc.prevent_renaming"));
        assertNull(item.get("mechanics.misc.burns_in_fire"));
        assertNull(item.get("mechanics.misc.burns_in_lava"));
        assertNull(item.get("mechanics.misc.breaks_from_cactus"));
        assertNull(item.get("mechanics.invulnerable"));
        assertTrue(item.getBoolean("components.fire_resistant"));
        assertTrue(migrator.configUpdated());
        assertTrue(migrator.blockConfigMigrated());
    }

    @Test
    void trueLegacyFlagsDoNotAddProtection() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection item = config.createSection("test_item");
        item.set("mechanics.misc.burns_in_fire", true);

        ItemMigrator migrator = new ItemMigrator(item);

        assertNull(item.get("mechanics.misc"));
        assertNull(item.get("invulnerable"));
        assertTrue(migrator.configUpdated());
        assertFalse(item.getBoolean("components.fire_resistant"));
    }
}
