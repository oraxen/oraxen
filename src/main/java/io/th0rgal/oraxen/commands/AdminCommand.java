package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.commands.arguments.*;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Stream;

public class AdminCommand {

    // Caps the edit cube so a single command cannot scan/mutate an unbounded
    // area; large areas would also stall whichever thread processes them.
    private static final int MAX_RADIUS = 16;

    OraxenCommand getAdminCommand() {
        return new OraxenCommand("admin")
                .withPermission("oraxen.command.admin")
                .withSubcommands(getFurniturePlaceRemoveCommand(), getNoteblockPlaceRemoveCommand());
    }

    private OraxenCommand getNoteblockPlaceRemoveCommand() {
        return new OraxenCommand("block")
                .withArguments(new TextArgument("block").replaceSuggestions(ArgumentSuggestions.strings(info ->
                        OraxenBlocks.getBlockIDs().toArray(new String[0]))))
                .withArguments(new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("place", "remove")))
                .withOptionalArguments(new LocationArgument("location"))
                .withOptionalArguments(new IntegerArgument("radius"))
                .withOptionalArguments(new BooleanArgument("random"))
                .executesPlayer((player, args) -> {
                    String id = (String) args.get("block");
                    if (!OraxenBlocks.isOraxenBlock(id)) {
                        AdventureUtils.sendMessage(player, AdventureUtils.MINI_MESSAGE.deserialize("<prefix> <red>Unknown block <white>" + id + "<red>."));
                    } else {
                        Location loc = (Location) args.getOptional("location").orElse(player.getLocation());
                        String type = (String) args.get("type");
                        int radius = clampRadius(player, (int) args.getOptional("radius").orElse(1));
                        boolean isRandom = (boolean) args.getOptional("random").orElse(false);
                        for (Location target : getTargetLocations(loc, radius, isRandom)) {
                            if (type == null) continue;
                            // The target may be anywhere in the world; blocks must be
                            // touched on the region thread that owns them (Folia).
                            SchedulerUtil.runAtLocation(target, () -> {
                                if (type.equals("remove")) OraxenBlocks.remove(target, null);
                                if (type.equals("place")) OraxenBlocks.place(id, target);
                            });
                        }
                    }
                });
    }

    private OraxenCommand getFurniturePlaceRemoveCommand() {
        return new OraxenCommand("furniture")
                .withArguments(
                        new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("place", "remove")),
                        new TextArgument("furniture").replaceSuggestions(ArgumentSuggestions.strings(info ->
                                Stream.concat(Stream.of("all"), OraxenFurniture.getFurnitureIDs().stream())
                                        .toArray(String[]::new)))
                )
                .withOptionalArguments(
                        new LocationArgument("location"),
                        new IntegerArgument("radius"),
                        new BooleanArgument("random")
                )
                .executesPlayer((player, args) -> {
                    String type = (String) args.get("type");
                    assert type != null;
                    String id = (String) args.getOrDefault("furniture", "");
                    if (!OraxenFurniture.isFurniture(id))
                        AdventureUtils.sendMessage(player, AdventureUtils.MINI_MESSAGE.deserialize("<prefix> <red>Unknown furniture <white>" + id + "<red>."));
                    else {
                        Location loc = (Location) args.getOptional("location").orElse(player.getLocation());
                        int radius = clampRadius(player, (int) args.getOptional("radius").orElse(0));
                        boolean isRandom = (boolean) args.getOptional("random").orElse(false);
                        for (Location target : getTargetLocations(loc, radius, isRandom)) {
                            // The target may be anywhere in the world; blocks and the
                            // furniture entities around them must be accessed on the
                            // region thread that owns them (Folia).
                            SchedulerUtil.runAtLocation(target, () -> {
                                if (type.equals("remove")) {
                                    FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(target.getBlock());
                                    if (mechanic != null && (id.isEmpty() || id.equals("all") || mechanic.getItemID().equals(id)))
                                        OraxenFurniture.remove(target, null);
                                }
                                if (type.equals("place")) OraxenFurniture.place(id, target, 0f, null);
                            });
                        }
                    }
                });
    }

    private int clampRadius(org.bukkit.entity.Player player, int radius) {
        if (radius > MAX_RADIUS) {
            AdventureUtils.sendMessage(player, AdventureUtils.MINI_MESSAGE.deserialize(
                    "<prefix> <red>Radius capped to <white>" + MAX_RADIUS + "<red>."));
            return MAX_RADIUS;
        }
        return radius;
    }

    /**
     * Computes block-aligned target locations without touching any chunk or
     * block on the calling thread; callers hop to the owning region thread
     * per location before accessing world state.
     */
    private Collection<Location> getTargetLocations(Location loc, int radius, boolean isRandom) {
        Location origin = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (radius <= 0) return Collections.singletonList(origin);
        List<Location> locations = new ArrayList<>();
        for (int x = origin.getBlockX() - radius; x <= origin.getBlockX() + radius; x++)
            for (int z = origin.getBlockZ() - radius; z <= origin.getBlockZ() + radius; z++)
                for (int y = origin.getBlockY() - radius; y <= origin.getBlockY() + radius; y++) {
                    locations.add(new Location(loc.getWorld(), x, y, z));
                }
        if (isRandom) return Collections.singletonList(locations.get(new Random().nextInt(locations.size())));
        return locations;
    }
}
