package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.commands.arguments.OraxenArgument;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class OraxenCommand {

    private static final Set<Command> REGISTERED_COMMANDS = new LinkedHashSet<>();

    private final String name;
    private final List<String> aliases = new ArrayList<>();
    private final List<OraxenCommand> subcommands = new ArrayList<>();
    private final List<OraxenArgument<?>> arguments = new ArrayList<>();
    private OraxenCommand parent;
    private String permission;
    private CommandExecutor executor;
    private PlayerCommandExecutor playerExecutor;

    public OraxenCommand(String name) {
        this.name = name;
    }

    public OraxenCommand withAliases(String... aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
        return this;
    }

    public OraxenCommand withPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public OraxenCommand withSubcommand(OraxenCommand command) {
        command.parent = this;
        subcommands.add(command);
        return this;
    }

    public OraxenCommand withSubcommands(OraxenCommand... commands) {
        for (OraxenCommand command : commands) {
            command.parent = this;
            subcommands.add(command);
        }
        return this;
    }

    public OraxenCommand withArguments(OraxenArgument<?>... arguments) {
        this.arguments.addAll(Arrays.asList(arguments));
        return this;
    }

    public OraxenCommand withOptionalArguments(OraxenArgument<?>... arguments) {
        for (OraxenArgument<?> argument : arguments) {
            argument.setOptional(true);
            this.arguments.add(argument);
        }
        return this;
    }

    public OraxenCommand executes(CommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    public OraxenCommand executesPlayer(PlayerCommandExecutor executor) {
        this.playerExecutor = executor;
        return this;
    }

    public void register() {
        Plugin plugin = OraxenPlugin.get();
        CommandMap commandMap = getCommandMap();
        BukkitOraxenCommand command = new BukkitOraxenCommand(this);
        command.setAliases(aliases);
        command.setPermission(permission);
        unregisterKnownCommands(commandMap, plugin.getName().toLowerCase(Locale.ROOT), command);
        commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), command);
        REGISTERED_COMMANDS.add(command);
        syncCommands();
    }

    public static void unregisterAll() {
        CommandMap commandMap = getCommandMap();
        for (Command command : REGISTERED_COMMANDS) {
            command.unregister(commandMap);
        }
        REGISTERED_COMMANDS.clear();
        syncCommands();
    }

    private boolean matches(String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        if (name.equalsIgnoreCase(lowerInput)) return true;
        return aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(lowerInput));
    }

    private RouteResult execute(CommandSender sender, List<String> input) {
        PermissionResult permissionResult = checkPermission(sender);
        if (!permissionResult.allowed()) return RouteResult.noPermission(permissionResult.permission());

        if (!input.isEmpty()) {
            List<OraxenCommand> matchingChildren = subcommands.stream().filter(command -> command.matches(input.getFirst())).toList();
            boolean hadChildPermissionFailure = false;
            String missingPermission = null;
            OraxenCommand syntaxCommand = null;
            for (OraxenCommand child : matchingChildren) {
                RouteResult result = child.execute(sender, input.subList(1, input.size()));
                if (result.executed()) return result;
                if (result.noPermission()) {
                    hadChildPermissionFailure = true;
                    missingPermission = result.permission();
                } else if (result.syntaxCommand() != null) {
                    syntaxCommand = result.syntaxCommand();
                }
            }
            if (hadChildPermissionFailure) return RouteResult.noPermission(missingPermission);
            if (syntaxCommand != null) return RouteResult.syntax(syntaxCommand);
        }

        ParsedArguments parsed = parseArguments(sender, input);
        if (!parsed.success() || parsed.consumed() != input.size()) return RouteResult.syntax(this);
        return runExecutor(sender, parsed.arguments());
    }

    private RouteResult runExecutor(CommandSender sender, CommandArguments arguments) {
        try {
            if (playerExecutor != null) {
                if (!(sender instanceof Player player)) {
                    Message.NOT_PLAYER.send(sender);
                    return RouteResult.success();
                }
                playerExecutor.run(player, arguments);
                return RouteResult.success();
            }
            if (executor != null) {
                executor.run(sender, arguments);
                return RouteResult.success();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return RouteResult.syntax(this);
    }

    private ParsedArguments parseArguments(CommandSender sender, List<String> input) {
        CommandArguments parsedArguments = new CommandArguments();
        int index = 0;
        for (OraxenArgument<?> argument : arguments) {
            OraxenArgument.ParseResult<?> result = argument.parse(sender, input, index);
            if (!result.success()) {
                if (argument.isOptional()) continue;
                return ParsedArguments.failure();
            }
            parsedArguments.put(argument.getName(), result.value());
            index += result.consumed();
        }
        return ParsedArguments.success(parsedArguments, index);
    }

    private PermissionResult checkPermission(CommandSender sender) {
        if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) {
            return new PermissionResult(false, permission);
        }
        return new PermissionResult(true, null);
    }

    private List<String> tabComplete(CommandSender sender, List<String> input) {
        if (!checkPermission(sender).allowed()) return List.of();
        if (!input.isEmpty()) {
            String token = input.getFirst();
            List<OraxenCommand> matchingChildren = subcommands.stream().filter(command -> command.matches(token)).toList();
            if (!matchingChildren.isEmpty()) {
                return matchingChildren.stream()
                        .map(command -> command.tabComplete(sender, input.subList(1, input.size())))
                        .flatMap(Collection::stream)
                        .distinct()
                        .toList();
            }
        }

        String current = input.isEmpty() ? "" : input.getLast();
        Set<String> suggestions = new LinkedHashSet<>();
        if (input.size() <= 1) {
            for (OraxenCommand child : subcommands) {
                if (child.checkPermission(sender).allowed()) suggestions.add(child.name);
            }
        }
        suggestions.addAll(argumentSuggestions(sender, input));
        return filterPrefix(suggestions, current);
    }

    private List<String> argumentSuggestions(CommandSender sender, List<String> input) {
        int index = 0;
        for (OraxenArgument<?> argument : arguments) {
            if (index >= input.size() - 1) return argument.suggestions(sender);
            OraxenArgument.ParseResult<?> result = argument.parse(sender, input, index);
            if (!result.success()) {
                if (argument.isOptional()) return argument.suggestions(sender);
                return List.of();
            }
            index += result.consumed();
        }
        return List.of();
    }

    private List<String> filterPrefix(Collection<String> suggestions, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(Predicate.not(String::isBlank))
                .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }

    private static CommandMap getCommandMap() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) method.invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to resolve Bukkit command map", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void unregisterKnownCommands(CommandMap commandMap, String fallbackPrefix, Command command) {
        if (!(commandMap instanceof SimpleCommandMap)) return;
        try {
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            List<String> names = new ArrayList<>();
            names.add(command.getName());
            names.add(fallbackPrefix + ":" + command.getName());
            command.getAliases().forEach(alias -> {
                names.add(alias);
                names.add(fallbackPrefix + ":" + alias);
            });
            for (String name : names) {
                Command registered = knownCommands.remove(name.toLowerCase(Locale.ROOT));
                if (registered != null) registered.unregister(commandMap);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void syncCommands() {
        try {
            Bukkit.getServer().getClass().getMethod("syncCommands").invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @FunctionalInterface
    public interface CommandExecutor {
        void run(CommandSender sender, CommandArguments arguments) throws Exception;
    }

    @FunctionalInterface
    public interface PlayerCommandExecutor {
        void run(Player player, CommandArguments arguments) throws Exception;
    }

    private record ParsedArguments(boolean success, CommandArguments arguments, int consumed) {
        private static ParsedArguments success(CommandArguments arguments, int consumed) {
            return new ParsedArguments(true, arguments, consumed);
        }

        private static ParsedArguments failure() {
            return new ParsedArguments(false, null, 0);
        }
    }

    private record PermissionResult(boolean allowed, String permission) {
    }

    private record RouteResult(boolean executed, boolean noPermission, String permission, OraxenCommand syntaxCommand) {
        private static RouteResult success() {
            return new RouteResult(true, false, null, null);
        }

        private static RouteResult syntax(OraxenCommand command) {
            return new RouteResult(false, false, null, command);
        }

        private static RouteResult noPermission(String permission) {
            return new RouteResult(false, true, permission, null);
        }
    }

    private static class BukkitOraxenCommand extends Command {

        private final OraxenCommand command;

        protected BukkitOraxenCommand(OraxenCommand command) {
            super(command.name);
            this.command = command;
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            RouteResult result = command.execute(sender, Arrays.asList(args));
            if (result.noPermission()) {
                Message.NO_PERMISSION.send(sender, AdventureUtils.tagResolver("permission", result.permission()));
                return true;
            }
            if (!result.executed()) {
                String usage = result.syntaxCommand().fullUsage();
                String commandUsage = "/" + commandLabel + (usage.isBlank() ? "" : " " + usage);
                AdventureUtils.sendMessage(sender, AdventureUtils.MINI_MESSAGE.deserialize("<prefix> <red>Wrong usage. Use ")
                        .append(Component.text(commandUsage, NamedTextColor.RED))
                        .append(AdventureUtils.MINI_MESSAGE.deserialize("<red>.")));
            }
            return true;
        }

        @Override
        public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
            return command.tabComplete(sender, Arrays.asList(args));
        }
    }

    private String fullUsage() {
        List<String> parts = new ArrayList<>();
        for (OraxenCommand command = this; command.parent != null; command = command.parent) {
            parts.addFirst(command.name);
        }
        String usage = usage();
        if (!usage.isBlank()) parts.add(usage);
        return String.join(" ", parts);
    }

    private String usage() {
        if (!subcommands.isEmpty()) {
            return "<" + String.join("|", subcommands.stream().map(command -> command.name).distinct().toList()) + ">";
        }
        List<String> parts = new ArrayList<>();
        for (OraxenArgument<?> argument : arguments) {
            parts.add((argument.isOptional() ? "[" : "<") + argument.getName() + (argument.isOptional() ? "]" : ">"));
        }
        return String.join(" ", parts);
    }
}
