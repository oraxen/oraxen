package io.th0rgal.oraxen.configs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClickActionsMigrationTest {

    @Test
    void movesBlockAndFurnitureActionsWithoutLosingExistingEvents() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection mechanics = config.createSection("mechanics");
        for (String id : List.of("block", "furniture")) {
            ConfigurationSection section = mechanics.createSection(id);
            section.set("events", List.of(Map.of("click", "LEFT", "actions", List.of(Map.of("message", "hello")))));
            section.set("clickActions", List.of(Map.of(
                    "conditions", List.of("#player.hasPermission('test')"),
                    "actions", List.of("[console] say one", "[message] two"))));
        }

        assertTrue(ClickActionsMigration.migrate(mechanics));
        assertFalse(ClickActionsMigration.migrate(mechanics));
        for (String id : List.of("block", "furniture")) {
            ConfigurationSection section = mechanics.getConfigurationSection(id);
            assertNotNull(section);
            assertFalse(section.contains("clickActions"));
            List<?> events = section.getList("events");
            assertNotNull(events);
            assertEquals(2, events.size());
            Map<?, ?> migrated = (Map<?, ?>) events.get(1);
            assertEquals("RIGHT", migrated.get("click"));
            List<?> actions = (List<?>) migrated.get("actions");
            assertEquals(1, actions.size());
            assertEquals(List.of("[console] say one", "[message] two"), ((Map<?, ?>) actions.get(0)).get("legacy"));
            assertEquals(List.of("#player.hasPermission('test')"), ((Map<?, ?>) actions.get(0)).get("conditions"));
        }
    }

    @Test
    void keepsScalarConditions() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection mechanic = config.createSection("mechanics.noteblock");
        mechanic.set("clickActions", List.of(Map.of(
                "conditions", "#player.hasPermission('test')",
                "actions", List.of("[message] hi"))));

        assertTrue(ClickActionsMigration.migrate(config.getConfigurationSection("mechanics")));

        List<?> events = mechanic.getList("events");
        assertNotNull(events);
        Map<?, ?> action = (Map<?, ?>) ((List<?>) ((Map<?, ?>) events.get(0)).get("actions")).get(0);
        assertEquals(List.of("#player.hasPermission('test')"), action.get("conditions"));
        assertFalse(mechanic.contains("clickActions"));
    }

    @Test
    void keepsUnreadableClickActions() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection mechanic = config.createSection("mechanics.furniture");
        mechanic.set("clickActions", List.of(Map.of("actions", List.of(1, true))));

        assertFalse(ClickActionsMigration.migrate(config.getConfigurationSection("mechanics")));
        assertTrue(mechanic.contains("clickActions"));
        assertNull(mechanic.get("events"));
    }
}
