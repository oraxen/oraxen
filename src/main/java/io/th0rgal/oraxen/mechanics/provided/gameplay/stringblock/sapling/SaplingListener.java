package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling;

import io.th0rgal.oraxen.compatibilities.provided.worldedit.WrappedWorldEdit;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener.getStringMechanic;

public class SaplingListener implements Listener {

    @EventHandler
    public void onBoneMeal(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        if (block == null || item == null || item.getType() != Material.BONE_MEAL) return;
        StringBlockMechanic mechanic = getStringMechanic(block);
        if (mechanic == null || !mechanic.isSapling()) return;
        SaplingMechanic sapling = mechanic.getSaplingMechanic();
        if (sapling == null || !sapling.hasSchematic()) return;
        if (sapling.requiresLight() && sapling.getMinLightLevel() > block.getLightLevel()) return;
        if (sapling.requiresWaterSource() && sapling.isInWater(block)) return;
        if (!sapling.canGrowFromBoneMeal()) return;
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) return;

        double random = Math.random() * 100;
        if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
        block.getWorld().spawnParticle(Particle.COMPOSTER, block.getLocation().add(0.5, 0.2, 0.5), 10);
        if (random < sapling.getBoneMealGrowChance()) {
            block.setType(Material.AIR, false);
            if (sapling.hasGrowSound())
                block.getWorld().playSound(block.getLocation(), sapling.getGrowSound(), 1.0f, 0.8f);
            WrappedWorldEdit.pasteSchematic(block.getLocation(), sapling.getSchematic(), sapling.copyBiomes(), sapling.copyEntities());
        }
    }
}


