package io.th0rgal.oraxen.compatibilities.provided.worldedit;

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

    public static void pasteSchematic(Location loc, File schematic, Boolean replaceBlocks, Boolean shouldCopyBiomes, Boolean shouldCopyEntities) {
        if (loaded) WorldEditUtils.pasteSchematic(loc, schematic, replaceBlocks, shouldCopyBiomes, shouldCopyEntities);
    }

    public static List<Block> getBlocksInSchematic(Location loc, File schematic) {
        if (loaded) return WorldEditUtils.getBlocksInSchematic(loc, schematic);
        else return new ArrayList<>();
    }
}
