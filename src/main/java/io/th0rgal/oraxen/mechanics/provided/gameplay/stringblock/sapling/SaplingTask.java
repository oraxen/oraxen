package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.worldedit.WrappedWorldEdit;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener.getStringMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic.SAPLING_KEY;

public class SaplingTask extends BukkitRunnable {

    private final StringBlockMechanicFactory factory;
    private final int delay;

    public SaplingTask(StringBlockMechanicFactory factory, int delay) {
        this.factory = factory;
        this.delay = delay;
    }

    @Override
    public void run() {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) return;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Block block : CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(), chunk)) {
                    PersistentDataContainer pdc = new CustomBlockData(block, OraxenPlugin.get());
                    if (pdc.has(SAPLING_KEY, PersistentDataType.STRING) && block.getType() == Material.TRIPWIRE) {
                        StringBlockMechanic string = getStringMechanic(block);
                        if (string == null || !string.isSapling()) return;
                        SaplingMechanic sapling = string.getSaplingMechanic();

                        if (!sapling.canGrowNaturally()) continue;
                        if (!sapling.hasSchematic()) continue;
                        if (sapling.requiresWaterSource() && !sapling.isInWater(block)) continue;
                        if (sapling.requiresLight() && block.getLightLevel() < sapling.getMinLightLevel()) continue;

                        if (pdc.has(SAPLING_KEY, PersistentDataType.INTEGER)) {
                            int growthTimeRemains = pdc.get(SAPLING_KEY, PersistentDataType.INTEGER) + delay;

                            if (sapling.getNaturalGrowthTime() - growthTimeRemains <= 0) {
                                block.setType(Material.AIR, false);
                                if (sapling.hasGrowSound())
                                    block.getWorld().playSound(block.getLocation(), sapling.getGrowSound(), 1.0f, 0.8f);
                                WrappedWorldEdit.pasteSchematic(block.getLocation(), sapling.getSchematic());
                            }
                            else pdc.set(SAPLING_KEY, PersistentDataType.INTEGER, growthTimeRemains);
                        }
                        else pdc.set(SAPLING_KEY, PersistentDataType.INTEGER, 0);
                    }
                }
            }
        }
    }
}
