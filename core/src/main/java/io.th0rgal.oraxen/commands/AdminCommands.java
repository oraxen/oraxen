package io.th0rgal.oraxen.commands;

import com.sk89q.worldedit.blocks.Blocks;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminCommands {

    CommandAPICommand getAdminCommand() {
        return new CommandAPICommand("admin")
                .withPermission("oraxen.command.admin")
                .withSubcommands(getFurniturePlaceRemoveCommand(), getNoteblockPlaceRemoveCommand());
    }

    private CommandAPICommand getNoteblockPlaceRemoveCommand() {
        return new CommandAPICommand("block")
                .withArguments(new TextArgument("block").replaceSuggestions(ArgumentSuggestions.strings(OraxenBlocks.getBlockIDs())))
                .withArguments(new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("place", "remove")))
                .withOptionalArguments(new LocationArgument("location"))
                .withOptionalArguments(new IntegerArgument("radius"))
                .withOptionalArguments(new BooleanArgument("random"))
                .executesPlayer((player, args) -> {
                    String id = (String) args.get("block");
                    if (!OraxenBlocks.isOraxenBlock(id)) {
                        OraxenPlugin.get().getAudience().player(player).sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>Unknown OraxenBlock: <yellow>" + id));
                    } else {
                        Location loc = (Location) args.getOptional("location").orElse(player.getLocation());
                        String type = (String) args.get("type");
                        int radius = (int) args.getOptional("radius").orElse(10);
                        boolean isRandom = (boolean) args.getOptional("random").orElse(false);
                        for (Block block : getBlocks(loc, radius, isRandom)) {
                            if (type.equals("remove")) OraxenBlocks.remove(block.getLocation(), null);
                            if (type.equals("place")) OraxenFurniture.place(block.getLocation(), id, Rotation.CLOCKWISE, null);
                        }
                    }
                });
    }

    private CommandAPICommand getFurniturePlaceRemoveCommand() {
        return new CommandAPICommand("furniture")
                .withArguments(new TextArgument("furniture").replaceSuggestions(ArgumentSuggestions.strings(OraxenFurniture.getFurnitureIDs())))
                .withArguments(new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("place", "remove")))
                .withOptionalArguments(new LocationArgument("location"))
                .withOptionalArguments(new IntegerArgument("radius"))
                .withOptionalArguments(new BooleanArgument("random"))
                .executesPlayer((player, args) -> {
                    String id = (String) args.get("furniture");
                    if (!OraxenFurniture.isFurniture(id))
                        OraxenPlugin.get().getAudience().player(player).sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>Unknown Furniture: <yellow>" + id));
                    else {
                        Location loc = (Location) args.getOptional("location").orElse(player.getLocation());
                        int radius = (int) args.getOptional("radius").orElse(0);
                        String type = (String) args.get("type");
                        boolean isRandom = (boolean) args.getOptional("random").orElse(false);
                        for (Block block : getBlocks(loc, radius, isRandom)) {
                            if (type.equals("remove")) OraxenFurniture.remove(block.getLocation(), null);
                            if (type.equals("place"))
                                OraxenFurniture.place(block.getLocation(), id, Rotation.CLOCKWISE, null);
                        }
                    }
                });
    }

    private Collection<Block> getBlocks(Location loc, int radius, boolean isRandom) {
        List<Block> blocks = new ArrayList<>();
        if (radius <= 0) return Collections.singletonList(loc.getBlock());
        for (int x = loc.getBlockX() - radius; x <= loc.getBlockX() + radius; x++)
            for (int z = loc.getBlockZ() - radius; z <= loc.getBlockZ() + radius; z++)
                for (int y = loc.getBlockY() - radius; y <= loc.getBlockY() + radius; y++) {
                    blocks.add(loc.getWorld().getBlockAt(x, y, z));
                }
        if (isRandom) return Collections.singletonList(blocks.get(new Random().nextInt(blocks.size())));
        return blocks;
    }
}
