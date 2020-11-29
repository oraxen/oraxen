package io.th0rgal.oraxen.command.argument;

import static io.th0rgal.oraxen.command.argument.function.FunctionHelper.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.syntaxphoenix.syntaxapi.command.ArgumentSuperType;
import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.BaseArgument;
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

    public static Optional<Number> range(Optional<Number> optional, Number min, Number max) {
        return max(min(optional, min), max);
    }

    public static Optional<Number> min(Optional<Number> optional, Number min) {
        return optional.map(value -> min(value, min));
    }

    private static Number min(Number value, Number min) {
        return value.doubleValue() > min.doubleValue() ? value : min;
    }

    public static Optional<Number> max(Optional<Number> optional, Number max) {
        return optional.map(value -> max(value, max));
    }

    private static Number max(Number value, Number max) {
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
        return optional.map(StringArgument::getValue).filter(values::contains);
    }

    /*
     * 
     * Loop helper
     * 
     */

    public static <E> void forEach(Optional<E[]> optional, Consumer<E> action) {
        optional.ifPresent(array -> {
            for (E e : array)
                action.accept(e);
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
        if (argument.getType() == ArgumentType.STRING) {
            return OraxenItems.getOptionalItemById(argument.asString().getValue());
        }
        return Optional.empty();
    }

    public static Optional<ItemStack> generalItem(BaseArgument argument) {
        if (argument == null)
            return Optional.empty();
        if (argument.getType() == ArgumentType.STRING) {
            String value = argument.asString().getValue();
            try {
                return Optional.of(new ItemStack(Material.valueOf(value.toUpperCase().replaceFirst("MINECRAFT:", ""))));
            } catch (IllegalArgumentException ignore) {
                return OraxenItems.getOptionalItemById(value.replaceFirst("oraxen:", "")).map(ItemBuilder::build);
            }
        }
        return Optional.empty();
    }

    /*
     * 
     * Player
     * 
     */

    public static Optional<Player> player(CommandSender sender, BaseArgument argument) {
        if (argument == null)
            return Optional.empty();
        if (argument.getType() == ArgumentType.STRING) {
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
                        .skip((int) (collection.size() * Math.random()))
                        .findFirst()
                        .map(player -> player);
                default:
                    return Optional.empty();
                }
            }
            return ofGet(() -> Bukkit.getPlayer(UUID.fromString(value)), () -> Bukkit.getPlayer(value));
        }
        return Optional.empty();
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
                    .min(Comparator.comparingDouble(p -> ORIGIN.distanceSquared(p.getLocation())))
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
                    .skip((int) (collection.size() * Math.random()))
                    .findFirst()
                    .map(player -> new Player[] { player });
            case 'a':
                Collection<? extends Player> collection1 = Bukkit.getOnlinePlayers();
                if (collection1.isEmpty())
                    return Optional.empty();
                return Optional.of(collection1.toArray(new Player[0]));
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
            for (BaseArgument baseArgument : list)
                players(sender, baseArgument).ifPresent(player -> Collections.addAll(players0, player));
            return Optional.ofNullable(players0.isEmpty() ? null : players0.toArray(new Player[0]));
        case ARRAY:
            BaseArgument[] values = argument.asArray().getValue();
            if (values.length == 0)
                return Optional.empty();
            ArrayList<Player> players = new ArrayList<>();
            for (BaseArgument value : values)
                players(sender, value).ifPresent(player -> Collections.addAll(players, player));
            return Optional.ofNullable(players.isEmpty() ? null : players.toArray(new Player[0]));
        default:
            return Optional.empty();
        }
    }

}
