package io.th0rgal.oraxen.mechanics.provided.farming.harvesting;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.protection.AntiGriefLib;
import io.th0rgal.oraxen.utils.timers.Timer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class HarvestingMechanicListener implements Listener {

    private final MechanicFactory factory;
    private final ThreadLocal<HarvestContext> activeHarvest = new ThreadLocal<>();

    public HarvestingMechanicListener(final MechanicFactory factory) {
        this.factory = factory;
    }

    private static List<Block> getNearbyBlocks(final Location location, final int radius, final int height) {
        final List<Block> blocks = new ArrayList<>();
        for (int x = location.getBlockX() - Math.floorDiv(radius, 2); x <= location.getBlockX()
                + Math.floorDiv(radius, 2); x++)
            for (int y = location.getBlockY() - Math.floorDiv(height, 2); y <= location.getBlockY()
                    + Math.floorDiv(height, 2); y++)
                for (int z = location.getBlockZ() - Math.floorDiv(radius, 2); z <= location.getBlockZ()
                        + Math.floorDiv(radius, 2); z++)
                    blocks.add(Objects.requireNonNull(location.getWorld()).getBlockAt(x, y, z));
        return blocks;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Block clickedBlock = event.getClickedBlock();
        final ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        final String itemID = OraxenItems.getIdByItem(item);
        final Player player = event.getPlayer();
        final HarvestingMechanic mechanic = (HarvestingMechanic) factory.getMechanic(itemID);

        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if (clickedBlock == null || factory.isNotImplementedIn(itemID) || mechanic == null) return;

        final Timer playerTimer = mechanic.getTimer(player);

        if (!playerTimer.isFinished()) {
            mechanic.getTimer(player).sendToPlayer(player);
            return;
        }

        playerTimer.reset();
        List<Block> nearbyBlocks = getNearbyBlocks(clickedBlock.getLocation(), mechanic.getRadius(), mechanic.getHeight());
        for (final Block block : nearbyBlocks) {
            if (block.getBlockData() instanceof Ageable ageable
                    && ageable.getAge() == ageable.getMaximumAge()
                    && AntiGriefLib.canBreak(player, block.getLocation())
                    && AntiGriefLib.canBuild(player, block.getLocation())) {
                if (!breakAndCollect(player, block)) continue;
                ageable.setAge(0);
                block.setBlockData(ageable);
            }
        }
    }

    boolean breakAndCollect(Player player, Block block) {
        activeHarvest.set(new HarvestContext(player.getUniqueId(), block.getLocation()));
        try {
            return player.breakBlock(block);
        } finally {
            activeHarvest.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        HarvestContext context = activeHarvest.get();
        if (context == null || !context.playerId().equals(event.getPlayer().getUniqueId())
                || !context.location().equals(event.getBlockState().getLocation())) return;

        List<Item> itemEntities = event.getItems();
        List<ItemStack> drops = itemEntities.stream().map(Item::getItemStack).map(ItemStack::clone).toList();
        itemEntities.clear();

        for (ItemStack drop : drops) {
            Map<Integer, ItemStack> overflow = event.getPlayer().getInventory().addItem(drop);
            for (ItemStack remaining : overflow.values())
                event.getPlayer().getWorld().dropItemNaturally(context.location(), remaining);
        }
    }

    private record HarvestContext(UUID playerId, Location location) {
    }
}
