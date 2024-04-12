package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.api.OraxenBlocks;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;

public class CustomBlockMiningListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageCustomBlock(BlockDamageEvent event) {
        final Block block = event.getBlock();
        final Player player = event.getPlayer();
        final ItemStack itemInHand = event.getItemInHand();

        CustomBlockMechanic mechanic = OraxenBlocks.getCustomBlockMechanic(block.getBlockData());
        if (mechanic == null) return;
        long breakTime = mechanic.breakTime(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageAbort(BlockDamageAbortEvent event) {



    }
}
