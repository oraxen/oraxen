package io.th0rgal.oraxen.mechanics.provided.farming.mining;

import io.th0rgal.oraxen.protection.AntiGriefLib;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class MiningMechanicListener implements Listener {

    private final MiningMechanicFactory factory;
    private final ThreadLocal<Boolean> activeMining = new ThreadLocal<>();

    public MiningMechanicListener(MiningMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (Boolean.TRUE.equals(activeMining.get())) return;
        activeMining.set(true);
        try {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            MiningMechanic mechanic = (MiningMechanic) factory.getMechanic(item);
            if (mechanic == null) return;

            Block origin = event.getBlock();
            for (MiningMechanic.Offset offset : mechanic.getOffsets()) {
                if (offset.x() == 0 && offset.y() == 0 && offset.z() == 0)
                    continue;
                Location target = origin.getLocation().clone().add(offset.x(), offset.y(), offset.z());
                if (Bukkit.isOwnedByCurrentRegion(target))
                    breakBlock(player, target.getBlock(), item);
                else {
                    ItemStack tool = item.clone();
                    SchedulerUtil.runAtLocation(target, () -> {
                        activeMining.set(true);
                        try {
                            breakBlock(player, target.getBlock(), tool);
                        } finally {
                            activeMining.remove();
                        }
                    });
                }
            }
        } finally {
            activeMining.remove();
        }
    }

    private void breakBlock(Player player, Block block, ItemStack itemStack) {
        if (block.isLiquid()
                || BlockHelpers.UNBREAKABLE_BLOCKS.contains(block.getType())
                || !AntiGriefLib.canBreak(player, block.getLocation()))
            return;
        if (factory.callEvents()) {
            BlockBreakEvent event = new BlockBreakEvent(block, player);
            if (!event.callEvent()) return;
            if (!event.isDropItems()) {
                block.setType(Material.AIR);
                return;
            }
        }
        block.breakNaturally(itemStack, true);
    }
}
