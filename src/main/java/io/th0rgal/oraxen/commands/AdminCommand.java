package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.commands.arguments.*;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class AdminCommand {

    // Caps the edit cube so a single command cannot scan/mutate an unbounded
    // area; large areas would also stall whichever thread processes them.
    private static final int MAX_RADIUS = 16;
    private static final int EDITS_PER_REGION_TICK = 256;

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
                        if (type == null) return;
                        performEdits(getTargetLocations(loc, radius, isRandom), target -> {
                            if (type.equals("remove")) OraxenBlocks.remove(target, null);
                            if (type.equals("place")) OraxenBlocks.place(id, target);
                        });
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
                        performEdits(getTargetLocations(loc, radius, isRandom), target -> {
                            if (type.equals("remove")) {
                                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(target.getBlock());
                                if (mechanic != null && (id.isEmpty() || id.equals("all") || mechanic.getItemID().equals(id)))
                                    OraxenFurniture.remove(target, null);
                            }
                            if (type.equals("place")) OraxenFurniture.place(id, target, 0f, null);
                        });
                    }
                });
    }

    private void performEdits(Collection<Location> targets, Consumer<Location> edit) {
        Map<Long, List<Location>> targetsByChunk = new HashMap<>();
        for (Location target : targets) {
            long chunkKey = ((long) target.getBlockX() >> 4) << 32 | ((target.getBlockZ() >> 4) & 0xffffffffL);
            targetsByChunk.computeIfAbsent(chunkKey, ignored -> new ArrayList<>()).add(target);
        }

        for (List<Location> chunkTargets : targetsByChunk.values()) {
            Location anchor = chunkTargets.getFirst();
            World world = Objects.requireNonNull(anchor.getWorld());
            int chunkX = anchor.getBlockX() >> 4;
            int chunkZ = anchor.getBlockZ() >> 4;
            world.getChunkAtAsync(chunkX, chunkZ, true).thenAccept(chunk ->
                    SchedulerUtil.runAtLocation(anchor, () -> {
                        chunk.addPluginChunkTicket(OraxenPlugin.get());
                        processChunkBatch(chunk, anchor, chunkTargets.iterator(), edit);
                    }));
        }
    }

    private void processChunkBatch(Chunk chunk, Location anchor, Iterator<Location> targets, Consumer<Location> edit) {
        boolean nextBatchScheduled = false;
        try {
            for (int processed = 0; processed < EDITS_PER_REGION_TICK && targets.hasNext(); processed++)
                edit.accept(targets.next());
            if (targets.hasNext()) {
                nextBatchScheduled = true;
                SchedulerUtil.runAtLocationLater(anchor, 1L, () -> processChunkBatch(chunk, anchor, targets, edit));
            }
        } finally {
            if (!nextBatchScheduled) chunk.removePluginChunkTicket(OraxenPlugin.get());
        }
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
