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

    private static final List<String> MECHANIC_IDS =
            List.of("block", "furniture", "noteblock", "stringblock", "chorusblock", "shaped_block");

    public static boolean migrate(ConfigurationSection mechanics) {
        if (mechanics == null) return false;
        boolean changed = false;
        for (String mechanicId : MECHANIC_IDS)
            changed |= migrateMechanic(mechanics.getConfigurationSection(mechanicId));
        return changed;
    }

    private static boolean migrateMechanic(ConfigurationSection mechanic) {
        if (mechanic == null || !mechanic.contains("clickActions")) return false;

        List<?> oldActions = mechanic.getList("clickActions");
        if (oldActions == null || oldActions.isEmpty()) {
            mechanic.set("clickActions", null);
            return true;
        }

        List<Object> events = new ArrayList<>(mechanic.getList("events", List.of()));
        boolean migratedAny = false;
        for (Object entry : oldActions) {
            Map<String, Object> event = migrateEntry(entry);
            if (event == null) continue;
            events.add(event);
            migratedAny = true;
        }
        if (!migratedAny) return false;

        mechanic.set("events", events);
        mechanic.set("clickActions", null);
        return true;
    }

    /** Converts one legacy clickActions entry into a right-click event, or null when it holds no usable action. */
    private static Map<String, Object> migrateEntry(Object entry) {
        if (!(entry instanceof Map<?, ?> oldEntry)) return null;
        if (!(oldEntry.get("actions") instanceof List<?> actions) || actions.isEmpty()) return null;

        List<String> legacyActions = new ArrayList<>();
        for (Object action : actions)
            if (action instanceof String actionText && !actionText.isBlank())
                legacyActions.add(actionText);
        if (legacyActions.isEmpty()) return null;

        // One legacy action keeps the old entry's actions in a single ordered run behind one condition check.
        Map<String, Object> migrated = new LinkedHashMap<>();
        migrated.put("legacy", legacyActions);
        List<String> conditions = conditionsOf(oldEntry.get("conditions"));
        if (!conditions.isEmpty())
            migrated.put("conditions", conditions);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("click", "RIGHT");
        event.put("actions", List.of(migrated));
        return event;
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
