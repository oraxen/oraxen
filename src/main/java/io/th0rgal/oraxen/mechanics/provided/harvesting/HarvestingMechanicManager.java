package io.th0rgal.oraxen.mechanics.provided.harvesting;

import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.compatibilities.provided.worldguard.WorldGuardCompatibility;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HarvestingMechanicManager implements Listener {

    private final MechanicFactory factory;
    private final WorldGuardCompatibility worldGuardCompatibility;

    public HarvestingMechanicManager(MechanicFactory factory) {
        this.factory = factory;
        if (CompatibilitiesManager.isCompatibilityEnabled("WorldGuard"))
            worldGuardCompatibility = (WorldGuardCompatibility) CompatibilitiesManager
                    .getActiveCompatibility("WorldGuard");
        else
            worldGuardCompatibility = null;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getClickedBlock() == null)
            return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item == null)
            return;

        String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID))
            return;

        HarvestingMechanic mechanic = (HarvestingMechanic) factory.getMechanic(itemID);

        Player player = event.getPlayer();

        for (Block block : getNearbyBlocks(event.getClickedBlock().getLocation(), mechanic.getRadius(),
                mechanic.getHeight())) {
            if (block.getBlockData() instanceof Ageable) {
                if (worldGuardCompatibility != null && !worldGuardCompatibility.canBreak(player, block))
                    return;
                Ageable ageable = (Ageable) block.getBlockData();
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    ageable.setAge(0);
                    block.setBlockData(ageable);
                    List<ItemStack> drops = new ArrayList<>();
                    switch (block.getType()) {
                        case WHEAT:
                            drops.add(new ItemStack(Material.WHEAT));
                            drops.add(new ItemStack(Material.WHEAT_SEEDS));
                            break;
                        case BEETROOTS:
                            drops.add(new ItemStack(Material.BEETROOT));
                            drops.add(new ItemStack(Material.BEETROOT_SEEDS));
                            break;
                        default:
                            drops.addAll(block.getDrops());
                    }
                    for (ItemStack itemStack : drops)
                        giveItem(player, itemStack);
                }
            }
        }

    }

    private static List<Block> getNearbyBlocks(Location location, int radius, int height) {
        List<Block> blocks = new ArrayList<>();
        for (int x = location.getBlockX() - Math.floorDiv(radius, 2); x <= location.getBlockX()
                + Math.floorDiv(radius, 2); x++) {
            for (int y = location.getBlockY() - Math.floorDiv(height, 2); y <= location.getBlockY()
                    + Math.floorDiv(height, 2); y++)
                for (int z = location.getBlockZ() - Math.floorDiv(radius, 2); z <= location.getBlockZ()
                        + Math.floorDiv(radius, 2); z++) {
                    blocks.add(Objects.requireNonNull(location.getWorld()).getBlockAt(x, y, z));
                }
        }
        return blocks;
    }

    private void giveItem(HumanEntity humanEntity, ItemStack item) {
        if (humanEntity.getInventory().firstEmpty() != -1)
            humanEntity.getInventory().addItem(item);
        else
            humanEntity.getWorld().dropItem(humanEntity.getLocation(), item);
    }
}
