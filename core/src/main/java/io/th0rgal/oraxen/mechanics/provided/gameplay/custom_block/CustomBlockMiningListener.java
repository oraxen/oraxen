package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class CustomBlockMiningListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageCustomBlock(BlockDamageEvent event) {
        final Block block = event.getBlock();
        final Player player = event.getPlayer();

        CustomBlockMechanic mechanic = OraxenBlocks.getCustomBlockMechanic(block.getBlockData());
        if (mechanic == null || player.getGameMode() == GameMode.CREATIVE) return;

        if (VersionUtil.below("1.20.5")) event.setCancelled(true);
        OraxenPlugin.get().breakerManager().startBlockBreak(player, block, mechanic);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageAbort(BlockDamageAbortEvent event) {
        OraxenPlugin.get().breakerManager().stopBlockBreak(event.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (VersionUtil.below("1.20.5")) return;
        OraxenPlugin.get().breakerManager().stopBlockBreak(event.getPlayer());
    }

    // This has some issues when the client assumes it has broken the block
    // This will then reset the active task and cause it to run whilst the player isnt mining
//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onBlockBreak(BlockBreakEvent event) {
//        OraxenPlugin.get().breakerManager().stopBlockBreak(event.getPlayer());
//    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        OraxenPlugin.get().breakerManager().stopBlockBreak(event.getPlayer());
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        OraxenPlugin.get().breakerManager().stopBlockBreak(event.getPlayer());
    }

    @EventHandler
    public void onDropHand(PlayerDropItemEvent event) {
        OraxenPlugin.get().breakerManager().stopBlockBreak(event.getPlayer());
    }
}
