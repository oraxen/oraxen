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

import java.util.ArrayList;
import java.util.List;

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
            for (Location target : targets(player, origin, mechanic)) {
                if (Bukkit.isOwnedByCurrentRegion(target))
                    breakBlock(player, target.getBlock(), item);
                else
                    breakOnBlockRegion(player, target, item.clone());
            }
        } finally {
            activeMining.remove();
        }
    }

    private static List<Location> targets(Player player, Block origin, MiningMechanic mechanic) {
        if (!mechanic.isFaceRelative()) return worldTargets(origin.getLocation(), mechanic.getOffsets());
        return mechanic.faceTargets(origin.getLocation(),
                MiningMechanic.lookingDirection(player.getEyeLocation().getDirection()));
    }

    private static List<Location> worldTargets(Location origin, List<MiningMechanic.Offset> offsets) {
        List<Location> targets = new ArrayList<>();
        for (MiningMechanic.Offset offset : offsets) {
            if (offset.x() == 0 && offset.y() == 0 && offset.z() == 0) continue;
            targets.add(origin.clone().add(offset.x(), offset.y(), offset.z()));
        }
        return targets;
    }

    private void breakOnBlockRegion(Player player, Location target, ItemStack tool) {
        SchedulerUtil.runAtLocation(target, () -> {
            Block block = target.getBlock();
            if (!canDamage(block)) return;
            // Protection plugins read the player, so that check stays on the player's region.
            SchedulerUtil.runForEntity(player, () -> {
                if (!player.isOnline() || !AntiGriefLib.canBreak(player, target)) return;
                SchedulerUtil.runAtLocation(target, () -> {
                    activeMining.set(true);
                    try {
                        damageBlock(player, block, tool);
                    } finally {
                        activeMining.remove();
                    }
                });
            }, null);
        });
    }

    private void breakBlock(Player player, Block block, ItemStack itemStack) {
        if (!canDamage(block) || !AntiGriefLib.canBreak(player, block.getLocation())) return;
        damageBlock(player, block, itemStack);
    }

    private void damageBlock(Player player, Block block, ItemStack itemStack) {
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

    private static boolean canDamage(Block block) {
        return !block.isLiquid() && !BlockHelpers.UNBREAKABLE_BLOCKS.contains(block.getType());
    }
}
