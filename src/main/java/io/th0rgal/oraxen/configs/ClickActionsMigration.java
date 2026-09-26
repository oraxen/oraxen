package io.th0rgal.oraxen.configs;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Moves placed block and furniture click actions into their shared events format. */
public final class ClickActionsMigration {

    private ClickActionsMigration() {
    }

    public static boolean migrate(ConfigurationSection mechanics) {
        if (mechanics == null) return false;
        boolean changed = false;
        for (String mechanicId : List.of("block", "furniture", "noteblock", "stringblock", "chorusblock", "shaped_block")) {
            ConfigurationSection mechanic = mechanics.getConfigurationSection(mechanicId);
            if (mechanic == null || !mechanic.contains("clickActions")) continue;

            List<?> oldActions = mechanic.getList("clickActions");
            List<Object> events = new ArrayList<>(mechanic.getList("events", List.of()));
            if (oldActions != null) {
                for (Object entry : oldActions) {
                    if (!(entry instanceof Map<?, ?> oldEntry)) continue;
                    Object rawActions = oldEntry.get("actions");
                    if (!(rawActions instanceof List<?> actions) || actions.isEmpty()) continue;
                    List<Map<String, Object>> migratedActions = new ArrayList<>();
                    for (Object action : actions) {
                        if (!(action instanceof String actionText)) continue;
                        Map<String, Object> migrated = new LinkedHashMap<>();
                        migrated.put("legacy", actionText);
                        if (oldEntry.get("conditions") instanceof List<?> conditions && !conditions.isEmpty())
                            migrated.put("conditions", conditions);
                        migratedActions.add(migrated);
                    }
                    if (!migratedActions.isEmpty()) {
                        Map<String, Object> event = new LinkedHashMap<>();
                        event.put("click", "RIGHT");
                        event.put("actions", migratedActions);
                        events.add(event);
                    }
                }
            }
            mechanic.set("events", events);
            mechanic.set("clickActions", null);
            changed = true;
        }
        return changed;
    }
}
