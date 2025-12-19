package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;

public class FarmBlockTask implements Runnable {
    private final int delay;
    private SchedulerUtil.ScheduledTask scheduledTask;

    public FarmBlockTask(int delay) {
        this.delay = delay;
    }

    public SchedulerUtil.ScheduledTask start(long initialDelay, long period) {
        scheduledTask = SchedulerUtil.runTaskTimer(initialDelay, period, this);
        return scheduledTask;
    }

    public void cancel() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    private boolean isAreaWet(FarmBlockDryout mechanic, Block block, PersistentDataContainer pdc) {
        boolean nearWater = mechanic.isConnectedToWaterSource(block, pdc);
        boolean rainingAtBlock = block.getWorld().hasStorm() && block.getWorld().getHighestBlockAt(block.getLocation()) == block;
        return nearWater || rainingAtBlock;
    }

    private void updateBlockModel(Block block, PersistentDataContainer pdc, String model) {
        pdc.remove(FARMBLOCK_KEY);
        NoteBlockMechanicFactory.setBlockModel(block, model);
    }

    private void updateBlock(Block block, PersistentDataContainer pdc) {
        if (pdc.getKeys().contains(FARMBLOCK_KEY)) {
            NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
            if (mechanic != null && mechanic.hasDryout()) {
                FarmBlockDryout farmMechanic = mechanic.getDryout();

                if (isAreaWet(farmMechanic, block, pdc)) {
                    if (pdc.has(FARMBLOCK_KEY, PersistentDataType.STRING))
                        updateBlockModel(block, pdc, farmMechanic.getMoistFarmBlock());
                    pdc.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
                } else if (pdc.has(FARMBLOCK_KEY, PersistentDataType.INTEGER)) {
                    int moistTimerRemain = pdc.getOrDefault(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0) + delay;
                    if (farmMechanic.getDryoutTime() - moistTimerRemain <= 0) {
                        updateBlockModel(block, pdc, farmMechanic.getFarmBlock());
                        pdc.set(FARMBLOCK_KEY, PersistentDataType.STRING, farmMechanic.getFarmBlock());
                    } else
                        pdc.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, moistTimerRemain);
                }
            } else //Remove FARMBLOCK_KEY from pdc if the block has no (longer a) dryout mechanic
                pdc.remove(FARMBLOCK_KEY);
        }
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds())
            for (Chunk chunk : world.getLoadedChunks())
                CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(), chunk).forEach(block ->
                        updateBlock(block, BlockHelpers.getPDC(block)));
    }
}
