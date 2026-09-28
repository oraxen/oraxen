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
            if (oldActions == null || oldActions.isEmpty()) {
                mechanic.set("clickActions", null);
                changed = true;
                continue;
            }

            List<Object> events = new ArrayList<>(mechanic.getList("events", List.of()));
            int migratedActions = 0;
            for (Object entry : oldActions) {
                if (!(entry instanceof Map<?, ?> oldEntry)) continue;
                Object rawActions = oldEntry.get("actions");
                if (!(rawActions instanceof List<?> actions) || actions.isEmpty()) continue;
                List<String> conditions = conditionsOf(oldEntry.get("conditions"));
                List<Map<String, Object>> migratedActionMaps = new ArrayList<>();
                for (Object action : actions) {
                    if (!(action instanceof String actionText) || actionText.isBlank()) continue;
                    Map<String, Object> migrated = new LinkedHashMap<>();
                    migrated.put("legacy", actionText);
                    if (!conditions.isEmpty())
                        migrated.put("conditions", conditions);
                    migratedActionMaps.add(migrated);
                }
                if (migratedActionMaps.isEmpty()) continue;
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("click", "RIGHT");
                event.put("actions", migratedActionMaps);
                events.add(event);
                migratedActions += migratedActionMaps.size();
            }
            if (migratedActions == 0) continue;

            mechanic.set("events", events);
            mechanic.set("clickActions", null);
            changed = true;
        }
        return changed;
    }

    private static List<String> conditionsOf(Object raw) {
        if (raw instanceof String text && !text.isBlank())
            return List.of(text);
        if (!(raw instanceof List<?> list))
            return List.of();
        List<String> conditions = new ArrayList<>();
        for (Object entry : list)
            if (entry instanceof String text && !text.isBlank())
                conditions.add(text);
        return conditions;
    }
}
