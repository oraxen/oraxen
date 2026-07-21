package io.th0rgal.oraxen.commands.arguments;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EntitySelectorArgument {

    private EntitySelectorArgument() {}

    public static class ManyPlayers extends OraxenArgument<Collection<Player>> {
        public ManyPlayers(String name) {
            super(name);
        }

        @Override
        protected ParseResult<Collection<Player>> parseValue(CommandSender sender, List<String> input, int index) {
            Set<Player> players = selectEntities(sender, input.get(index)).stream()
                    .filter(Player.class::isInstance)
                    .map(Player.class::cast)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            return players.isEmpty() ? ParseResult.failure() : ParseResult.success(players, 1);
        }

        @Override
        protected List<String> defaultSuggestions(CommandSender sender) {
            return playerSuggestions();
        }
    }

    public static class OnePlayer extends OraxenArgument<Player> {
        public OnePlayer(String name) {
            super(name);
        }

        @Override
        protected ParseResult<Player> parseValue(CommandSender sender, List<String> input, int index) {
            List<Player> players = selectEntities(sender, input.get(index)).stream()
                    .filter(Player.class::isInstance)
                    .map(Player.class::cast)
                    .toList();
            return players.size() == 1 ? ParseResult.success(players.getFirst(), 1) : ParseResult.failure();
        }

        @Override
        protected List<String> defaultSuggestions(CommandSender sender) {
            return playerSuggestions();
        }
    }

    public static class ManyEntities extends OraxenArgument<Collection<Entity>> {
        public ManyEntities(String name) {
            super(name);
        }

        @Override
        protected ParseResult<Collection<Entity>> parseValue(CommandSender sender, List<String> input, int index) {
            List<Entity> entities = selectEntities(sender, input.get(index));
            return entities.isEmpty() ? ParseResult.failure() : ParseResult.success(entities, 1);
        }

        @Override
        protected List<String> defaultSuggestions(CommandSender sender) {
            return entitySuggestions();
        }
    }

    private static List<Entity> selectEntities(CommandSender sender, String input) {
        try {
            List<Entity> selected = Bukkit.selectEntities(sender, input);
            if (!selected.isEmpty()) return selected;
        } catch (IllegalArgumentException ignored) {
        }

        Player player = Bukkit.getPlayerExact(input);
        return player == null ? List.of() : List.of(player);
    }

    private static List<String> playerSuggestions() {
        return java.util.stream.Stream.concat(
                Bukkit.getOnlinePlayers().stream().map(Player::getName),
                java.util.stream.Stream.of("@a", "@p", "@s", "@r")
        ).toList();
    }

    private static List<String> entitySuggestions() {
        return java.util.stream.Stream.concat(playerSuggestions().stream(), java.util.stream.Stream.of("@e")).toList();
    }
}
