package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.listeners;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureHelpers;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.seats.FurnitureSeat;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.Objects;
import java.util.Optional;

public class FurnitureListener implements Listener {

    /*@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemFrameRotate(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(frame);
        if (mechanic == null || mechanic.isRotatable()) return;
        event.setCancelled(true);
    }*/

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLimitedPlacing(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        ItemStack item = event.getItem();

        if (item == null || block == null || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(OraxenItems.getIdByItem(item));
        if (mechanic == null || !mechanic.hasLimitedPlacing()) return;

        if (!event.getPlayer().isSneaking() && BlockHelpers.isInteractable(block)) return;

        LimitedPlacing limitedPlacing = mechanic.limitedPlacing();
        Block belowPlaced = block.getRelative(blockFace).getRelative(BlockFace.DOWN);

        if (limitedPlacing.isNotPlacableOn(block, blockFace)) event.setCancelled(true);
        else if (limitedPlacing.isRadiusLimited()) {
            LimitedPlacing.RadiusLimitation radiusLimitation = limitedPlacing.getRadiusLimitation();
            int radius = radiusLimitation.getRadius();
            int amount = radiusLimitation.getAmount();
            if (block.getWorld().getNearbyEntities(block.getLocation(), radius, radius, radius).stream()
                    .filter(OraxenFurniture::isFurniture)
                    .filter(e -> OraxenFurniture.getFurnitureMechanic(e).getItemID().equals(mechanic.getItemID()))
                    .filter(e -> e.getLocation().distanceSquared(block.getLocation()) <= radius * radius)
                    .count() >= amount) event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.ALLOW)
            if (!limitedPlacing.checkLimitedMechanic(belowPlaced)) event.setCancelled(true);
        else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.DENY)
            if (limitedPlacing.checkLimitedMechanic(belowPlaced)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onFurniturePlace(final PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        final Player player = event.getPlayer();
        final BlockFace blockFace = event.getBlockFace();
        final Location targetLocation = Optional.ofNullable(event.getInteractionPoint())
                .map(l -> blockFace == BlockFace.DOWN ? l.subtract(0,1.0,0) : l).orElse(null);
        final EquipmentSlot hand = event.getHand();
        FurnitureMechanic mechanic = getMechanic(item, player, targetLocation);

        if (targetLocation == null || mechanic == null || item == null || hand != EquipmentSlot.HAND) return;
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!player.isSneaking() && BlockHelpers.isInteractable(targetLocation.getBlock())) return;

        final Block block = targetLocation.getBlock();
        final BlockData currentBlockData = block.getBlockData();

        Block farm = block.getRelative(BlockFace.DOWN);
        if (mechanic.farmlandRequired && farm.getType() != Material.FARMLAND) return;

        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(block, block.getState(), block.getRelative(event.getBlockFace()), item, player, true, hand);

        final Rotation rotation = getRotation(player.getEyeLocation().getYaw(), mechanic);
        final float yaw = FurnitureHelpers.correctedYaw(mechanic, FurnitureHelpers.rotationToYaw(rotation));
        if (player.getGameMode() == GameMode.ADVENTURE)
            blockPlaceEvent.setCancelled(true);
        if (mechanic.notEnoughSpace(block.getLocation(), yaw)) {
            blockPlaceEvent.setCancelled(true);
            Message.NOT_ENOUGH_SPACE.send(player);
        }

        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            block.setBlockData(currentBlockData);
            return;
        }

        ItemDisplay baseEntity = mechanic.place(block.getLocation(), yaw, event.getBlockFace(), true);
        Utils.swingHand(player, event.getHand());

        final OraxenFurniturePlaceEvent furniturePlaceEvent = new OraxenFurniturePlaceEvent(mechanic, block, baseEntity, player, item, hand);
        if (!EventUtils.callEvent(furniturePlaceEvent)) {
            OraxenFurniture.remove(baseEntity, null);
            block.setBlockData(currentBlockData);
            return;
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE)) item.setAmount(item.getAmount() - 1);
        event.setUseInteractedBlock(Event.Result.DENY);
        if (VersionUtil.isPaperServer()) baseEntity.getWorld().sendGameEvent(player, GameEvent.BLOCK_PLACE, baseEntity.getLocation().toVector());
    }

    private FurnitureMechanic getMechanic(ItemStack item, Player player, Location placed) {
        final String itemID = OraxenItems.getIdByItem(item);
        if (itemID == null || placed == null) return null;
        if (FurnitureFactory.get().isNotImplementedIn(itemID) || BlockHelpers.isStandingInside(player, placed.getBlock())) return null;
        if (!ProtectionLib.canBuild(player, placed)) return null;
        if (OraxenFurniture.isFurniture(placed)) return null;

        return FurnitureFactory.get().getMechanic(item);
    }

    private Rotation getRotation(final double yaw, FurnitureMechanic mechanic) {
        FurnitureMechanic.RestrictedRotation restrictedRotation = mechanic.restrictedRotation();
        int id = (int) (((Location.normalizeYaw((float) yaw) + 180) * 8 / 360) + 0.5) % 8;
        int offset = restrictedRotation == FurnitureMechanic.RestrictedRotation.STRICT ? 0 : 1;
        if (restrictedRotation != FurnitureMechanic.RestrictedRotation.NONE && id % 2 != 0) id -= offset;
        return Rotation.values()[id];
    }

    /**
     * Prevents ItemFrame based furniture from breaking due to being in a barrier block
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(final HangingBreakEvent event) {
        Entity entity = event.getEntity();
        if (event.getCause() == HangingBreakEvent.RemoveCause.ENTITY) return;

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return;

        event.setCancelled(true);
        OraxenFurniture.remove(entity, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBreakHanging(final EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getDamager() instanceof Player player)) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return;

        event.setCancelled(true);
        if (!ProtectionLib.canBreak(player, entity.getLocation())) return;
        OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic, entity, player, entity.getLocation().getBlock());
        if (!EventUtils.callEvent(furnitureBreakEvent)) return;
        if (OraxenFurniture.remove(entity, player, furnitureBreakEvent.getDrop())) event.setCancelled(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHitFurniture(final ProjectileHitEvent event) {
        Block hitBlock = event.getHitBlock();
        Entity hitEntity = event.getHitEntity();
        Projectile projectile = event.getEntity();
        Player player = projectile.getShooter() instanceof Player ? (Player) projectile.getShooter() : null;
        Location hitLocation = hitBlock != null ? hitBlock.getLocation() : hitEntity != null ? hitEntity.getLocation() : null;
        boolean isFurniture = hitEntity != null ? OraxenFurniture.isFurniture(hitEntity) : hitLocation != null ? OraxenFurniture.isFurniture(hitLocation) : false;

        // Do not break furniture with a hitbox unless its explosive
        if (hitLocation != null && isFurniture) {
            if (player != null && !ProtectionLib.canBreak(player, hitLocation))
                event.setCancelled(true);
            else if (projectile instanceof Explosive) {
                event.setCancelled(true);
                OraxenFurniture.remove(hitLocation, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileDamageFurniture(final EntityDamageByEntityEvent event) {
        Entity furniture = event.getEntity();
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(furniture);
        if (mechanic == null || !(event.getDamager() instanceof Projectile projectile)) return;
        Player player = projectile.getShooter() instanceof Player ? (Player) projectile.getShooter() : null;

        event.setCancelled(true);
        if (!mechanic.hitbox().barrierHitboxes().isEmpty() || !isDamagingProjectile(projectile)) return;
        if (player != null && !ProtectionLib.canBreak(player, furniture.getLocation())) return;

        OraxenFurniture.remove(furniture, player);
    }

    private static boolean isDamagingProjectile(Projectile projectile) {
        return projectile instanceof AbstractArrow || projectile instanceof Fireball;
    }*/

    /*@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractFurniture(PlayerInteractEntityEvent event) {
        Entity baseEntity = event.getRightClicked();
        final Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();

        if (!ProtectionLib.canInteract(player, baseEntity.getLocation())) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;

        ItemStack itemInHand = hand == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        EventUtils.callEvent(new OraxenFurnitureInteractEvent(mechanic, baseEntity, player, itemInHand, hand));
    }*/

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFurnitureInteract(OraxenFurnitureInteractEvent event) {
        Player player = event.player();
        FurnitureMechanic mechanic = event.mechanic();
        ItemDisplay baseEntity = event.baseEntity();
        Location interactionPoint = event.interactionPoint();

        mechanic.runClickActions(player);

        if (mechanic.isRotatable() && player.isSneaking()) mechanic.rotateFurniture(baseEntity);
        else if (mechanic.isStorage() && !player.isSneaking()) {
            StorageMechanic storage = mechanic.storage();
            switch (storage.getStorageType()) {
                case STORAGE, SHULKER -> storage.openStorage(baseEntity, player);
                case PERSONAL -> storage.openPersonalStorage(player, baseEntity.getLocation(), baseEntity);
                case DISPOSAL -> storage.openDisposal(player, baseEntity.getLocation(), baseEntity);
                case ENDERCHEST -> player.openInventory(player.getEnderChest());
            }
        } else if (mechanic.hasSeats() && !player.isSneaking()) FurnitureSeat.sitOnSeat(baseEntity, player, interactionPoint);
        else if (mechanic.isRotatable()) mechanic.rotateFurniture(baseEntity);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(final InventoryCreativeEvent event) {
        final Player player = (Player) event.getInventory().getHolder();
        if (event.getClickedInventory() == null || player == null) return;
        if (event.getClick() != ClickType.CREATIVE) return;
        if (event.getSlotType() != InventoryType.SlotType.QUICKBAR) return;
        if (event.getCursor().getType() != Material.BARRIER) return;

        final RayTraceResult rayTraceResult = player.rayTraceBlocks(8.0);
        FurnitureMechanic mechanic = rayTraceResult != null ? OraxenFurniture.getFurnitureMechanic(rayTraceResult.getHitPosition().toLocation(player.getWorld())) : null;
        if (mechanic == null) return;

        ItemStack item = OraxenItems.getItemById(mechanic.getItemID()).build();
        for (int i = 0; i <= 8; i++) {
            if (Objects.equals(OraxenItems.getIdByItem(player.getInventory().getItem(i)), mechanic.getItemID())) {
                player.getInventory().setHeldItemSlot(i);
                event.setCancelled(true);
                return;
            }
        }
        event.setCursor(item);
    }

    @EventHandler(ignoreCancelled = true)
    public void updateLightOnBlockBreak(BlockBreakEvent event) {
        //Block block = event.getBlock();
        //if (!OraxenFurniture.isFurniture(block)) LightMechanic.refreshBlockLight(block);
    }

    @EventHandler
    public void onPlayerQuitEvent(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (OraxenFurniture.isFurniture(player.getVehicle())) player.leaveVehicle();
    }
}
