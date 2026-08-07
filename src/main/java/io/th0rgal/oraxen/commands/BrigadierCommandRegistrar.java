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
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
     */
    private static final ArgumentType<String> TOKEN = new CustomArgumentType<String, String>() {
        @Override
        public String parse(StringReader reader) {
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
        String permission = command.getPermission();
        if (permission != null && !permission.isBlank()) {
            literal.requires(source -> source.getSender().hasPermission(permission));
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

        // Constraint: optional arguments must be trailing. Arguments form a single linear
        // chain, and only positions at or after the last required argument get an executor.
        // An optional argument placed before a required one therefore silently becomes
        // required on this path. No current command has that shape; keep it that way.
        List<OraxenArgument<?>> arguments = command.getArguments();
        int lastRequired = -1;
        for (int i = 0; i < arguments.size(); i++) {
            if (!arguments.get(i).isOptional()) lastRequired = i;
        }

        // Executable without any arguments when every argument is optional (or none exist).
        if (lastRequired < 0) literal.executes(executor(command, List.of()));

        List<RequiredArgumentBuilder<CommandSourceStack, ?>> nodes = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            OraxenArgument<?> argument = arguments.get(i);
            RequiredArgumentBuilder<CommandSourceStack, ?> node =
                    Commands.argument(argument.getName(), argumentType(argument));
            if (argument.hasCustomSuggestions()) node.suggests(suggestions(argument));
            if (i >= lastRequired) node.executes(executor(command, List.copyOf(arguments.subList(0, i + 1))));
            nodes.add(node);
        }

        for (int i = nodes.size() - 1; i > 0; i--) {
            nodes.get(i - 1).then(nodes.get(i));
        }
        if (!nodes.isEmpty()) literal.then(nodes.getFirst());
    }

    private static Command<CommandSourceStack> executor(OraxenCommand command, List<OraxenArgument<?>> included) {
        return context -> {
            CommandArguments arguments = new CommandArguments();
            for (OraxenArgument<?> argument : included) {
                arguments.put(argument.getName(), resolveValue(context, argument));
            }
            command.runBrigadierExecutor(context.getSource().getSender(), arguments);
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
        return context.getArgument(name, String.class);
    }

    private static ArgumentType<?> argumentType(OraxenArgument<?> argument) {
        if (argument instanceof EntitySelectorArgument.ManyPlayers) return ArgumentTypes.players();
        if (argument instanceof EntitySelectorArgument.OnePlayer) return ArgumentTypes.player();
        if (argument instanceof EntitySelectorArgument.ManyEntities) return ArgumentTypes.entities();
        if (argument instanceof LocationArgument) return ArgumentTypes.finePosition();
        if (argument instanceof IntegerArgument) return IntegerArgumentType.integer();
        if (argument instanceof BooleanArgument) return BoolArgumentType.bool();
        if (argument instanceof GreedyStringArgument) return StringArgumentType.greedyString();
        return TOKEN;
    }

    private static SuggestionProvider<CommandSourceStack> suggestions(OraxenArgument<?> argument) {
        return (context, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String suggestion : argument.customSuggestions(context.getSource().getSender())) {
                if (suggestion == null || suggestion.isBlank()) continue;
                if (suggestion.toLowerCase(Locale.ROOT).startsWith(remaining)) builder.suggest(suggestion);
            }
            return builder.buildFuture();
        };
    }
}
