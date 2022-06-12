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
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WorldEditUtils {

    protected static void pasteSchematic(Location loc, File schematic) {
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
            com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(loc.getWorld());

            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(adaptedWorld, -1);

            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
                    .to(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ())).ignoreAirBlocks(true).build();

            try {
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
}
