package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import de.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.*;

public class FurnitureListener implements Listener {

    private final MechanicFactory factory;

    public FurnitureListener(final MechanicFactory factory) {
        this.factory = factory;
        BreakerSystem.MODIFIERS.add(getHardnessModifier());
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                return block.getType() == Material.BARRIER;
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
                    final PersistentDataContainer customBlockData = new CustomBlockData(block, OraxenPlugin.get());
                    if (!customBlockData.has(FURNITURE_KEY, PersistentDataType.STRING))
                        return;
                    final String mechanicID = customBlockData.get(FURNITURE_KEY, PersistentDataType.STRING);
                    final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(mechanicID);
                    final BlockLocation rootBlockLocation = new BlockLocation(customBlockData.get(ROOT_KEY,
                            PersistentDataType.STRING));
                    if (mechanic.removeSolid(block.getWorld(), rootBlockLocation, customBlockData
                            .get(ORIENTATION_KEY, PersistentDataType.FLOAT)))
                        mechanic.getDrop().spawns(block.getLocation(), tool);
                });
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                return 1;
            }
        };
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onHangingPlaceEvent(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;
        final Player player = event.getPlayer();
        final Block placedAgainst = event.getClickedBlock();
        final Block target;
        final Material type = placedAgainst.getType();
        if (Utils.REPLACEABLE_BLOCKS.contains(type))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(event.getBlockFace());
            if (target.getType() != Material.AIR && target.getType() != Material.WATER
                    && target.getType() != Material.CAVE_AIR)
                return;
        }
        if (isStandingInside(player, target)
                || !ProtectionLib.canBuild(player, target.getLocation()))
            return;

        for (final Entity entity : target.getWorld().getNearbyEntities(target.getLocation(), 1, 1, 1))
            if (entity instanceof ItemFrame
                    && entity.getLocation().getBlockX() == target.getX()
                    && entity.getLocation().getBlockY() == target.getY()
                    && entity.getLocation().getBlockZ() == target.getZ())
                return;

        final BlockData curentBlockData = target.getBlockData();
        final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(itemID);

        if (mechanic.farmlandRequired &&
                target.getLocation().clone().subtract(0, 1, 0).getBlock().getType()
                        != Material.FARMLAND)
            return;

        target.setType(Material.AIR, false);
        final BlockState currentBlockState = target.getState();

        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player,
                true, event.getHand());
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);
        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return;
        }
        final Rotation rotation = mechanic.hasRotation()
                ? mechanic.getRotation()
                : getRotation(player.getEyeLocation().getYaw(),
                mechanic.hasBarriers()
                        && mechanic.getBarriers().size() > 1);

        final float yaw = mechanic.getYaw(rotation) + mechanic.getSeatYaw();
        final String entityId;
        if (mechanic.hasSeat()) {
            final ArmorStand seat = target.getWorld().spawn(target.getLocation()
                    .add(0.5, mechanic.getSeatHeight() - 1, 0.5), ArmorStand.class, (ArmorStand stand) -> {
                stand.setVisible(false);
                stand.setRotation(yaw, 0);
                stand.setInvulnerable(true);
                stand.setPersistent(true);
                stand.setAI(false);
                stand.setCollidable(false);
                stand.setGravity(false);
                stand.setSilent(true);
                stand.setCustomNameVisible(false);
                stand.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, itemID);
            });
            entityId = seat.getUniqueId().toString();
        } else entityId = null;

        mechanic.place(rotation, yaw, target.getLocation(), entityId);
        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);
    }

    private Rotation getRotation(final double yaw, final boolean restricted) {
        int id = (int) (((Location.normalizeYaw((float) yaw) + 180) * 8 / 360) + 0.5) % 8;
        if (restricted && id % 2 != 0)
            id -= 1;
        return Rotation.values()[id];
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(final HangingBreakEvent event) {
        final PersistentDataContainer container = event.getEntity().getPersistentDataContainer();
        if (container.has(FURNITURE_KEY, PersistentDataType.STRING)) {
            final ItemFrame frame = (ItemFrame) event.getEntity();

            if (event.getCause() == HangingBreakEvent.RemoveCause.ENTITY)
                return;
            event.setCancelled(true);

            final String itemID = container.get(FURNITURE_KEY, PersistentDataType.STRING);
            if (!OraxenItems.exists(itemID))
                return;
            final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(itemID);
            if (mechanic.hasBarriers())
                return;

            mechanic.removeAirFurniture(frame);
            mechanic.getDrop().spawns(frame.getLocation(), new ItemStack(Material.AIR));
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBreakHanging(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame frame)
            if (event.getDamager() instanceof Player player) {
                final PersistentDataContainer container = frame.getPersistentDataContainer();
                if (container.has(FURNITURE_KEY, PersistentDataType.STRING)) {
                    final String itemID = container.get(FURNITURE_KEY, PersistentDataType.STRING);
                    if (!OraxenItems.exists(itemID))
                        return;
                    final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(itemID);
                    event.setCancelled(true);
                    mechanic.removeAirFurniture(frame);
                    mechanic.getDrop().spawns(frame.getLocation(), player.getInventory().getItemInMainHand());
                }
            }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnitureBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block.getType() != Material.BARRIER || event.getPlayer().getGameMode() != GameMode.CREATIVE)
            return;

        final ItemFrame frame = getItemFrame(block.getLocation());
        if (frame != null) {
            frame.remove();
            if (frame.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                final Entity stand = Bukkit.getEntity(UUID.fromString(frame.getPersistentDataContainer()
                        .get(SEAT_KEY, PersistentDataType.STRING)));
                stand.remove();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRotateFurniture(final PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame
                && event.getRightClicked().getPersistentDataContainer()
                .has(FURNITURE_KEY, PersistentDataType.STRING))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerClickOnFurniture(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;
        final Block block = event.getClickedBlock();
        if (block.getType() != Material.BARRIER || event.getPlayer().isSneaking())
            return;
        final ItemFrame frame = getItemFrame(block.getLocation());
        if (frame == null)
            return;
        final String entityId = frame.getPersistentDataContainer().get(SEAT_KEY, PersistentDataType.STRING);
        final Entity stand = Bukkit.getEntity(UUID.fromString(entityId));
        if (stand != null && stand.getPassengers().size() == 0) {
            stand.addPassenger(event.getPlayer());
            event.setCancelled(true);
        }
    }

    private boolean isStandingInside(final Player player, final Block block) {
        final Location playerLocation = player.getLocation();
        final Location blockLocation = block.getLocation();
        return playerLocation.getBlockX() == blockLocation.getBlockX()
                && (playerLocation.getBlockY() == blockLocation.getBlockY()
                || playerLocation.getBlockY() + 1 == blockLocation.getBlockY())
                && playerLocation.getBlockZ() == blockLocation.getBlockZ();
    }

}
