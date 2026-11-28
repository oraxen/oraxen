package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public static SchematicRegion getSchematicRegion(File schematic) {
        return loaded ? WorldEditUtils.getSchematicRegion(schematic) : null;
    }

    /**
     * The block offsets a schematic covers around the location it is pasted at.
     */
    public record SchematicRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                  List<BlockOffset> occupiedOffsets) {

        public SchematicRegion {
            occupiedOffsets = List.copyOf(occupiedOffsets);
        }

        /**
         * Returns the blocks the schematic would replace, without the paste location itself.
         */
        public List<Block> getBlocks(Location loc) {
            List<Block> blocks = new ArrayList<>();
            if (loc.getWorld() == null) return blocks;
            Block origin = loc.getBlock();
            for (BlockOffset offset : occupiedOffsets) {
                Block block = origin.getRelative(offset.x(), offset.y(), offset.z());
                if (BlockHelpers.isReplaceable(block) || block.equals(origin)) continue;
                blocks.add(block);
            }
            return blocks;
        }

        public record BlockOffset(int x, int y, int z) {}
    }
}
