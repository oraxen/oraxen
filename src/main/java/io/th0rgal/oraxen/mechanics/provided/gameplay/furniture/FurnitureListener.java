package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.api.events.OraxenFurniturePlaceEvent;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.oraxen.utils.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.storage.StorageMechanic;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.Objects;
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
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                return 1;
            }
        };
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void callInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getType() != Material.NOTE_BLOCK) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (mechanic == null) return;

        OraxenFurnitureInteractEvent oraxenEvent = new OraxenFurnitureInteractEvent(mechanic, event.getPlayer(), block, mechanic.getItemFrame(block));
        Bukkit.getPluginManager().callEvent(oraxenEvent);
        if (oraxenEvent.isCancelled()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLimitedPlacing(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        ItemStack item = event.getItem();

        if (item == null || block == null || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (block.getType().isInteractable() && block.getType() != Material.NOTE_BLOCK) return;

        FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(OraxenItems.getIdByItem(item));
        if (mechanic == null || !mechanic.hasLimitedPlacing()) return;

        LimitedPlacing limitedPlacing = mechanic.getLimitedPlacing();
        Block belowPlaced = block.getRelative(blockFace).getRelative(BlockFace.DOWN);

        if (limitedPlacing.isNotPlacableOn(belowPlaced, blockFace)) {
            event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.ALLOW) {
            if (!limitedPlacing.checkLimitedMechanic(belowPlaced))
                event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.DENY) {
            if (limitedPlacing.checkLimitedMechanic(belowPlaced))
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onFurniturePlace(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block placedAgainst = event.getClickedBlock();
        final EquipmentSlot hand = event.getHand();
        assert placedAgainst != null;
        Block block = getTarget(placedAgainst, event.getBlockFace());
        ItemStack item = event.getItem();

        if (event.useInteractedBlock() == Event.Result.DENY) return;
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (item == null || hand != EquipmentSlot.HAND) return;
        if (placedAgainst.getType().isInteractable() && placedAgainst.getType() != Material.NOTE_BLOCK) return;

        if (block == null) return;
        final BlockData currentBlockData = block.getBlockData();
        FurnitureMechanic mechanic = getMechanic(item, player, block);
        if (mechanic == null) return;

        Block farm = block.getRelative(BlockFace.DOWN);
        if (mechanic.farmlandRequired && farm.getType() != Material.FARMLAND) return;

        if (mechanic.farmblockRequired) {
            if (farm.getType() != Material.NOTE_BLOCK) return;
            NoteBlockMechanic farmMechanic = OraxenBlocks.getNoteBlockMechanic(farm);
            if (farmMechanic == null || !farmMechanic.hasDryout()) return;
            if (!farmMechanic.getDryout().isFarmBlock()) return;
        }

        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(block, block.getState(), placedAgainst, item, player, true, hand);

        final Rotation rotation = getRotation(player.getEyeLocation().getYaw(), mechanic.getBarriers().size() > 1);
        final float yaw = mechanic.getYaw(rotation);
        if (player.getGameMode() == GameMode.ADVENTURE)
            blockPlaceEvent.setCancelled(true);
        if (mechanic.notEnoughSpace(yaw, block.getLocation())) {
            blockPlaceEvent.setCancelled(true);
            Message.NOT_ENOUGH_SPACE.send(player);
        }

        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            block.setBlockData(currentBlockData);
            return;
        }

        ItemFrame itemframe = mechanic.place(rotation, yaw, event.getBlockFace(), block.getLocation(), item);
        Utils.swingHand(player, event.getHand());

        final OraxenFurniturePlaceEvent furniturePlaceEvent = new OraxenFurniturePlaceEvent(mechanic, block, itemframe, player);
        Bukkit.getPluginManager().callEvent(furniturePlaceEvent);

        if (furniturePlaceEvent.isCancelled()) {
            OraxenFurniture.remove(itemframe, null);
            block.setBlockData(currentBlockData);
            return;
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    private Block getTarget(Block placedAgainst, BlockFace blockFace) {
        Block target;
        if (BlockHelpers.REPLACEABLE_BLOCKS.contains(placedAgainst.getType()))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(blockFace);
            if (!target.getType().isAir() && !target.isLiquid() && target.getType() != Material.LIGHT) return null;
        }
        return target;
    }

    private FurnitureMechanic getMechanic(ItemStack item, Player player, Block placed) {
        final String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID) || BlockHelpers.isStandingInside(player, placed)) return null;
        if (!ProtectionLib.canBuild(player, placed.getLocation())) return null;
        if (OraxenFurniture.isFurniture(placed)) return null;

        return (FurnitureMechanic) factory.getMechanic(itemID);
    }

    private Rotation getRotation(final double yaw, final boolean restricted) {
        int id = (int) (((Location.normalizeYaw((float) yaw) + 180) * 8 / 360) + 0.5) % 8;
        if (restricted && id % 2 != 0)
            id -= 1;
        return Rotation.values()[id];
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(final HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (event.getCause() == HangingBreakEvent.RemoveCause.ENTITY) return;

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(frame);
        if (mechanic == null) return;

        event.setCancelled(true);
        if (mechanic.hasBarriers()) return;

        mechanic.removeAirFurniture(frame);
        mechanic.getDrop().spawns(frame.getLocation(), new ItemStack(Material.AIR));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBreakHanging(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame frame) {
            if (event.getDamager() instanceof Player player) {
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(frame);
                if (mechanic == null) return;
                event.setCancelled(true);

                OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic, player, frame.getLocation().getBlock(), frame);
                OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
                if (furnitureBreakEvent.isCancelled()) return;

                OraxenFurniture.remove(frame, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomFurniture(final BlockBreakEvent event) {
        final Block block = event.getBlock();

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (mechanic == null || !event.isDropItems()) return;

        OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic, event.getPlayer(), block, mechanic.getItemFrame(block));
        OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
        if (furnitureBreakEvent.isCancelled()) return;

        if (OraxenFurniture.remove(block.getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        } else event.setDropItems(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHitFurniture(final ProjectileHitEvent event) {
        Block block = event.getHitBlock();
        Entity hitEntity = event.getHitEntity();
        Projectile projectile = event.getEntity();
        Player player = projectile.getShooter() instanceof Player ? (Player) projectile.getShooter() : null;
        Location location = block != null && block.getType() == Material.BARRIER
                ? block.getLocation() : hitEntity instanceof ItemFrame
                ? hitEntity.getLocation() : null;

        // Do not break furniture with a hitbox unless its explosive
        if (location != null) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(hitEntity);
            if (player != null && !ProtectionLib.canBreak(player, location))
                event.setCancelled(true);
            else if (projectile instanceof Explosive) {
                event.setCancelled(true);
                OraxenFurniture.remove(location, player);
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
        if (mechanic.hasBarriers() || !isDamagingProjectile(projectile)) return;
        if (player != null && !ProtectionLib.canBreak(player, furniture.getLocation())) return;

        OraxenFurniture.remove(furniture, player);
    }

    private static boolean isDamagingProjectile(Projectile projectile) {
        return projectile instanceof AbstractArrow || projectile instanceof Fireball;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractFurniture(PlayerInteractEntityEvent event) {
        final Entity entity = event.getRightClicked();
        final Player player = event.getPlayer();
        if (!(entity instanceof ItemFrame itemFrame)) return;
        String mechanicID = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (mechanicID == null) return;
        //prevent rotation
        event.setCancelled(true);
        FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(mechanicID);
        OraxenFurnitureInteractEvent furnitureInteractEvent = new OraxenFurnitureInteractEvent(mechanic, player, null, itemFrame);
        OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureInteractEvent);
        if (furnitureInteractEvent.isCancelled()) {
            return;
        }
        if (mechanic.hasClickActions()) {
            mechanic.runClickActions(player);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerClickOnFurniture(final PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        if (block == null || block.getType() != Material.BARRIER || player.isSneaking()) return;

        final FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);

        Utils.swingHand(player, event.getHand());

        if (mechanic != null) {
            // Call the oraxen furniture event
            final PersistentDataContainer pdc = BlockHelpers.getPDC(block);
            Float orientation = pdc.get(ORIENTATION_KEY, PersistentDataType.FLOAT);
            final BlockLocation rootBlockLocation = new BlockLocation(Objects.requireNonNull(pdc.get(ROOT_KEY, PersistentDataType.STRING)));
            final ItemFrame frame = mechanic.getItemFrame(block);

            final OraxenFurnitureInteractEvent furnitureInteractEvent = new OraxenFurnitureInteractEvent(mechanic, player, block, frame);
            Bukkit.getPluginManager().callEvent(furnitureInteractEvent);

            if (furnitureInteractEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            if (mechanic.hasClickActions()) {
                mechanic.runClickActions(player);
                event.setCancelled(true);
            }

            if (mechanic.isStorage()) {
                StorageMechanic storage = mechanic.getStorage();
                switch (storage.getStorageType()) {
                    case STORAGE, SHULKER -> storage.openStorage(frame, player);
                    case PERSONAL -> storage.openPersonalStorage(player, frame.getLocation(), frame);
                    case DISPOSAL -> storage.openDisposal(player, frame.getLocation(), frame);
                    case ENDERCHEST -> player.openInventory(player.getEnderChest());
                }
                event.setCancelled(true);
            }
        }

        final String entityUuid = BlockHelpers.getPDC(block).getOrDefault(SEAT_KEY, PersistentDataType.STRING, "");
        if (entityUuid.isBlank()) return;
        final Entity stand = Bukkit.getEntity(UUID.fromString(entityUuid));

        if (stand != null && stand.getPassengers().isEmpty()) {
            stand.addPassenger(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(final InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE) return;
        final Player player = (Player) event.getInventory().getHolder();
        if (player == null) return;
        if (event.getCursor().getType() == Material.BARRIER) {
            final RayTraceResult rayTraceResult = player.rayTraceBlocks(6.0);
            if (rayTraceResult == null) return;
            final Block block = rayTraceResult.getHitBlock();
            if (block == null) return;
            FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
            if (furnitureMechanic == null) return;

            ItemStack item = OraxenItems.getItemById(furnitureMechanic.getItemID()).build();
            for (int i = 0; i <= 8; i++) {
                if (Objects.equals(OraxenItems.getIdByItem(player.getInventory().getItem(i)), furnitureMechanic.getItemID())) {
                    player.getInventory().setHeldItemSlot(i);
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCursor(item);
        } else if (OraxenItems.getIdByItem(event.getCursor()) != null) {
            String id = OraxenItems.getIdByItem(event.getCursor());
            if (!(factory.getMechanic(id) instanceof FurnitureMechanic)) return;
            for (int i = 0; i <= 8; i++) {
                if (Objects.equals(OraxenItems.getIdByItem(player.getInventory().getItem(i)), id)) {
                    player.getInventory().setHeldItemSlot(i);
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCursor(OraxenItems.getItemById(id).build());
        }
    }

    @EventHandler
    public void onPlayerQuitEvent(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final Entity vehicle = player.getVehicle();
        if (vehicle instanceof final ArmorStand armorStand) {
            if (armorStand.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                player.leaveVehicle();
            }
        }
    }

    /**
     * Scheduled for removal in a future update. As of 1.147.0 API has been entirely redone.<br>
     * See {@link io.th0rgal.oraxen.api.OraxenFurniture#getFurnitureMechanic(Block)} for the new method
     */
    @Deprecated(forRemoval = true, since = "1.147.0")
    public static FurnitureMechanic getFurnitureMechanic(Block block) {
        if (block.getType() != Material.BARRIER) return null;
        final String mechanicID = BlockHelpers.getPDC(block).get(FURNITURE_KEY, PersistentDataType.STRING);
        return (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(mechanicID);
    }
}
