package io.th0rgal.oraxen.commands.arguments;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class LocationArgument extends OraxenArgument<Location> {

    public LocationArgument(String name) {
        super(name);
    }

    @Override
    protected ParseResult<Location> parseValue(CommandSender sender, List<String> input, int index) {
        if (index + 2 >= input.size()) return ParseResult.failure();

        Location base = sender instanceof Player player ? player.getLocation() : null;
        World world = base != null ? base.getWorld() : Bukkit.getWorlds().stream().findFirst().orElse(null);
        if (world == null) return ParseResult.failure();

        Double x = parseCoordinate(input.get(index), base == null ? 0 : base.getX());
        Double y = parseCoordinate(input.get(index + 1), base == null ? 0 : base.getY());
        Double z = parseCoordinate(input.get(index + 2), base == null ? 0 : base.getZ());
        if (x == null || y == null || z == null) return ParseResult.failure();

        return ParseResult.success(new Location(world, x, y, z), 3);
    }

    private Double parseCoordinate(String input, double relativeTo) {
        if (input.equals("~")) return relativeTo;
        if (input.startsWith("~")) {
            try {
                return relativeTo + Double.parseDouble(input.substring(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected List<String> defaultSuggestions(CommandSender sender) {
        return List.of("~ ~ ~");
    }
}
