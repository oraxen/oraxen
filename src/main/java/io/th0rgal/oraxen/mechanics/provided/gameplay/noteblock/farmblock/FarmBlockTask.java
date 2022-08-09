package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class FarmBlockTask extends BukkitRunnable {

    private final int delay;

    public FarmBlockTask(int delay) {
        this.delay = delay;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Block block : CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(), chunk)) {
                    PersistentDataContainer customBlockData = new CustomBlockData(block, OraxenPlugin.get());
                    if (customBlockData.has(FARMBLOCK_KEY, PersistentDataType.STRING)) {
                        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
                        if (mechanic != null && mechanic.hasDryout()) {
                            Block blockBelow = block.getRelative(BlockFace.DOWN);
                            FarmBlockDryout farmMechanic = mechanic.getDryout();

                            if (customBlockData.has(FARMBLOCK_KEY, PersistentDataType.INTEGER)) {
                                int moistTimerRemain = customBlockData.get(FARMBLOCK_KEY, PersistentDataType.INTEGER) + delay;

                                if (blockBelow.getType() == Material.WATER) {
                                    customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
                                    return;
                                }

                                if (farmMechanic.getDryoutTime() - moistTimerRemain <= 0) {
                                    NoteBlockMechanicFactory.setBlockModel(block, farmMechanic.getFarmBlock());
                                    customBlockData.remove(FARMBLOCK_KEY);
                                    customBlockData.set(FARMBLOCK_KEY, PersistentDataType.STRING, farmMechanic.getFarmBlock());
                                } else customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, moistTimerRemain);
                            }

                            else {
                                boolean isWet = farmMechanic.isMoistFarmBlock() && farmMechanic.isConnectedToWaterSource(block, customBlockData);
                                boolean rainingAtBlock = world.hasStorm() && world.getHighestBlockAt(block.getLocation()) == block;
                                if (isWet || rainingAtBlock) {
                                    NoteBlockMechanicFactory.setBlockModel(block, farmMechanic.getMoistFarmBlock());
                                    customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
                                }
                            }
                        }
                        // Remove leftover farmblock pdcs if theyre not a farmblock anymore
                        // Also covers blocks that arent even custom blocks
                        else if (mechanic == null || !mechanic.hasDryout()) {
                            customBlockData.remove(FARMBLOCK_KEY);
                        }
                    }
                }
            }
        }
    }
}
