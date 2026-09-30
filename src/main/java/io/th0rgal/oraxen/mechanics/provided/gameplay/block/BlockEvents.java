package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.actions.ActionConditions;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlockEvents {

    private final List<BlockEvent> events;
    private final String eventPath;

    public BlockEvents(ConfigurationSection section, String sourceID) {
        this(section, sourceID, "block.events");
    }

    public BlockEvents(ConfigurationSection section, String sourceID, String eventPath) {
        this.eventPath = eventPath;
        events = parseEvents(section.getList("events"), sourceID);
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public boolean hasLeftClickEvent() {
        return events.stream().anyMatch(event -> event.click().matches(Action.LEFT_CLICK_BLOCK));
    }

    public boolean run(Player player, Action clickAction) {
        boolean ran = false;
        for (BlockEvent event : events) {
            if (!event.click().matches(clickAction)) continue;
            event.run(player);
            ran = true;
        }
        return ran;
    }

    private List<BlockEvent> parseEvents(Object value, String sourceID) {
        if (!(value instanceof List<?> eventConfigs) || eventConfigs.isEmpty()) return List.of();

        List<BlockEvent> parsedEvents = new ArrayList<>();
        for (Object eventConfig : eventConfigs) {
            if (!(eventConfig instanceof Map<?, ?> eventMap)) {
                Logs.logWarning("Invalid " + eventPath + " entry in " + sourceID + "; entries must be maps.");
                continue;
            }

            ClickFilter click = ClickFilter.from(eventMap.get("click"), sourceID, eventPath);
            List<ConditionalAction> actions = parseActions(eventMap.get("actions"), sourceID);
            if (actions.isEmpty()) {
                Logs.logWarning(eventPath + " entry in " + sourceID + " has no valid actions.");
                continue;
            }

            parsedEvents.add(new BlockEvent(click, actions));
        }

        return List.copyOf(parsedEvents);
    }

    private List<ConditionalAction> parseActions(Object value, String sourceID) {
        if (!(value instanceof List<?> actionConfigs) || actionConfigs.isEmpty()) return List.of();

        List<ConditionalAction> parsedActions = new ArrayList<>();
        for (Object actionConfig : actionConfigs) {
            if (!(actionConfig instanceof Map<?, ?> actionMap)) {
                Logs.logWarning("Invalid " + eventPath + " action in " + sourceID + "; actions must be maps.");
                continue;
            }

            Object command = actionMap.get("command");
            Object message = actionMap.get("message");
            Object legacy = actionMap.get("legacy");
            List<String> conditions = parseConditions(actionMap, sourceID);
            if (command != null) {
                String commandText = command.toString().trim();
                if (commandText.isEmpty()) {
                    Logs.logWarning("Empty command action in " + eventPath + " of " + sourceID + ".");
                    continue;
                }
                parsedActions.add(new ConditionalAction(new CommandAction(commandText, CommandExecutor.from(actionMap.get("executor"), sourceID, eventPath)), conditions));
                continue;
            }

            if (message != null) {
                parsedActions.add(new ConditionalAction(new MessageAction(message.toString()), conditions));
                continue;
            }

            if (legacy != null) {
                // Migrated clickActions run as one ordered sequence so [DELAY] and command order behave as before.
                var actions = OraxenPlugin.get().getClickActionManager().parse(Player.class, legacyActions(legacy));
                if (!actions.isEmpty())
                    parsedActions.add(new ConditionalAction(player -> OraxenPlugin.get().getClickActionManager().runOrdered(player, actions), conditions));
                continue;
            }

            Logs.logWarning("Unknown " + eventPath + " action in " + sourceID + "; expected 'command', 'message', or migrated 'legacy'.");
        }

        return List.copyOf(parsedActions);
    }

    private static List<String> legacyActions(Object legacy) {
        if (legacy instanceof List<?> values)
            return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        return List.of(legacy.toString());
    }

    private List<String> parseConditions(Map<?, ?> actionMap, String sourceID) {
        Object value = actionMap.containsKey("conditions") ? actionMap.get("conditions") : actionMap.get("condition");
        if (value == null) return List.of();
        if (value instanceof String condition) return List.of(condition);
        if (value instanceof List<?> values && values.stream().allMatch(String.class::isInstance))
            return values.stream().map(String.class::cast).toList();
        Logs.logWarning("Invalid " + eventPath + " condition in " + sourceID + "; blocking action.");
        return List.of("false");
    }

    private static String applyPlaceholders(String text, Player player) {
        String parsed = text
                .replace("<Player>", player.getName())
                .replace("<player>", player.getName())
                .replace("<PLAYER>", player.getName());

        if (CompatibilitiesManager.hasPlugin("PlaceholderAPI")) {
            parsed = PapiAliases.setPlaceholders(player, parsed);
        }

        return parsed;
    }

    private static String commandWithoutSlash(String command) {
        String parsed = command.trim();
        while (parsed.startsWith("/")) parsed = parsed.substring(1);
        return parsed;
    }

    private record BlockEvent(ClickFilter click, List<ConditionalAction> actions) {
        private void run(Player player) {
            for (ConditionalAction action : actions) {
                action.run(player);
            }
        }
    }

    private record ConditionalAction(BlockEventAction action, List<String> conditions) {
        private void run(Player player) {
            if (ActionConditions.matches(player, conditions)) action.run(player);
        }
    }

    @FunctionalInterface
    private interface BlockEventAction {
        void run(Player player);
    }

    private record CommandAction(String command, CommandExecutor executor) implements BlockEventAction {
        @Override
        public void run(Player player) {
            String parsedCommand = commandWithoutSlash(applyPlaceholders(command, player));
            if (parsedCommand.isEmpty()) return;
            executor.run(player, parsedCommand);
        }
    }

    private record MessageAction(String message) implements BlockEventAction {
        @Override
        public void run(Player player) {
            AdventureUtils.sendMessage(player,
                    AdventureUtils.MINI_MESSAGE.deserialize(applyPlaceholders(message, player))
            );
        }
    }

    private enum CommandExecutor {
        PLAYER {
            @Override
            void run(Player player, String command) {
                player.performCommand(command);
            }
        },
        CONSOLE {
            @Override
            void run(Player player, String command) {
                if (SchedulerUtil.isGlobalThread()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                else SchedulerUtil.runTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
            }
        },
        OP_PLAYER {
            @Override
            void run(Player player, String command) {
                boolean wasOp = player.isOp();
                if (!wasOp) player.setOp(true);
                try {
                    Bukkit.dispatchCommand(player, command);
                } finally {
                    if (!wasOp) player.setOp(false);
                }
            }
        };

        abstract void run(Player player, String command);

        private static CommandExecutor from(Object value, String sourceID, String eventPath) {
            if (value == null) return PLAYER;

            String normalized = value.toString().trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if (normalized.isEmpty()) return PLAYER;

            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException exception) {
                Logs.logWarning("Invalid " + eventPath + " executor '" + value + "' in " + sourceID + "; using PLAYER.");
                return PLAYER;
            }
        }
    }

    private enum ClickFilter {
        BOTH,
        LEFT,
        RIGHT;

        private boolean matches(Action action) {
            return switch (this) {
                case BOTH -> action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK;
                case LEFT -> action == Action.LEFT_CLICK_BLOCK;
                case RIGHT -> action == Action.RIGHT_CLICK_BLOCK;
            };
        }

        private static ClickFilter from(Object value, String sourceID, String eventPath) {
            if (value == null) return BOTH;

            String normalized = value.toString().trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if (normalized.isEmpty()) return BOTH;

            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException exception) {
                Logs.logWarning("Invalid " + eventPath + " click filter '" + value + "' in " + sourceID + "; using BOTH.");
                return BOTH;
            }
        }
    }
}
