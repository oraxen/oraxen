package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional.DirectionalBlock;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WorldEditUtils {

    private WorldEditUtils() {}

    protected static class OraxenBlockInputParser extends InputParser<BaseBlock> {

        public OraxenBlockInputParser() {
            super(WorldEdit.getInstance());
            if (WrappedWorldEdit.loaded) {
                WorldEdit.getInstance().getBlockFactory().register(this);
            }
        }

        @Override
        public BaseBlock parseFromInput(String input, ParserContext context) {
            if (input.equals("minecraft:note_block") || input.equals("note_block")) {
                return BukkitAdapter.adapt(Bukkit.createBlockData(Material.NOTE_BLOCK)).toBaseBlock();
            } else if (input.equals("minecraft:tripwire") || input.equals("tripwire")) {
                return BukkitAdapter.adapt(Bukkit.createBlockData(Material.TRIPWIRE)).toBaseBlock();
            }

            if (!input.startsWith("oraxen:") || input.endsWith(":")) return null;
            String id = input.split(":")[1].split("\\[")[0]; // Potential arguments
            boolean hasArguments = input.contains("[");
            if (id.equals(input) || !OraxenBlocks.isOraxenBlock(id)) return null;

            NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(id);
            BlockData blockData = OraxenBlocks.getOraxenBlockData(id);
            if (blockData == null) return null;

            if (Settings.WORLDEDIT_STRINGBLOCKS.toBool() && OraxenBlocks.isOraxenStringBlock(id))
                return BukkitAdapter.adapt(blockData).toBaseBlock();
            else if (Settings.WORLDEDIT_NOTEBLOCKS.toBool() && noteMechanic != null) {
                return BukkitAdapter.adapt(hasArguments ? parseNoteBlock(noteMechanic, input) : blockData).toBaseBlock();
            } else return null;
        }
    }

    private static BlockData parseNoteBlock(NoteBlockMechanic mechanic, String input) {
        NoteBlockMechanicFactory factory = NoteBlockMechanicFactory.getInstance();
        if (mechanic.isDirectional()) {
            String direction = (input.contains("\\[")) ? input.split("\\[")[1].split("=")[1].split("]")[0] : input;
            DirectionalBlock dirBlock = mechanic.getDirectional();

            if (!dirBlock.isParentBlock()) {
                return factory.createNoteBlockData(dirBlock.getParentBlock());
            } else if (dirBlock.isParentBlock() && !direction.equals(input)) {
                return  factory.createNoteBlockData(getDirectionalID(mechanic, direction));
            } else return factory.createNoteBlockData(mechanic.getItemID());
        } else return factory.createNoteBlockData(mechanic.getItemID());
    }

    private static String getDirectionalID(NoteBlockMechanic mechanic, String direction) {
        DirectionalBlock dirBlock = mechanic.getDirectional();
        if (dirBlock.isLog()) {
            return switch (direction) {
                case "x" -> dirBlock.getXBlock();
                case "y" -> dirBlock.getYBlock();
                case "z" -> dirBlock.getZBlock();
                default -> mechanic.getItemID();
            };
        } else {
            return switch (direction) {
                case "north" -> dirBlock.getNorthBlock();
                case "south" -> dirBlock.getSouthBlock();
                case "west" -> dirBlock.getWestBlock();
                case "east" -> dirBlock.getEastBlock();
                case "up" -> dirBlock.isDropper() ? dirBlock.getUpBlock() : mechanic.getItemID();
                case "down" -> dirBlock.isDropper() ? dirBlock.getDownBlock() : mechanic.getItemID();
                default -> mechanic.getItemID();
            };
        }
    }

    protected static void pasteSchematic(Location loc, File schematic, Boolean replaceBlocks, Boolean shouldCopyBiomes, Boolean shouldCopyEntities) {
        ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(schematic);
        if (clipboardFormat == null) return;
        Clipboard clipboard;

        try (final FileInputStream inputStream = new FileInputStream(schematic); ClipboardReader reader = clipboardFormat.getReader(inputStream)) {
            clipboard = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            World world = loc.getWorld();
            if (world == null) return;
            com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(world);
            EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(adaptedWorld).maxBlocks(-1).build();
            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
                    .to(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()))
                    .copyBiomes(shouldCopyBiomes).copyEntities(shouldCopyEntities).ignoreAirBlocks(true).build();

            try {
                if (replaceBlocks || getBlocksInSchematic(clipboard, loc).isEmpty())
                    Operations.complete(operation);
                editSession.close();
            } catch (WorldEditException e) {
                OraxenPlugin.get().getLogger().warning("Could not paste schematic for sapling-mechanic");
                if (Settings.DEBUG.toBool()) e.printStackTrace();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Block> getBlocksInSchematic(Clipboard clipboard, Location loc) {
        List<Block> list = new ArrayList<>();
        World world = loc.getWorld();
        assert world != null;

        for (int x = clipboard.getMinimumPoint().getX(); x <= clipboard.getMaximumPoint().getX(); x++) {
            for (int y = clipboard.getMinimumPoint().getY(); y <= clipboard.getMaximumPoint().getY(); y++) {
                for (int z = clipboard.getMinimumPoint().getZ(); z <= clipboard.getMaximumPoint().getZ(); z++) {
                    Location offset = new Location(world, x - clipboard.getOrigin().getBlockX(), y - clipboard.getOrigin().getBlockY(), z - clipboard.getOrigin().getBlockZ());

                    Block block = world.getBlockAt(loc.clone().add(offset));
                    if (BlockHelpers.isReplaceable(block)) continue;
                    if (BlockHelpers.toBlockLocation(loc).equals(BlockHelpers.toBlockLocation(loc))) continue;
                    list.add(block);
                }
            }
        }
        return list;
    }

    public static List<Block> getBlocksInSchematic(Location loc, File schematic) {
        List<Block> list = new ArrayList<>();
        World world = loc.getWorld();
        assert world != null;

        ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(schematic);
        if (clipboardFormat == null) return list;
        Clipboard clipboard;

        try (final FileInputStream inputStream = new FileInputStream(schematic); ClipboardReader reader = clipboardFormat.getReader(inputStream)) {
            clipboard = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int x = clipboard.getMinimumPoint().getX(); x <= clipboard.getMaximumPoint().getX(); x++) {
            for (int y = clipboard.getMinimumPoint().getY(); y <= clipboard.getMaximumPoint().getY(); y++) {
                for (int z = clipboard.getMinimumPoint().getZ(); z <= clipboard.getMaximumPoint().getZ(); z++) {
                    Location offset = new Location(world, x - clipboard.getOrigin().getBlockX(), y - clipboard.getOrigin().getBlockY(), z - clipboard.getOrigin().getBlockZ());

                    Block block = world.getBlockAt(loc.clone().add(offset));
                    if (BlockHelpers.isReplaceable(block)) continue;
                    if (BlockHelpers.toBlockLocation(loc).equals(BlockHelpers.toBlockLocation(loc))) continue;
                    list.add(block);
                }
            }
        }

        return list;
    }
}
