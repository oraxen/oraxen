package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WorldEditUtils {

    private WorldEditUtils() {}

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
        return collectBlocksFromClipboard(clipboard, loc);
    }

    public static List<Block> getBlocksInSchematic(Location loc, File schematic) {
        World world = loc.getWorld();
        if (world == null) return new ArrayList<>();

        ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(schematic);
        if (clipboardFormat == null) return new ArrayList<>();

        try (FileInputStream inputStream = new FileInputStream(schematic);
             ClipboardReader reader = clipboardFormat.getReader(inputStream)) {
            Clipboard clipboard = reader.read();
            return collectBlocksFromClipboard(clipboard, loc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Block> collectBlocksFromClipboard(Clipboard clipboard, Location loc) {
        List<Block> list = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return list;

        BlockVector3 min = clipboard.getMinimumPoint();
        BlockVector3 max = clipboard.getMaximumPoint();
        BlockVector3 origin = clipboard.getOrigin();

        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    Location offset = new Location(world, x - origin.x(), y - origin.y(), z - origin.z());
                    Block block = world.getBlockAt(loc.clone().add(offset));
                    if (BlockHelpers.isReplaceable(block)) continue;
                    if (loc.toBlockLocation().equals(block.getLocation().toBlockLocation())) continue;
                    list.add(block);
                }
            }
        }
        return list;
    }
}
