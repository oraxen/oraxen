package io.th0rgal.oraxen.command.argument;

import static io.th0rgal.oraxen.command.argument.function.FunctionHelper.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.syntaxphoenix.syntaxapi.command.ArgumentSuperType;
import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.BaseArgument;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import com.syntaxphoenix.syntaxapi.command.arguments.StringArgument;

import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;

public abstract class ArgumentHelper {

    /*
     * Global Variables
     */

    private static final Location ORIGIN = new Location(null, 0, 0, 0);
    
    /*
     * 
     * Completion helper
     * 
     */
    
    public static void completion(DefaultCompletion completion, String... strings) {
        for(int index = 0; index < strings.length; index++)
            completion.add(new StringArgument(strings[index]));
    }

    /*
     * 
     * Optional Arguments
     * 
     */

    public static Optional<BaseArgument> get(Arguments arguments, int position) {
        return arguments.count() < position ? Optional.empty() : Optional.of(arguments.get(position));
    }

    public static Optional<BaseArgument> get(Arguments arguments, int position, ArgumentType type) {
        return get(arguments, position).filter(argument -> argument.getType() == type);
    }

    public static Optional<BaseArgument> get(Arguments arguments, int position, ArgumentSuperType type) {
        return get(arguments, position).filter(argument -> argument.getSuperType() == type);
    }

    /*
     * 
     * Optional Link
     * 
     */

    public static <E> Optional<E> get(Arguments arguments, int position, Function<BaseArgument, Optional<E>> mapper) {
        return get(arguments, position).flatMap(mapper);
    }

    /*
     * 
     * Number helper
     * 
     */

    public static <E extends Number> Optional<E> range(Optional<E> optional, E min, E max) {
        return max(min(optional, min), max);
    }

    public static <E extends Number> Optional<E> min(Optional<E> optional, E min) {
        return optional.map(value -> min(value, min));
    }

    private static <E extends Number> E min(E value, E min) {
        return value.doubleValue() > min.doubleValue() ? value : min;
    }

    public static <E extends Number> Optional<E> max(Optional<E> optional, E max) {
        return optional.map(value -> max(value, max));
    }

    private static <E extends Number> E max(E value, E max) {
        return value.doubleValue() < max.doubleValue() ? value : max;
    }
    
    /*
     * 
     * String helper
     * 
     */
    
    public static Optional<String> restrict(Optional<StringArgument> optional, String... values) {
        return restrict(optional, Arrays.asList(values));
    }
    
    public static Optional<String> restrict(Optional<StringArgument> optional, List<String> values) {
        return optional.map(argument -> argument.getValue()).filter(value -> values.contains(value));
    }
    
    /*
     * 
     * Loop helper
     * 
     */
    
    public static <E> void forEach(Optional<E[]> optional, Consumer<E> action) {
        optional.ifPresent(array -> {
            for(int index = 0; index < array.length; index++)
                action.accept(array[index]);
        });
    }

    /*
     * 
     * Items
     * 
     */

    public static Optional<ItemBuilder> item(BaseArgument argument) {
        if (argument == null)
            return Optional.empty();
        switch (argument.getType()) {
        case STRING:
            return OraxenItems.getOptionalItemById(argument.asString().getValue());
        default:
            return Optional.empty();
        }
    }

    /*
     * 
     * Player
     * 
     */

