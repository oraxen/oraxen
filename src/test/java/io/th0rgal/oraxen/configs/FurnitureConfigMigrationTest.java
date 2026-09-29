package io.th0rgal.oraxen.configs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FurnitureConfigMigrationTest {

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
