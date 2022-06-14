package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.worldedit.WorldEditUtils;
import io.th0rgal.oraxen.compatibilities.provided.worldedit.WrappedWorldEdit;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener.getStringMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic.SAPLING_KEY;

public class SaplingListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBoneMeal(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        if (block == null  || block.getType() != Material.TRIPWIRE) return;
        if (item == null || item.getType() != Material.BONE_MEAL) return;

        StringBlockMechanic mechanic = getStringMechanic(block);
        if (mechanic == null || !mechanic.isSapling()) return;

        SaplingMechanic sapling = mechanic.getSaplingMechanic();
        if (sapling == null || !sapling.hasSchematic()) return;
        if (sapling.requiresLight() && sapling.getMinLightLevel() > block.getLightLevel()) return;
        if (sapling.requiresWaterSource() && sapling.isInWater(block)) return;
        if (!sapling.canGrowFromBoneMeal()) return;
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) return;
        if (!sapling.replaceBlocks() && !WorldEditUtils.getBlocksInSchematic(block.getLocation(), sapling.getSchematic()).isEmpty()) return;

        if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
        block.getWorld().spawnParticle(Particle.COMPOSTER, block.getLocation().add(0.5, 0.2, 0.5), 10);

        PersistentDataContainer pdc = new CustomBlockData(block, OraxenPlugin.get());
        int growthTimeRemains = pdc.get(SAPLING_KEY, PersistentDataType.INTEGER) - sapling.getBoneMealGrowthSpeedup();
        if (growthTimeRemains <= 0) {
            block.setType(Material.AIR, false);
            if (sapling.hasGrowSound())
                player.playSound(block.getLocation(), sapling.getGrowSound(), 1.0f, 0.8f);
            WrappedWorldEdit.pasteSchematic(block.getLocation(), sapling.getSchematic(), sapling.replaceBlocks(), sapling.copyBiomes(), sapling.copyEntities());
        } else pdc.set(SAPLING_KEY, PersistentDataType.INTEGER, growthTimeRemains);
    }
}


