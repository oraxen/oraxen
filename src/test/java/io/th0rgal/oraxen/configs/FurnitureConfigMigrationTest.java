package io.th0rgal.oraxen.configs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FurnitureConfigMigrationTest {

    @ParameterizedTest
    @ValueSource(strings = {"display_entity_properties", "armor_stand_properties"})
    void migratesLegacyProperties(String legacySection) {
        ConfigurationSection furniture = new YamlConfiguration().createSection("item")
                .createSection("mechanics").createSection("furniture");
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
        assertEquals("FIXED", furniture.getString("properties.display_transform"));
        assertEquals(0.25, furniture.getDouble("properties.offset.z"));
        assertFalse(furniture.contains("display_entity_properties"));
        assertFalse(furniture.contains("armor_stand_properties"));
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
}
