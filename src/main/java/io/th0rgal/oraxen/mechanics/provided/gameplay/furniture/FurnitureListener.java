package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.customblockdata.CustomBlockData;
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
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onHangingPlaceEvent(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block placedAgainst = event.getClickedBlock();
        assert placedAgainst != null;
        final Block block = getTarget(placedAgainst, event.getBlockFace());
        ItemStack item = event.getItem();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (item == null || event.getHand() != EquipmentSlot.HAND) return;
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

        Material oldtype = block.getType();
        block.setType(Material.AIR, false);
        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(block, block.getState(), placedAgainst,
                item, player,
                true, Objects.requireNonNull(event.getHand()));

        final Rotation rotation = mechanic.hasRotation()
                ? mechanic.getRotation()
                : getRotation(player.getEyeLocation().getYaw(),
                mechanic.hasBarriers() && mechanic.getBarriers().size() > 1);
        final float yaw = mechanic.getYaw(rotation);
        if (player.getGameMode() == GameMode.ADVENTURE) blockPlaceEvent.setCancelled(true);
        if (mechanic.notEnoughSpace(yaw, block.getLocation())) {
            blockPlaceEvent.setCancelled(true);
            Message.NOT_ENOUGH_SPACE.send(player);
        }

        //Bukkit.getPluginManager().callEvent(blockPlaceEvent);

        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            block.setBlockData(currentBlockData, false); // false to cancel physic
            return;
        }

        ItemFrame itemframe = mechanic.place(rotation, yaw, event.getBlockFace(), block.getLocation(), item, player);
        Utils.swingHand(player, event.getHand());

        final OraxenFurniturePlaceEvent furniturePlaceEvent = new OraxenFurniturePlaceEvent(mechanic, block, itemframe, player);

        Bukkit.getPluginManager().callEvent(furniturePlaceEvent);

        if (furniturePlaceEvent.isCancelled()) {
            itemframe.remove();
            block.setType(oldtype, false);
            return;
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);
    }

    private Block getTarget(Block placedAgainst, BlockFace blockFace) {
        final Material type = placedAgainst.getType();
        if (BlockHelpers.REPLACEABLE_BLOCKS.contains(type))
            return placedAgainst;
        else {
            Block target = placedAgainst.getRelative(blockFace);
            if (!target.getType().isAir() && target.getType() != Material.WATER)
                return null;
            return target;
        }
    }

    private FurnitureMechanic getMechanic(ItemStack item, Player player, Block target) {
        final String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID) || BlockHelpers.isStandingInside(player, target)) return null;
        if (!ProtectionLib.canBuild(player, target.getLocation())) return null;

        for (final Entity entity : target.getWorld().getNearbyEntities(target.getLocation(), 1, 1, 1))
            if (entity instanceof ItemFrame
                    && entity.getLocation().getBlockX() == target.getX()
                    && entity.getLocation().getBlockY() == target.getY()
                    && entity.getLocation().getBlockZ() == target.getZ())
                return null;

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
        final PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
        if (pdc.has(FURNITURE_KEY, PersistentDataType.STRING)) {
            final ItemFrame frame = (ItemFrame) event.getEntity();

            if (event.getCause() == HangingBreakEvent.RemoveCause.ENTITY) return;
            event.setCancelled(true);

            final String itemID = pdc.get(FURNITURE_KEY, PersistentDataType.STRING);
            if (!OraxenItems.exists(itemID)) return;
            final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(itemID);
            if (mechanic == null || mechanic.hasBarriers()) return;

            mechanic.removeAirFurniture(frame);
            mechanic.getDrop().spawns(frame.getLocation(), new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBreakHanging(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame frame) {
            if (event.getDamager() instanceof Player player) {
                Block block = frame.getLocation().getBlock();
                final FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(frame);
                event.setCancelled(true);

                OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic, player, block, frame);
                OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
                if (furnitureBreakEvent.isCancelled()) {
                    return;
                }

                mechanic.removeAirFurniture(frame);
                if (player.getGameMode() != GameMode.CREATIVE) {
                    ItemStack itemInHand = player.getInventory().getItemInMainHand();
                    ItemMeta meta = frame.getItem().getItemMeta();

                    if (mechanic.isStorage()) {
                        mechanic.getStorage().dropStorageContent(mechanic, frame);
                        if (mechanic.getStorage().isShulker()) return; // drop method handles all relevant drops
                    }

                    if (mechanic.hasEvolution())
                        mechanic.getDrop().spawns(frame.getLocation(), itemInHand);
                    else if (meta instanceof LeatherArmorMeta || meta instanceof PotionMeta) {
                        mechanic.getDrop().furnitureSpawns(frame, itemInHand);
                    } else mechanic.getDrop().spawns(frame.getLocation(), itemInHand);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomFurniture(final BlockBreakEvent event) {
        final Block block = event.getBlock();

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (mechanic == null || !event.isDropItems()) return;

        if (OraxenFurniture.remove(block.getLocation(), event.getPlayer())) {
            event.setCancelled(true);
        } else event.setDropItems(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHitFurniture(final ProjectileHitEvent event) {
        Block block = event.getHitBlock();
        Entity entity = event.getHitEntity();
        Location location = block != null && block.getType() == Material.BARRIER
                ? block.getLocation() : entity instanceof ItemFrame
                ? entity.getLocation() : null;

        if (location != null && event.getEntity() instanceof Explosive) {
            OraxenFurniture.remove(location, null);
        } else event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnitureBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block.getType() != Material.BARRIER) return;

        final FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (mechanic == null) return;

        OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic, event.getPlayer(), block, mechanic.getItemFrame(block));
        OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
        if (furnitureBreakEvent.isCancelled()) event.setCancelled(true);
        else OraxenBlocks.remove(block.getLocation(), event.getPlayer());
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
                    case PERSONAL -> storage.openPersonalStorage(player);
                    case DISPOSAL -> storage.openDisposal(player, frame.getLocation());
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

    @EventHandler // Set rotation key of old furniture and its barriers
    public void setMissingPDCKeys(ChunkLoadEvent event) {
        for (Block block : CustomBlockData.getBlocksWithCustomData(OraxenPlugin.get(), event.getChunk())) {
            if (block.getType() != Material.BARRIER) continue;
            PersistentDataContainer pdc = BlockHelpers.getPDC(block);
            if (pdc.has(ROTATION_KEY, DataType.asEnum(Rotation.class))) continue;
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
            if (mechanic == null) continue;
            Rotation rotation = getRotation(pdc.getOrDefault(ORIENTATION_KEY, PersistentDataType.FLOAT, 0f), mechanic.hasBarriers() && mechanic.getBarriers().size() > 1);
            pdc.set(ROTATION_KEY, DataType.asEnum(Rotation.class), rotation);
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
