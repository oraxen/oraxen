package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.DurationUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.actions.ActionConditions;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.timers.TimersFactory;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ScriptEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;

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

    public boolean hasClickEvents() {
        return events.stream().anyMatch(event -> event.trigger() == Trigger.CLICK);
    }

    public boolean hasLeftClickEvent() {
        return events.stream().anyMatch(event -> event.trigger() == Trigger.CLICK && event.click().matches(Action.LEFT_CLICK_BLOCK));
    }

    public boolean run(Player player, Action clickAction) {
        return runTrigger(player, Trigger.CLICK, clickAction);
    }

    public boolean runBreak(Player player) {
        return runTrigger(player, Trigger.BREAK, null);
    }

    public boolean runPlace(Player player) {
        return runTrigger(player, Trigger.PLACE, null);
    }

    public boolean runSit(Player player) {
        return runTrigger(player, Trigger.SIT, null);
    }

    private boolean runTrigger(Player player, Trigger trigger, Action clickAction) {
        if (events.isEmpty()) return false;
        List<BlockEvent> matching = events.stream()
                .filter(event -> event.trigger() == trigger && (trigger != Trigger.CLICK || event.click().matches(clickAction)))
                .toList();
        if (matching.isEmpty()) return false;

        if (matching.stream().anyMatch(BlockEvent::hasJavaActions)) {
            SchedulerUtil.runOnOwningThread(player, () -> runEvents(player, matching));
        } else {
            runEvents(player, matching);
        }
        return true;
    }

    private static void runEvents(Player player, List<BlockEvent> matching) {
        for (BlockEvent event : matching) event.run(player);
    }

    private List<BlockEvent> parseEvents(Object value, String sourceID) {
        if (!(value instanceof List<?> eventConfigs) || eventConfigs.isEmpty()) return List.of();

        List<BlockEvent> parsedEvents = new ArrayList<>();
        for (Object eventConfig : eventConfigs) {
            if (!(eventConfig instanceof Map<?, ?> eventMap)) {
                Logs.logWarning("Invalid " + eventPath + " entry in " + sourceID + "; entries must be maps.");
                continue;
            }

            Trigger trigger = Trigger.from(eventMap.get("trigger"), sourceID, eventPath);
            if (trigger == null) continue;
            Object type = eventMap.containsKey("type") ? eventMap.get("type") : eventMap.get("click");
            ClickFilter click = trigger == Trigger.CLICK ? ClickFilter.from(type, sourceID, eventPath) : ClickFilter.BOTH;
            List<ConditionalAction> actions = parseActions(eventMap.get("actions"), sourceID);
            if (actions.isEmpty()) {
                Logs.logWarning(eventPath + " entry in " + sourceID + " has no valid actions.");
                continue;
            }

            parsedEvents.add(new BlockEvent(trigger, click, actions));
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
            TimersFactory cooldown = null;
            if (actionMap.containsKey("cooldown")) {
                Object cooldownValue = actionMap.get("cooldown");
                OptionalLong duration = DurationUtils.parseMillis(cooldownValue == null ? null : cooldownValue.toString());
                if (duration.isEmpty()) {
                    Logs.logWarning("Invalid " + eventPath + " cooldown '" + cooldownValue + "' in " + sourceID + "; blocking action.");
                    conditions = List.of("false");
                } else if (duration.getAsLong() > 0) {
                    cooldown = new TimersFactory(duration.getAsLong());
                }
            }
            if (actionMap.containsKey("java")) {
                if (actionMap.containsKey("command") || actionMap.containsKey("message") || actionMap.containsKey("legacy")) {
                    Logs.logWarning("Java action in " + eventPath + " of " + sourceID + " cannot be combined with another action type; skipping action.");
                    continue;
                }
                if (!(actionMap.get("java") instanceof String source) || source.isBlank()) {
                    Logs.logWarning("Invalid Java action in " + eventPath + " of " + sourceID + "; expected a non-blank string.");
                    continue;
                }
                try {
                    var evaluator = new ScriptEvaluator();
                    evaluator.setParentClassLoader(BlockEvents.class.getClassLoader());
                    evaluator.setTargetVersion(8); // Static interface methods, such as Component.text, require Java 8 bytecode.
                    evaluator.setDebuggingInformation(true, true, false);
                    var script = evaluator.createFastEvaluator(source, JavaEventScript.class, new String[]{"player", "server"});
                    parsedActions.add(new ConditionalAction(new JavaAction(script, sourceID, eventPath), conditions, cooldown));
                } catch (CompileException | RuntimeException | LinkageError exception) {
                    Logs.logWarning("Failed to compile Java action in " + eventPath + " of " + sourceID + "; skipping action.");
                    Bukkit.getLogger().warning(exception.toString());
                }
                continue;
            }
            if (command != null) {
                String commandText = command.toString().trim();
                if (commandText.isEmpty()) {
                    Logs.logWarning("Empty command action in " + eventPath + " of " + sourceID + ".");
                    continue;
                }
                parsedActions.add(new ConditionalAction(new CommandAction(commandText, CommandExecutor.from(actionMap.get("executor"), sourceID, eventPath)), conditions, cooldown));
                continue;
            }

            if (message != null) {
                parsedActions.add(new ConditionalAction(new MessageAction(message.toString()), conditions, cooldown));
                continue;
            }

            if (legacy != null) {
                // Migrated clickActions run as one ordered sequence so [DELAY] and command order behave as before.
                var actions = OraxenPlugin.get().getClickActionManager().parse(Player.class, legacyActions(legacy));
                if (!actions.isEmpty())
                    parsedActions.add(new ConditionalAction(player -> OraxenPlugin.get().getClickActionManager().runOrdered(player, actions), conditions, cooldown));
                continue;
            }

            Logs.logWarning("Unknown " + eventPath + " action in " + sourceID + "; expected 'command', 'message', 'java', or migrated 'legacy'.");
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

    private record BlockEvent(Trigger trigger, ClickFilter click, List<ConditionalAction> actions) {
        private boolean hasJavaActions() {
            return actions.stream().anyMatch(action -> action.action() instanceof JavaAction);
        }

        private void run(Player player) {
            for (ConditionalAction action : actions) {
                action.run(player);
            }
        }
    }

    private record ConditionalAction(BlockEventAction action, List<String> conditions, TimersFactory cooldown) {
        private void run(Player player) {
            if (!ActionConditions.matches(player, conditions)) return;
            if (cooldown != null) {
                var timer = cooldown.getTimer(player);
                if (!timer.isFinished()) return;
                timer.reset();
            }
            action.run(player);
        }
    }

    @FunctionalInterface
    private interface BlockEventAction {
        void run(Player player);
    }

    @FunctionalInterface
    public interface JavaEventScript {
        void run(Player player, Server server) throws Exception;
    }

    private record JavaAction(JavaEventScript script, String sourceID, String eventPath) implements BlockEventAction {
        @Override
        public void run(Player player) {
            try {
                script.run(player, Bukkit.getServer());
            } catch (Exception | LinkageError exception) {
                Logs.logWarning("Failed to run Java action in " + eventPath + " of " + sourceID + ".");
                Bukkit.getLogger().warning(exception.toString());
                Logs.debug(exception);
            }
        }
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

    private enum Trigger {
        CLICK,
        BREAK,
        PLACE,
        SIT;

        private static Trigger from(Object value, String sourceID, String eventPath) {
            if (value == null) return CLICK;
            try {
                return valueOf(value.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                Logs.logWarning("Invalid " + eventPath + " trigger '" + value + "' in " + sourceID + "; skipping event.");
                return null;
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
