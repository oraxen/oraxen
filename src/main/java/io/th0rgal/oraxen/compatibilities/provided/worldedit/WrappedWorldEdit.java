package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.sk89q.worldedit.WorldEdit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WrappedWorldEdit {

    private WrappedWorldEdit() {
    }

    public static boolean loaded;

    public static void init() {
        loaded = Bukkit.getPluginManager().isPluginEnabled("WorldEdit") || Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit");
    }

    public static void registerParser() {
        if (loaded) {
            new WorldEditUtils.OraxenBlockInputParser();
            new WorldEditListener();
        }
    }

    public static void unregister() {
        if (loaded) {
            WorldEdit.getInstance().getEventBus().unregister(new WorldEditListener());
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
