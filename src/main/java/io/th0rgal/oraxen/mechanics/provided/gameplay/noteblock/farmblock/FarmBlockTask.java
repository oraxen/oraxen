package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class FarmBlockTask extends BukkitRunnable {

    private final NoteBlockMechanicFactory factory;
    private final int delay;

    public FarmBlockTask(NoteBlockMechanicFactory factory, int delay) {
        this.factory = factory;
        this.delay = delay;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Block block : CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(), chunk)) {
                    PersistentDataContainer customBlockData = new CustomBlockData(block, OraxenPlugin.get());
                    if (customBlockData.has(FARMBLOCK_KEY, PersistentDataType.INTEGER) && block.getType() == Material.NOTE_BLOCK) {
                        FarmBlockDryout farmMechanic = getNoteBlockMechanic(block).getDryout();

                        int moistTimerRemain = customBlockData.get(FARMBLOCK_KEY, PersistentDataType.INTEGER) + delay;

                        if (farmMechanic.getDryoutTime() - moistTimerRemain <= 0) {
                            NoteBlockMechanicFactory.setBlockModel(block, farmMechanic.getFarmBlock());
                            customBlockData.remove(FARMBLOCK_KEY);
                        } else customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, moistTimerRemain);
                    }
                }
            }
        }
    }
}
