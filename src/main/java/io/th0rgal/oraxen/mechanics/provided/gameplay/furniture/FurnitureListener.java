package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.morepersistentdatatypes.DataType;
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

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.SEAT_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.rotationToYaw;

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

    @EventHandler(priority = EventPriority.LOWEST)
    public void callInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        EquipmentSlot hand = event.getHand();
        if (block == null) return;
        if (hand != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useInteractedBlock() == Event.Result.DENY) return;

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (mechanic == null) return;
        Entity baseEntity = mechanic.getBaseEntity(block);
        if (baseEntity == null) return;

        OraxenFurnitureInteractEvent oraxenEvent = new OraxenFurnitureInteractEvent(mechanic, baseEntity, event.getPlayer(), event.getItem(), hand, block, event.getBlockFace());
        Bukkit.getPluginManager().callEvent(oraxenEvent);
        if (oraxenEvent.isCancelled()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemFrameRotate(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;
        if (!OraxenFurniture.isFurniture(frame)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLimitedPlacing(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        ItemStack item = event.getItem();

        if (item == null || block == null || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (BlockHelpers.isInteractable(block) && !event.getPlayer().isSneaking()) return;

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
        if (block == null || !placedAgainst.canPlace(block.getBlockData())) return;
        if (BlockHelpers.isInteractable(placedAgainst) && !player.isSneaking()) return;

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
        final float yaw = rotationToYaw(rotation);
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

        Entity baseEntity = mechanic.place(block.getLocation(), item, rotation, yaw, event.getBlockFace());
        Utils.swingHand(player, event.getHand());

        final OraxenFurniturePlaceEvent furniturePlaceEvent = new OraxenFurniturePlaceEvent(mechanic, block, baseEntity, player, item, hand);
        Bukkit.getPluginManager().callEvent(furniturePlaceEvent);

        if (furniturePlaceEvent.isCancelled()) {
            OraxenFurniture.remove(baseEntity, null);
            block.setBlockData(currentBlockData);
            return;
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    private Block getTarget(Block placedAgainst, BlockFace blockFace) {
        Block target;
        if (BlockHelpers.isReplaceable(placedAgainst))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(blockFace);
            if (!BlockHelpers.isReplaceable(target) && !target.getType().isAir() && !target.isLiquid() && target.getType() != Material.LIGHT) return null;
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
        Entity entity = event.getEntity();
        if (event.getCause() == HangingBreakEvent.RemoveCause.ENTITY) return;

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return;

        event.setCancelled(true);
        if (mechanic.hasBarriers()) return;

        mechanic.removeAirFurniture(entity);
        mechanic.getDrop().spawns(entity.getLocation(), new ItemStack(Material.AIR));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBreakHanging(final EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getDamager() instanceof Player player)) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return;

        event.setCancelled(true);
        entity = mechanic.getBaseEntity(entity);
        if (entity == null) return;
        OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic, entity, player, entity.getLocation().getBlock());
        OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
        if (furnitureBreakEvent.isCancelled()) return;

        OraxenFurniture.remove(entity, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomFurniture(final BlockBreakEvent event) {
        final Block block = event.getBlock();

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (mechanic == null) return;

        OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic, mechanic.getBaseEntity(block), event.getPlayer(), block);
        OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
        if (furnitureBreakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

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
        Location location = block != null && block.getType() == Material.BARRIER ? block.getLocation()
                : hitEntity != null ? hitEntity.getLocation() : null;
        boolean isFurniture = block != null ? OraxenFurniture.isFurniture(block) : hitEntity != null && OraxenFurniture.isFurniture(hitEntity);

        // Do not break furniture with a hitbox unless its explosive
        if (location != null && isFurniture) {
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
        Entity baseEntity = event.getRightClicked();
        final Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;
        // Swap baseEntity to the baseEntity if interacted with entity is Interaction type
        Entity interaction = null;
        if (OraxenPlugin.supportsDisplayEntities && baseEntity instanceof Interaction interactionEntity) {
            interaction = interactionEntity;
            baseEntity = mechanic.getBaseEntity(interaction);
            baseEntity = baseEntity != null ? baseEntity : interaction;
        }

        ItemStack itemInHand = hand == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        OraxenFurnitureInteractEvent furnitureInteractEvent = new OraxenFurnitureInteractEvent(mechanic, baseEntity, player, itemInHand, hand);
        OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureInteractEvent);
        if (furnitureInteractEvent.isCancelled()) return;

        mechanic.runClickActions(player);

        if (mechanic.isStorage()) {
            StorageMechanic storage = mechanic.getStorage();
            switch (storage.getStorageType()) {
                case STORAGE: case SHULKER: storage.openStorage(baseEntity, player);
                case PERSONAL: storage.openPersonalStorage(player, baseEntity.getLocation(), baseEntity);
                case DISPOSAL: storage.openDisposal(player, baseEntity.getLocation(), baseEntity);
                case ENDERCHEST: player.openInventory(player.getEnderChest());
            }
            event.setCancelled(true);
        }

        if (mechanic.hasSeat() && interaction != null) {
            PersistentDataContainer pdc = interaction.getPersistentDataContainer();
            UUID entityUuid = pdc.get(SEAT_KEY, DataType.UUID);
            //Convert old seats to new, remove in a good while
            if (entityUuid == null) {
                String oldUUID = pdc.get(SEAT_KEY, PersistentDataType.STRING);
                if (oldUUID == null) return;
                entityUuid = UUID.fromString(oldUUID);
                pdc.remove(SEAT_KEY);
                pdc.set(SEAT_KEY, DataType.UUID, entityUuid);
            }
            final Entity stand = Bukkit.getEntity(entityUuid);

            if (stand != null && stand.getPassengers().isEmpty()) {
                stand.addPassenger(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerClickOnFurniture(final PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || hand != EquipmentSlot.HAND) return;
        if (block == null || block.getType() != Material.BARRIER || player.isSneaking()) return;

        final FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (mechanic == null) return;

        final Entity baseEntity = mechanic.getBaseEntity(block);

        final OraxenFurnitureInteractEvent furnitureInteractEvent = new OraxenFurnitureInteractEvent(mechanic, baseEntity, player, event.getItem(), hand, block, event.getBlockFace());
        Bukkit.getPluginManager().callEvent(furnitureInteractEvent);
        if (furnitureInteractEvent.isCancelled()) return;

        mechanic.runClickActions(player);

        if (mechanic.isStorage()) {
            StorageMechanic storage = mechanic.getStorage();
            switch (storage.getStorageType()) {
                case STORAGE: case SHULKER: storage.openStorage(baseEntity, player);
                case PERSONAL: storage.openPersonalStorage(player, baseEntity.getLocation(), baseEntity);
                case DISPOSAL: storage.openDisposal(player, baseEntity.getLocation(), baseEntity);
                case ENDERCHEST: player.openInventory(player.getEnderChest());
            }
        }

        if (mechanic.hasSeat()) {
            PersistentDataContainer pdc = BlockHelpers.getPDC(block);
            UUID entityUuid = pdc.has(SEAT_KEY, DataType.UUID) ? pdc.get(SEAT_KEY, DataType.UUID) : null;

            //Convert old seats to new, remove in a good while
            if (entityUuid == null) {
                String oldUUID = pdc.has(SEAT_KEY, PersistentDataType.STRING) ? pdc.get(SEAT_KEY, PersistentDataType.STRING) : null;
                if (oldUUID == null) return;
                entityUuid = UUID.fromString(oldUUID);
                pdc.remove(SEAT_KEY);
                pdc.set(SEAT_KEY, DataType.UUID, entityUuid);
            }

            Entity stand = Bukkit.getEntity(entityUuid);
            if (stand != null && stand.getPassengers().isEmpty()) {
                stand.addPassenger(event.getPlayer());
            }
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
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
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
        } else if (OraxenItems.getIdByItem(event.getCursor()) != null) {
            String itemID = OraxenItems.getIdByItem(event.getCursor());
            if (!OraxenFurniture.isFurniture(itemID)) return;
            for (int i = 0; i <= 8; i++) {
                if (Objects.equals(OraxenItems.getIdByItem(player.getInventory().getItem(i)), itemID)) {
                    player.getInventory().setHeldItemSlot(i);
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCursor(OraxenItems.getItemById(itemID).build());
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
}
