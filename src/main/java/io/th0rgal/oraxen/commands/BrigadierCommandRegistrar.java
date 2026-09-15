package io.th0rgal.oraxen.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.commands.arguments.BooleanArgument;
import io.th0rgal.oraxen.commands.arguments.EntitySelectorArgument;
import io.th0rgal.oraxen.commands.arguments.GreedyStringArgument;
import io.th0rgal.oraxen.commands.arguments.IntegerArgument;
import io.th0rgal.oraxen.commands.arguments.LocationArgument;
import io.th0rgal.oraxen.commands.arguments.OraxenArgument;
import io.th0rgal.oraxen.commands.arguments.StringArgument;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registers {@link OraxenCommand} trees through Paper's Brigadier command API
 * ({@code LifecycleEvents.COMMANDS}, available since Paper 1.20.6). This provides typed
 * arguments with client-side validation, native tab completion and syntax highlighting,
 * without any command-map reflection.
 *
 * <p>The lifecycle handler is registered once; Paper re-invokes it on data pack or server
 * reloads and re-registers the same definition tree. {@code /oraxen reload} intentionally
 * never re-registers commands: dynamic state (item names, hud ids, ...) is resolved lazily
 * through suggestion providers and executors instead.
 *
 * <p>This class must only be classloaded after {@link OraxenCommand} has verified the
 * Brigadier API is present, since it references classes missing on Paper 1.20.1-1.20.4.
 */
final class BrigadierCommandRegistrar {

    private static final SimpleCommandExceptionType NO_PLAYER_FOUND =
            new SimpleCommandExceptionType(new LiteralMessage("No player was found"));

    /**
     * A single whitespace-delimited token. {@link StringArgumentType#word()} only accepts
     * {@code [0-9A-Za-z_\-.+]}, but DSL text arguments legitimately take values outside that
     * set (e.g. {@code minecraft:totem_of_undying} item ids, user-configured recipe names),
     * which the legacy command-map path allowed by splitting on whitespace. Parsing up to the
     * next space keeps every suggestion the plugin offers enterable verbatim, with no quoting
     * or escaping required.
     *
     * <p>The client validates against the native quoted-string type, so tokens containing
     * characters outside {@code [0-9A-Za-z_\-.+]} (e.g. {@code :}) render red unless quoted.
     * Suggestions for such tokens are therefore quoted (see {@link #quoteIfNeeded(String)}),
     * and the parser unquotes accordingly.
     */
    private static final ArgumentType<String> TOKEN = new CustomArgumentType<String, String>() {
        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            if (reader.canRead() && StringReader.isQuotedStringStart(reader.peek())) {
                return reader.readQuotedString();
            }
            int start = reader.getCursor();
            while (reader.canRead() && reader.peek() != ' ') reader.skip();
            return reader.getString().substring(start, reader.getCursor());
        }

