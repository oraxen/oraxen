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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import io.th0rgal.oraxen.utils.logs.Logs;

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
                Logs.debug(e);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Block> getBlocksInSchematic(Clipboard clipboard, Location loc) {
        return toRegion(clipboard).getBlocks(loc);
    }

    public static List<Block> getBlocksInSchematic(Location loc, File schematic) {
        WrappedWorldEdit.SchematicRegion region = getSchematicRegion(schematic);
        return region != null ? region.getBlocks(loc) : new ArrayList<>();
    }

    @Nullable
    static WrappedWorldEdit.SchematicRegion getSchematicRegion(File schematic) {
        ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(schematic);
        if (clipboardFormat == null) return null;

        try (FileInputStream inputStream = new FileInputStream(schematic);
             ClipboardReader reader = clipboardFormat.getReader(inputStream)) {
            return toRegion(reader.read());
        } catch (IOException e) {
            Logs.logWarning("Could not read the schematic " + schematic.getName());
            Logs.debug(e);
            return null;
        }
    }

    private static WrappedWorldEdit.SchematicRegion toRegion(Clipboard clipboard) {
        BlockVector3 min = clipboard.getMinimumPoint().subtract(clipboard.getOrigin());
        BlockVector3 max = clipboard.getMaximumPoint().subtract(clipboard.getOrigin());
        return new WrappedWorldEdit.SchematicRegion(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }
}
