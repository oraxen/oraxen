package io.th0rgal.oraxen.compatibilities.provided.lightapi;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;

public class WrappedLightAPI {

    public static boolean lightApiEnabled;
    public static boolean lighterApiEnabled;

    static {
        lightApiEnabled = PluginUtils.isEnabled("LightAPI");
        lighterApiEnabled = PluginUtils.isEnabled("LighterAPI");
    }

    private WrappedLightAPI() {
    }

    private static boolean loaded;

    public static void init() {
        if (lightApiEnabled) loaded = true;
        else if (lighterApiEnabled) loaded = true;

        if (loaded) Bukkit.getPluginManager().registerEvents(new LightApiListener(), OraxenPlugin.get());
    }

    public static void createBlockLight(Location location, int value) {
        if (loaded) LightApiUtils.createBlockLight(location, value);
    }

    public static void removeBlockLight(Location location) {
        if (loaded) LightApiUtils.removeBlockLight(location);
    }


    public static class LightApiListener implements Listener {
        private final Set<BlockFace> blockFaces = Set.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

        @EventHandler
        public void onBlockBreak(OraxenNoteBlockBreakEvent event) {
            updateLight(event.getBlock());
        }

        @EventHandler
        public void onBlockBreak(OraxenStringBlockBreakEvent event) {
            updateLight(event.getBlock());
        }

        public void updateLight(Block block) {
            // Remove all blockLight from relative blocks
            removeSurroundingLight(block);
            // Then update light for all blocks around the adjacent blocks
            for (BlockFace blockFace : blockFaces) {
                Block relative = block.getRelative(blockFace);
                if (!relative.equals(block)) updateLight(relative);
                for (BlockFace blockFace2 : blockFaces) {
                    relative = block.getRelative(blockFace).getRelative(blockFace2);
                    if (!relative.equals(block)) updateLight(relative);
                }
            }
        }

        private void removeSurroundingLight(Block block) {
            for (BlockFace blockFace : blockFaces) removeBlockLight(block.getRelative(blockFace).getLocation());
        }

        private void fixLight(Block block) {
            NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(block);
            StringBlockMechanic stringMechanic = OraxenBlocks.getStringMechanic(block);

            if (noteMechanic != null && noteMechanic.hasLight())
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> createBlockLight(block.getLocation(), noteMechanic.getLight()), 1);
            else if (stringMechanic != null && stringMechanic.hasLight())
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> createBlockLight(block.getLocation(), stringMechanic.getLight()), 1);
        }

    }
}
