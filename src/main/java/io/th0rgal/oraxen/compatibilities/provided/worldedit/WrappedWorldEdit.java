package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;

public class WrappedWorldEdit {

    private static boolean loaded;

    public static void init() {
        loaded = Bukkit.getPluginManager().isPluginEnabled("WorldEdit");
    }

    public static void pasteSchematic(Location loc, File schematic, Boolean shouldCopyBiomes, Boolean shouldCopyEntities) {
        if (loaded) WorldEditUtils.pasteSchematic(loc, schematic, shouldCopyBiomes, shouldCopyEntities);
    }
}
