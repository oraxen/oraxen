package io.th0rgal.oraxen.mechanics.provided.farming.harvesting;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundGroup;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HarvestingMechanicListener implements Listener {

    private final MechanicFactory factory;

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
        int durabilityDamage = 0;
        List<Block> nearbyBlocks = getNearbyBlocks(clickedBlock.getLocation(), mechanic.getRadius(), mechanic.getHeight());
        for (final Block block : nearbyBlocks) {
            if (block.getBlockData() instanceof Ageable ageable
                    && ageable.getAge() == ageable.getMaximumAge()
                    && ProtectionLib.canBreak(player, block.getLocation())
                    && ProtectionLib.canBuild(player, block.getLocation())) {
                ageable.setAge(0);
                block.setBlockData(ageable);
                SoundGroup soundGroup = block.getBlockData().getSoundGroup();
                block.getWorld().playSound(block.getLocation(), soundGroup.getBreakSound(), soundGroup.getVolume(), soundGroup.getPitch());
                final List<ItemStack> drops = new ArrayList<>();
                switch (block.getType()) {
                    case WHEAT -> {
                        drops.add(new ItemStack(Material.WHEAT));
                        drops.add(new ItemStack(Material.WHEAT_SEEDS));
                    }
                    case BEETROOTS -> {
                        drops.add(new ItemStack(Material.BEETROOT));
                        drops.add(new ItemStack(Material.BEETROOT_SEEDS));
                    }
                    default -> drops.addAll(block.getDrops());
                }
                for (final ItemStack itemStack : drops)
                    giveItem(player, itemStack, clickedBlock.getLocation());
                durabilityDamage++;
            }
        }

        if (mechanic.shouldLowerItemDurability() && item.getItemMeta() instanceof Damageable && durabilityDamage > 0) {
            EventUtils.callEvent(new PlayerItemDamageEvent(player, item, durabilityDamage));
        }
    }

    private void giveItem(final Player player, final ItemStack item, final Location location) {
        if (player.getInventory().firstEmpty() != -1) {
            for (Map.Entry<Integer, ItemStack> itemStack : player.getInventory().addItem(item).entrySet())
                player.getWorld().dropItem(player.getLocation(), itemStack.getValue());
        } else player.getWorld().dropItemNaturally(location, item);
    }
}