        @Override
        public ArgumentType<String> getNativeType() {
            return StringArgumentType.string();
        }
    };

    private static final Map<String, OraxenCommand> ROOTS = new LinkedHashMap<>();
    private static boolean handlerRegistered;

    private BrigadierCommandRegistrar() {
    }

    static void register(OraxenCommand root) {
        ROOTS.put(root.getName(), root);
        if (handlerRegistered) return;
        handlerRegistered = true;
        OraxenPlugin.get().getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            for (OraxenCommand command : ROOTS.values()) {
                commands.register(build(command, command.getName()), List.copyOf(command.getAliases()));
            }
        });
    }

    private static LiteralCommandNode<CommandSourceStack> build(OraxenCommand command, String label) {
        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(label);
        // Only the root literal is gated through Brigadier's requires-predicate (hiding the
        // whole tree from players lacking the base permission, like the legacy command map
        // did through Command#setPermission). Subcommand permissions are checked inside the
        // executor instead: a failing requires-predicate makes the client report a generic
        // "Unknown command", whereas the legacy path sent Oraxen's NO_PERMISSION message.
        String permission = command.getPermission();
        if (command.isRoot() && permission != null && !permission.isBlank()) {
            // ';'-separated alternatives: any one grants access (Bukkit setPermission semantics).
            String[] alternatives = permission.split(";");
            literal.requires(source -> {
                for (String alternative : alternatives) {
                    if (!alternative.isBlank() && source.getSender().hasPermission(alternative.trim())) return true;
                }
                return false;
            });
        }

        for (OraxenCommand child : command.getSubcommands()) {
            // Same-named children (e.g. the two "give" overloads) merge via Brigadier's addChild.
            literal.then(build(child, child.getName()));
            for (String alias : child.getAliases()) {
                literal.then(build(child, alias));
            }
        }

        appendArguments(command, literal);
        return literal.build();
    }

    private static void appendArguments(OraxenCommand command, LiteralArgumentBuilder<CommandSourceStack> literal) {
        // Arguments on a node without an executor can never run; skip them entirely.
        if (!command.hasAnyExecutor()) return;

        // Arguments form a chain with additional skip-links over optional arguments, so any
        // optional argument may be omitted individually (matching the legacy command-map
        // parser, e.g. "/oraxen admin block <id> place 5" without a preceding location).
        // Only positions at or after the last required argument get an executor.
        List<OraxenArgument<?>> arguments = command.getArguments();
        int lastRequired = -1;
        for (int i = 0; i < arguments.size(); i++) {
            if (!arguments.get(i).isOptional()) lastRequired = i;
        }

        // Executable without any arguments when every argument is optional (or none exist).
        if (lastRequired < 0) literal.executes(executor(command, List.of()));

        // Build from the tail so skip-links can attach already-built child nodes: each node
        // links to the next argument, and additionally to every argument reachable by
        // skipping over optional ones.
        int count = arguments.size();
        List<CommandNode<CommandSourceStack>> built = new ArrayList<>(Collections.nCopies(count, null));
        for (int i = count - 1; i >= 0; i--) {
            OraxenArgument<?> argument = arguments.get(i);
            ArgumentType<?> type = argumentType(argument);
            RequiredArgumentBuilder<CommandSourceStack, ?> node = Commands.argument(argument.getName(), type);
            if (argument.hasCustomSuggestions()) node.suggests(suggestions(command, argument, type == TOKEN));
            if (i >= lastRequired) node.executes(executor(command, List.copyOf(arguments.subList(0, i + 1))));
            for (int j = i + 1; j < count; j++) {
                node.then(built.get(j));
                if (!arguments.get(j).isOptional()) break;
            }
            built.set(i, node.build());
        }
        for (int j = 0; j < count; j++) {
            literal.then(built.get(j));
            if (!arguments.get(j).isOptional()) break;
        }
    }

    private static Command<CommandSourceStack> executor(OraxenCommand command, List<OraxenArgument<?>> included) {
        return context -> {
            CommandSender sender = context.getSource().getSender();
            String missingPermission = command.firstMissingPermission(sender);
            if (missingPermission != null) {
                Message.NO_PERMISSION.send(sender, AdventureUtils.tagResolver("permission", missingPermission));
                return Command.SINGLE_SUCCESS;
            }
            CommandArguments arguments = new CommandArguments();
            for (OraxenArgument<?> argument : included) {
                Object value;
                try {
                    value = resolveValue(context, argument);
                } catch (IllegalArgumentException e) {
                    // Skip-links make optional arguments genuinely absent from the context.
                    if (argument.isOptional()) continue;
                    throw e;
                }
                arguments.put(argument.getName(), value);
            }
            command.runBrigadierExecutor(sender, arguments);
            return Command.SINGLE_SUCCESS;
        };
    }

    private static Object resolveValue(CommandContext<CommandSourceStack> context, OraxenArgument<?> argument)
            throws CommandSyntaxException {
        String name = argument.getName();
        if (argument instanceof EntitySelectorArgument.ManyPlayers) {
            return context.getArgument(name, PlayerSelectorArgumentResolver.class).resolve(context.getSource());
        }
        if (argument instanceof EntitySelectorArgument.OnePlayer) {
            List<Player> players = context.getArgument(name, PlayerSelectorArgumentResolver.class).resolve(context.getSource());
            if (players.isEmpty()) throw NO_PLAYER_FOUND.create();
            return players.getFirst();
        }
        if (argument instanceof EntitySelectorArgument.ManyEntities) {
            return context.getArgument(name, EntitySelectorArgumentResolver.class).resolve(context.getSource());
        }
        if (argument instanceof LocationArgument) {
            return context.getArgument(name, FinePositionResolver.class).resolve(context.getSource())
                    .toLocation(context.getSource().getLocation().getWorld());
        }
        if (argument instanceof IntegerArgument) return context.getArgument(name, Integer.class);
        if (argument instanceof BooleanArgument) return context.getArgument(name, Boolean.class);
        if (argument instanceof StringArgument || argument instanceof GreedyStringArgument)
            return context.getArgument(name, String.class);
        throw unsupportedArgument(argument);
    }

    private static ArgumentType<?> argumentType(OraxenArgument<?> argument) {
        if (argument instanceof EntitySelectorArgument.ManyPlayers) return ArgumentTypes.players();
        if (argument instanceof EntitySelectorArgument.OnePlayer) return ArgumentTypes.player();
        if (argument instanceof EntitySelectorArgument.ManyEntities) return ArgumentTypes.entities();
        if (argument instanceof LocationArgument) return ArgumentTypes.finePosition();
        if (argument instanceof IntegerArgument) return IntegerArgumentType.integer();
        if (argument instanceof BooleanArgument) return BoolArgumentType.bool();
        if (argument instanceof GreedyStringArgument) return StringArgumentType.greedyString();
        if (argument instanceof StringArgument) return TOKEN;
        throw unsupportedArgument(argument);
    }

    private static IllegalArgumentException unsupportedArgument(OraxenArgument<?> argument) {
        return new IllegalArgumentException("Unsupported Brigadier argument type "
                + argument.getClass().getName() + " for argument '" + argument.getName() + "'");
    }

    private static SuggestionProvider<CommandSourceStack> suggestions(OraxenCommand command, OraxenArgument<?> argument,
                                                                      boolean quoteUnsafeTokens) {
        return (context, builder) -> {
            CommandSender sender = context.getSource().getSender();
            if (command.firstMissingPermission(sender) != null) return builder.buildFuture();
            String remaining = builder.getRemainingLowerCase();
            // Ignore an opening quote so partially-typed quoted tokens still match.
            if (remaining.startsWith("\"")) remaining = remaining.substring(1);
            for (String suggestion : argument.customSuggestions(sender)) {
                if (suggestion == null || suggestion.isBlank()) continue;
                if (!suggestion.toLowerCase(Locale.ROOT).startsWith(remaining)) continue;
                builder.suggest(quoteUnsafeTokens ? quoteIfNeeded(suggestion) : suggestion);
            }
            return builder.buildFuture();
        };
    }

    /**
     * Quotes a suggested token when it contains characters the client-side quoted-string
     * validator rejects unquoted (anything outside {@code [0-9A-Za-z_\-.+]}, notably
     * {@code :} in namespaced ids), so accepted suggestions no longer render red.
     */
    private static String quoteIfNeeded(String token) {
        boolean safe = true;
        for (int i = 0; i < token.length(); i++) {
            if (!StringReader.isAllowedInUnquotedString(token.charAt(i))) {
                safe = false;
                break;
            }
        }
        if (safe) return token;
        return '"' + token.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
