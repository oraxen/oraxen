package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.sk89q.worldedit.WorldEdit;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WrappedWorldEdit {

    private static boolean loaded;

    public static void init() {
        loaded = Bukkit.getPluginManager().isPluginEnabled("WorldEdit");
    }

    public static void registerParser() {
        if (loaded) {
            WorldEdit.getInstance().getBlockFactory().register(new WorldEditUtils.OraxenBlockInputParser());
            try {
                // Try and load class, if it fails it is not Paper server so don't register event
                Class.forName("com.destroystokyo.paper.event.server.AsyncTabCompleteEvent");
                Bukkit.getPluginManager().registerEvents(new WorldEditListener(), OraxenPlugin.get());
            }
            catch (ClassNotFoundException ignored) {
            }
        }
    }

    public static void pasteSchematic(Location loc, File schematic, Boolean replaceBlocks, Boolean shouldCopyBiomes, Boolean shouldCopyEntities) {
        if (loaded) WorldEditUtils.pasteSchematic(loc, schematic, replaceBlocks, shouldCopyBiomes, shouldCopyEntities);
    }

    public static List<Block> getBlocksInSchematic(Location loc, File schematic) {
        if (loaded) return WorldEditUtils.getBlocksInSchematic(loc, schematic);
        else return new ArrayList<>();
    }
}
