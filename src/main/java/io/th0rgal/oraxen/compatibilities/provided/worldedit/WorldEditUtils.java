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
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WorldEditUtils {

    protected static void pasteSchematic(Location loc, File schematic, Boolean replaceBlocks, Boolean shouldCopyBiomes, Boolean shouldCopyEntities) {
        ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(schematic);
        assert clipboardFormat != null;
        ClipboardReader reader;
        try {
            reader = clipboardFormat.getReader(new FileInputStream(schematic));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Clipboard clipboard;
        try {
            clipboard = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            World world = loc.getWorld();
            if (world == null) return;
            com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(world);

            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(adaptedWorld, -1);

            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
                    .to(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()))
                    .copyBiomes(shouldCopyBiomes).copyEntities(shouldCopyEntities).ignoreAirBlocks(true).build();

            try {
                if (replaceBlocks || getBlocksInSchematic(loc, schematic).isEmpty())
                    Operations.complete(operation);
                editSession.close();

            } catch (WorldEditException e) {
                OraxenPlugin.get().getLogger().warning("Could not paste schematic for sapling-mechanic");
                e.printStackTrace();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Block> getBlocksInSchematic(Location loc, File schematic) {
        List<Block> list = new ArrayList<>();
        World world = loc.getWorld();
        if (world == null) return list;

        ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(schematic);
        assert clipboardFormat != null;
        ClipboardReader reader;
        try {
            reader = clipboardFormat.getReader(new FileInputStream(schematic));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Clipboard clipboard;
        try {
            clipboard = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int x = clipboard.getMinimumPoint().getX(); x <= clipboard.getMaximumPoint().getX(); x++) {
            for (int y = clipboard.getMinimumPoint().getY(); y <= clipboard.getMaximumPoint().getY(); y++) {
                for (int z = clipboard.getMinimumPoint().getZ(); z <= clipboard.getMaximumPoint().getZ(); z++) {
                    Block block = world.getBlockAt(x,y,z);
                    if (BlockVector3.at(x, y, z) == clipboard.getOrigin()) continue; // Exclude origin-block
                    if (Utils.REPLACEABLE_BLOCKS.contains(block)) continue; // Skip replacable blocks
                    list.add(block);
                }
            }
        }
        return list;
    }
}
