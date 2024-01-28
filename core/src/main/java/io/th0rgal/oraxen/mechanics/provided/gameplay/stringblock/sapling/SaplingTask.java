package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.compatibilities.provided.worldedit.WrappedWorldEdit;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic.SAPLING_KEY;

public class SaplingTask extends BukkitRunnable {

    private final int delay;

    public SaplingTask(int delay) {
        this.delay = delay;
    }

    @Override
    public void run() {
        if (!PluginUtils.isEnabled("WorldEdit")) return;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Block block : CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(), chunk)) {
                    PersistentDataContainer pdc = BlockHelpers.getPDC(block);
                    if (pdc.has(SAPLING_KEY, PersistentDataType.INTEGER) && block.getType() == Material.TRIPWIRE) {
                        StringBlockMechanic string = OraxenBlocks.getStringMechanic(block);
                        if (string == null || !string.isSapling()) return;

                        SaplingMechanic sapling = string.getSaplingMechanic();
                        if (sapling == null || !sapling.hasSchematic()) continue;
                        if (!sapling.canGrowNaturally()) continue;
                        if (sapling.requiresWaterSource() && !sapling.isUnderWater(block)) continue;
                        if (sapling.requiresLight() && block.getLightLevel() < sapling.getMinLightLevel()) continue;
                        if (!sapling.replaceBlocks() && !WrappedWorldEdit.getBlocksInSchematic(block.getLocation(), sapling.getSchematic()).isEmpty()) continue;

                        int growthTimeRemains = pdc.getOrDefault(SAPLING_KEY, PersistentDataType.INTEGER, 0) - delay;
                        if (growthTimeRemains <= 0) {
                            block.setType(Material.AIR, false);
                            if (sapling.hasGrowSound())
                                block.getWorld().playSound(block.getLocation(), sapling.getGrowSound(), 1.0f, 0.8f);
                            WrappedWorldEdit.pasteSchematic(block.getLocation(), sapling.getSchematic(), sapling.replaceBlocks(), sapling.copyBiomes(), sapling.copyEntities());
                        } else pdc.set(SAPLING_KEY, PersistentDataType.INTEGER, growthTimeRemains);
                    }
                    else if (pdc.has(SAPLING_KEY, PersistentDataType.INTEGER) && block.getType() != Material.TRIPWIRE) {
                        pdc.remove(SAPLING_KEY);
                    }
                }
            }
        }
    }
}