    public static Optional<Player> player(CommandSender sender, BaseArgument argument) {
        if (argument == null)
            return Optional.empty();
        switch (argument.getType()) {
        case STRING:
            String value = argument.asString().getValue();
            if (value.startsWith("@") && value.length() >= 2) {
                char type = value.charAt(1);
                switch (type) {
                case 'p':
                    if (sender == null)
                        return Optional.empty();
                    if (sender instanceof Player)
                        return Optional.of((Player) sender);
                    Collection<? extends Player> collection0 = Bukkit.getOnlinePlayers();
                    if (collection0.isEmpty())
                        return Optional.empty();
                    return collection0
                        .stream()
                        .unordered()
                        .min((p1, p2) -> Double
                            .compare(ORIGIN.distanceSquared(p1.getLocation()),
                                ORIGIN.distanceSquared(p2.getLocation())))
                        .map(player -> player);
                case 's':
                    if (sender == null)
                        return Optional.empty();
                    return Optional.ofNullable(sender instanceof Player ? (Player) sender : null);
                case 'r':
                    Collection<? extends Player> collection = Bukkit.getOnlinePlayers();
                    if (collection.isEmpty())
                        return Optional.empty();
                    return collection
                        .stream()
                        .unordered()
                        .skip((long) (Math.random() * 500L))
                        .findAny()
                        .map(player -> player);
                default:
                    return Optional.empty();
                }
            }
            return ofGet(() -> Bukkit.getPlayer(UUID.fromString(value)), () -> Bukkit.getPlayer(value));
        default:
            return Optional.empty();
        }
    }

    private static Optional<Player[]> players(CommandSender sender, String value) {
        if (value.startsWith("@") && value.length() >= 2) {
            char type = value.charAt(1);
            switch (type) {
            case 'p':
                if (sender == null)
                    return Optional.empty();
                if (sender instanceof Player)
                    return Optional.of((Player) sender).map(player -> new Player[] { player });
                Collection<? extends Player> collection0 = Bukkit.getOnlinePlayers();
                if (collection0.isEmpty())
                    return Optional.empty();
                return collection0
                    .stream()
                    .unordered()
                    .min((p1, p2) -> Double
                        .compare(ORIGIN.distanceSquared(p1.getLocation()), ORIGIN.distanceSquared(p2.getLocation())))
                    .map(player -> new Player[] { player });
            case 's':
                if (sender == null)
                    return Optional.empty();
                return Optional
                    .ofNullable(sender instanceof Player ? (Player) sender : null)
                    .map(player -> new Player[] { player });
            case 'r':
                Collection<? extends Player> collection = Bukkit.getOnlinePlayers();
                if (collection.isEmpty())
                    return Optional.empty();
                return collection
                    .stream()
                    .unordered()
                    .skip((long) (Math.random() * 500L))
                    .findAny()
                    .map(player -> new Player[] { player });
            case 'a':
                Collection<? extends Player> collection1 = Bukkit.getOnlinePlayers();
                if (collection1.isEmpty())
                    return Optional.empty();
                return Optional.of(collection1.stream().toArray(size -> new Player[size]));
            default:
                return Optional.empty();
            }
        }
        return ofGet(() -> Bukkit.getPlayer(UUID.fromString(value)), () -> Bukkit.getPlayer(value))
            .map(player -> new Player[] { player });
    }

    public static Optional<Player[]> players(CommandSender sender, BaseArgument argument) {
        switch (argument.getType()) {
        case STRING:
            return players(sender, argument.asString().getValue());
        case LIST:
            List<BaseArgument> list = argument.asList().getValue();
            if (list.isEmpty())
                return Optional.empty();
            ArrayList<Player> players0 = new ArrayList<>();
            int length = list.size();
            for (int index = 0; index < length; index++)
                players(sender, list.get(index)).ifPresent(player -> Collections.addAll(players0, player));
            return Optional.ofNullable(players0.isEmpty() ? null : players0.toArray(new Player[0]));
        case ARRAY:
            BaseArgument[] values = argument.asArray().getValue();
            if (values.length == 0)
                return Optional.empty();
            ArrayList<Player> players = new ArrayList<>();
            for (int index = 0; index < values.length; index++)
                players(sender, values[index]).ifPresent(player -> Collections.addAll(players, player));
            return Optional.ofNullable(players.isEmpty() ? null : players.toArray(new Player[0]));
        default:
            return Optional.empty();
        }
    }

}
