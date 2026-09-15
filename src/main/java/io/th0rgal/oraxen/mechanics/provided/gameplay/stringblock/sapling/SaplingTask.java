package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.compatibilities.provided.worldedit.WrappedWorldEdit;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic.SAPLING_KEY;

public class SaplingTask implements Runnable {

    private final int delay;
    private SchedulerUtil.ScheduledTask scheduledTask;

    public SaplingTask(int delay) {
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

    @Override
    public void run() {
        if (!PluginUtils.isEnabled("WorldEdit")) return;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                // Hop to the chunk's region thread before scanning its PDC for Folia compatibility;
                // all blocks in the chunk belong to the same region, so they can be processed inline
                Location chunkLoc = new Location(world, chunk.getX() << 4, 0, chunk.getZ() << 4);
                SchedulerUtil.runAtLocation(chunkLoc, () -> {
                    if (!chunk.isLoaded()) return;
                    for (Block block : BlockHelpers.getBlocksWithCustomData(OraxenPlugin.get(), chunk))
                        processSapling(block);
                });
            }
        }
    }

    private void processSapling(Block block) {
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        if (pdc.has(SAPLING_KEY, PersistentDataType.INTEGER) && block.getType() == Material.TRIPWIRE) {
            StringBlockMechanic string = OraxenBlocks.getStringMechanic(block);
            if (string == null || !string.isSapling()) return;

            SaplingMechanic sapling = string.getSaplingMechanic();
            if (sapling == null || !sapling.hasSchematic()) return;
            if (!sapling.canGrowNaturally()) return;
            if (sapling.requiresWaterSource() && !sapling.isUnderWater(block)) return;
            if (sapling.requiresLight() && block.getLightLevel() < sapling.getMinLightLevel()) return;
            if (!sapling.replaceBlocks() && !WrappedWorldEdit.getBlocksInSchematic(block.getLocation(), sapling.getSchematic()).isEmpty()) return;

            int growthTimeRemains = pdc.getOrDefault(SAPLING_KEY, PersistentDataType.INTEGER, 0) - delay;
            if (growthTimeRemains <= 0) {
                block.setType(Material.AIR, false);
                if (sapling.hasGrowSound())
                    AdventureUtils.playSound(block.getLocation(), sapling.getGrowSound(), Sound.Source.MASTER, 1.0f, 0.8f);
                WrappedWorldEdit.pasteSchematic(block.getLocation(), sapling.getSchematic(), sapling.replaceBlocks(), sapling.copyBiomes(), sapling.copyEntities());
            } else {
                pdc.set(SAPLING_KEY, PersistentDataType.INTEGER, growthTimeRemains);
            }
        } else if (pdc.has(SAPLING_KEY, PersistentDataType.INTEGER) && block.getType() != Material.TRIPWIRE) {
            pdc.remove(SAPLING_KEY);
        }
    }
}
