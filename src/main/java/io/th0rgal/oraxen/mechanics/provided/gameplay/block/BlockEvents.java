package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlockEvents {

    private final List<BlockEvent> events;

    public BlockEvents(ConfigurationSection section, String sourceID) {
        events = parseEvents(section.getList("events"), sourceID);
    }

    public boolean isEmpty() {
        return events.isEmpty();
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
                Logs.logWarning("Invalid block.events entry in " + sourceID + "; entries must be maps.");
                continue;
            }

            ClickFilter click = ClickFilter.from(eventMap.get("click"), sourceID);
            List<BlockEventAction> actions = parseActions(eventMap.get("actions"), sourceID);
            if (actions.isEmpty()) {
                Logs.logWarning("Block event in " + sourceID + " has no valid actions.");
                continue;
            }

            parsedEvents.add(new BlockEvent(click, actions));
        }

        return List.copyOf(parsedEvents);
    }

    private List<BlockEventAction> parseActions(Object value, String sourceID) {
        if (!(value instanceof List<?> actionConfigs) || actionConfigs.isEmpty()) return List.of();

        List<BlockEventAction> parsedActions = new ArrayList<>();
        for (Object actionConfig : actionConfigs) {
            if (!(actionConfig instanceof Map<?, ?> actionMap)) {
                Logs.logWarning("Invalid block.events action in " + sourceID + "; actions must be maps.");
                continue;
            }

            Object command = actionMap.get("command");
            Object message = actionMap.get("message");
            if (command != null) {
                String commandText = command.toString().trim();
                if (commandText.isEmpty()) {
                    Logs.logWarning("Empty command action in block event of " + sourceID + ".");
                    continue;
                }
                parsedActions.add(new CommandAction(commandText, CommandExecutor.from(actionMap.get("executor"), sourceID)));
                continue;
            }

            if (message != null) {
                parsedActions.add(new MessageAction(message.toString()));
                continue;
            }

            Logs.logWarning("Unknown block.events action in " + sourceID + "; expected 'command' or 'message'.");
        }

        return List.copyOf(parsedActions);
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

    private record BlockEvent(ClickFilter click, List<BlockEventAction> actions) {
        private void run(Player player) {
            for (BlockEventAction action : actions) {
                action.run(player);
            }
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
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        },
        OP_PLAYER {
            @Override
            void run(Player player, String command) {
                Bukkit.dispatchCommand(opCommandSender(player), command);
            }
        };

        abstract void run(Player player, String command);

        private static Player opCommandSender(Player player) {
            return (Player) Proxy.newProxyInstance(
                    Player.class.getClassLoader(),
                    new Class[]{Player.class},
                    (proxy, method, args) -> invokeAsOpPlayer(player, proxy, method, args)
            );
        }

        private static boolean grantsOpPermission(Method method) {
            return method.getName().equals("hasPermission") || method.getName().equals("isPermissionSet");
        }

        private static Object invokeAsOpPlayer(Player player, Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "OpPlayerCommandSender{" + player.getName() + "}";
                    default -> method.invoke(player, args);
                };
            }

            if (method.getParameterCount() == 0 && method.getName().equals("isOp")) return true;
            if (method.getParameterCount() == 1 && grantsOpPermission(method)) return true;
            if (method.getParameterCount() == 1 && method.getName().equals("setOp")) return null;

            try {
                return method.invoke(player, args);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        }

        private static CommandExecutor from(Object value, String sourceID) {
            if (value == null) return PLAYER;

            String normalized = value.toString().trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if (normalized.isEmpty()) return PLAYER;

            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException exception) {
                Logs.logWarning("Invalid block.events executor '" + value + "' in " + sourceID + "; using PLAYER.");
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

        private static ClickFilter from(Object value, String sourceID) {
            if (value == null) return BOTH;

            String normalized = value.toString().trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if (normalized.isEmpty()) return BOTH;

            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException exception) {
                Logs.logWarning("Invalid block.events click filter '" + value + "' in " + sourceID + "; using BOTH.");
                return BOTH;
            }
        }
    }
}
