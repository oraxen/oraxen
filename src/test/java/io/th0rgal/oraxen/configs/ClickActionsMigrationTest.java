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
            assertEquals(2, actions.size());
            assertEquals("[console] say one", ((Map<?, ?>) actions.get(0)).get("legacy"));
            assertEquals(List.of("#player.hasPermission('test')"), ((Map<?, ?>) actions.get(0)).get("conditions"));
        }
    }
}
