package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;

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
                for (Block block : CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(),chunk)) {
                    PersistentDataContainer customBlock = new CustomBlockData(block, OraxenPlugin.get());
                    if (customBlock.has(FARMBLOCK_KEY, PersistentDataType.STRING)) {
                        Bukkit.broadcastMessage("i has le farmblock key");
                    } else Bukkit.broadcastMessage("no");
                }
            }
        }
    }
}
