package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
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
        loaded = PluginUtils.isEnabled("WorldEdit") || PluginUtils.isEnabled("FastAsyncWorldEdit");
        if (loaded && VersionUtil.atOrAbove("1.21.4")) WorldEditFurniture.register();
    }

    public static void disable() {
        if (loaded && VersionUtil.atOrAbove("1.21.4")) WorldEditFurniture.unregister();
        loaded = false;
    }

    public static void pasteSchematic(Location loc, File schematic, Boolean replaceBlocks, Boolean shouldCopyBiomes, Boolean shouldCopyEntities) {
        if (loaded) WorldEditUtils.pasteSchematic(loc, schematic, replaceBlocks, shouldCopyBiomes, shouldCopyEntities);
    }

    public static List<Block> getBlocksInSchematic(Location loc, File schematic) {
        if (loaded) return WorldEditUtils.getBlocksInSchematic(loc, schematic);
        else return new ArrayList<>();
    }
}
