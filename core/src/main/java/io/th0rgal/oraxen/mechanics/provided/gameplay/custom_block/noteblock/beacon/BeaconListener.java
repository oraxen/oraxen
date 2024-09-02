package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.beacon;

import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BeaconListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void o(BeaconEffectEvent event) {
        if (!checkBeaconPyramid(event.getBlock())) event.setCancelled(true);
    }

    private static boolean checkBeaconPyramid(Block block) {
        if (!(block.getState() instanceof Beacon beacon)) return false;

        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        World world = block.getWorld();

        boolean validPyramid = true;
        for (int tier = 1; tier <= beacon.getTier(); tier++) {
            int tierY = y - tier;

            if (tierY < world.getMinHeight()) break;


            boolean validTier = true;
            for (int tierX = x - tier; tierX <= x + tier; ++tierX) {
                for (int tierZ = z - tier; tierZ <= z + tier; ++tierZ) {
                    Location tierBlockLoc = new Location(world, tierX, tierY, tierZ);
                    BlockData blockData = world.getBlockData(tierBlockLoc);
                    if (!Tag.BEACON_BASE_BLOCKS.isTagged(blockData.getMaterial())) {
                        validTier = false;
                        break;
                    }

                    NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(blockData);
                    if (mechanic != null && !mechanic.isBeaconBaseBlock()) {
                        validTier = false;
                        break;
                    }
                }
            }

            if (!validTier) {
                validPyramid = false;
                break;
            }
        }

        return validPyramid;
    }
}
