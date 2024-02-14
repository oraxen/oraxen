package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.compatibilities.provided.worldedit.WrappedWorldEdit;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.PluginUtils;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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

        Location loc = block.getLocation();
        StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
        if (mechanic == null || !mechanic.isSapling()) return;

        SaplingMechanic sapling = mechanic.getSaplingMechanic();
        if (sapling == null || !sapling.hasSchematic()) return;
        if (sapling.requiresLight() && sapling.getMinLightLevel() > block.getLightLevel()) return;
        if (sapling.requiresWaterSource() && sapling.isUnderWater(block)) return;
        if (!sapling.canGrowFromBoneMeal()) return;
        if (!PluginUtils.isEnabled("WorldEdit")) return;
        if (!sapling.replaceBlocks() && !WrappedWorldEdit.getBlocksInSchematic(loc, sapling.getSchematic()).isEmpty()) return;

        if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
        block.getWorld().playEffect(loc, Effect.BONE_MEAL_USE, 3);

        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        int growthTimeRemains = pdc.getOrDefault(SAPLING_KEY, PersistentDataType.INTEGER, 0) - sapling.getBoneMealGrowthSpeedup();
        if (growthTimeRemains <= 0) {
            block.setType(Material.AIR, false);
            if (sapling.hasGrowSound())
                player.playSound(loc, sapling.getGrowSound(), 1.0f, 0.8f);
            WrappedWorldEdit.pasteSchematic(loc, sapling.getSchematic(), sapling.replaceBlocks(), sapling.copyBiomes(), sapling.copyEntities());
        } else pdc.set(SAPLING_KEY, PersistentDataType.INTEGER, growthTimeRemains);
    }
}


