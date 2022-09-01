package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.events.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.events.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolvingFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.oraxen.utils.limitedplacing.LimitedPlacing;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.*;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;
import static io.th0rgal.oraxen.utils.BlockHelpers.*;

public class FurnitureListener implements Listener {

    private final MechanicFactory factory;
    private final Map<Block, BukkitTask> breakerPlaySound = new HashMap<>();

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
                    if (!customBlockData.has(FURNITURE_KEY, PersistentDataType.STRING)) return;

                    final String mechanicID = customBlockData.get(FURNITURE_KEY, PersistentDataType.STRING);
                    final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(mechanicID);
                    Float orientation = customBlockData.get(ORIENTATION_KEY, PersistentDataType.FLOAT);
                    final BlockLocation rootBlockLocation = new BlockLocation(customBlockData.get(ROOT_KEY, PersistentDataType.STRING));
                    final ItemFrame frame = mechanic.getItemFrame(block, rootBlockLocation, orientation);

                    OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic, player, block);
                    OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
                    if (furnitureBreakEvent.isCancelled()) {
                        return;
                    }

                    if (mechanic.removeSolid(block.getWorld(), rootBlockLocation, orientation)) {
                        mechanic.getDrop().furnitureSpawns(frame, tool);
                    }
                });
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                return 1;
            }
        };
    }


    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlacingStone(final BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.BARRIER || block.getType() == Material.TRIPWIRE) return;
        if (block.getBlockData().getSoundGroup().getPlaceSound() != Sound.BLOCK_STONE_PLACE) return;
        BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), VANILLA_STONE_PLACE);
    }

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingStone(final BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.BARRIER || block.getType() == Material.TRIPWIRE) return;
        if (block.getBlockData().getSoundGroup().getBreakSound() != Sound.BLOCK_STONE_BREAK) return;
        if (breakerPlaySound.containsKey(block)) {
            breakerPlaySound.get(block).cancel();
            breakerPlaySound.remove(block);
        }
        BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), VANILLA_STONE_BREAK);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHitStone(final BlockDamageEvent event) {
        Block block = event.getBlock();
        SoundGroup soundGroup = block.getBlockData().getSoundGroup();

        if (event.getInstaBreak()) return;
        if (block.getType() == Material.BARRIER || soundGroup.getHitSound() != Sound.BLOCK_STONE_HIT) return;
        if (breakerPlaySound.containsKey(block)) return;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () ->
                BlockHelpers.playCustomBlockSound(block.getLocation(), VANILLA_STONE_HIT), 3L, 3L);
        breakerPlaySound.put(block, task);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStopHittingStone(final BlockDamageAbortEvent event) {
        Block block = event.getBlock();
        if (breakerPlaySound.containsKey(block)) {
            breakerPlaySound.get(block).cancel();
            breakerPlaySound.remove(block);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLimitedPlacing(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();

        if (item == null || block == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block.getType() != Material.NOTE_BLOCK) return;
        if (block.getType().isInteractable() && block.getType() != Material.NOTE_BLOCK) return;

        FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(OraxenItems.getIdByItem(item));
        if (mechanic == null || !mechanic.hasLimitedPlacing()) return;

        LimitedPlacing limitedPlacing = mechanic.getLimitedPlacing();
        Block placedAgainst = block.getRelative(event.getBlockFace()).getRelative(BlockFace.DOWN);

        if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.ALLOW) {
            if (!limitedPlacing.checkLimitedMechanic(placedAgainst))
                event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.DENY) {
            if (limitedPlacing.checkLimitedMechanic(placedAgainst))
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onHangingPlaceEvent(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        final Player player = event.getPlayer();
        final Block placedAgainst = event.getClickedBlock();
        assert placedAgainst != null;
        final Block target = getTarget(placedAgainst, event.getBlockFace());
        ItemStack item = event.getItem();

        // Cancel placing when clicking a clickAction furniture
        final PersistentDataContainer customBlockData = new CustomBlockData(placedAgainst, OraxenPlugin.get());
        if (customBlockData.has(FURNITURE_KEY, PersistentDataType.STRING)) {
            String id = customBlockData.get(FURNITURE_KEY, PersistentDataType.STRING);
            if (!OraxenItems.exists(id)) return;
            final FurnitureMechanic fMechanic = (FurnitureMechanic) factory.getMechanic(id);
            if (fMechanic.hasClickActions() && !player.isSneaking()) {
                if (item != null && item.getType().isBlock()) event.setCancelled(true);
                return;
            }
        }

        if (target == null) return;
        final BlockData currentBlockData = target.getBlockData();
        FurnitureMechanic mechanic = getMechanic(item, player, target);
        if (mechanic == null) return;

        Block farm = target.getRelative(BlockFace.DOWN);

        if (mechanic.farmlandRequired && farm.getType() != Material.FARMLAND) return;

        if (mechanic.farmblockRequired) {
            if (farm.getType() != Material.NOTE_BLOCK) return;
            NoteBlockMechanic farmMechanic = getNoteBlockMechanic(farm);
            if (farmMechanic == null || !farmMechanic.hasDryout()) return;
            if (!farmMechanic.getDryout().isFarmBlock()) return;
        }

        target.setType(Material.AIR, false);
        assert item != null;
        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, target.getState(), placedAgainst,
                item, player,
                true, Objects.requireNonNull(event.getHand()));

        final Rotation rotation = mechanic.hasRotation()
                ? mechanic.getRotation()
                : getRotation(player.getEyeLocation().getYaw(),
                mechanic.hasBarriers() && mechanic.getBarriers().size() > 1);
        final float yaw = mechanic.getYaw(rotation);
        if (player.getGameMode() == GameMode.ADVENTURE) blockPlaceEvent.setCancelled(true);
        if (!mechanic.isEnoughSpace(yaw, target.getLocation())) {
            blockPlaceEvent.setCancelled(true);
            Message.NOT_ENOUGH_SPACE.send(player);
        }

        Bukkit.getPluginManager().callEvent(blockPlaceEvent);

        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(currentBlockData, false); // false to cancel physic
            return;
        }

        mechanic.place(rotation, yaw, event.getBlockFace(), target.getLocation(), item);
        Utils.sendAnimation(player, event.getHand());
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
        if (factory.isNotImplementedIn(itemID) || isStandingInside(player, target)) return null;
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

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(final HangingBreakEvent event) {
        final PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
        if (pdc.has(FURNITURE_KEY, PersistentDataType.STRING)) {
            final ItemFrame frame = (ItemFrame) event.getEntity();

            if (event.getCause() == HangingBreakEvent.RemoveCause.ENTITY) return;
            event.setCancelled(true);

            final String itemID = pdc.get(FURNITURE_KEY, PersistentDataType.STRING);
            if (!OraxenItems.exists(itemID)) return;
            final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(itemID);
            if (mechanic.hasBarriers()) return;

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

                    OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic, player);
                    OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
                    if (furnitureBreakEvent.isCancelled()) {
                        return;
                    }

                    mechanic.removeAirFurniture(frame);
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        ItemStack itemInHand = player.getInventory().getItemInMainHand();
                        ItemMeta meta = frame.getItem().getItemMeta();
                        if (mechanic.hasEvolution())
                            mechanic.getDrop().spawns(frame.getLocation(), itemInHand);
                        else if (meta instanceof LeatherArmorMeta || meta instanceof PotionMeta) {
                            mechanic.getDrop().furnitureSpawns(frame, itemInHand);
                        } else mechanic.getDrop().spawns(frame.getLocation(), itemInHand);
                    }
                }
            }
    }

    @EventHandler
    public void onProjectileHitFurniture(final ProjectileHitEvent event) {
        Block hitBlock = event.getHitBlock();
        Entity hitEntity = event.getHitEntity();

        if (hitBlock != null && hitBlock.getType() == Material.BARRIER) {
            final PersistentDataContainer customBlockData = new CustomBlockData(hitBlock, OraxenPlugin.get());
            if (!customBlockData.has(FURNITURE_KEY, PersistentDataType.STRING)) return;
            final BlockLocation furnitureLocation = new BlockLocation(customBlockData.get(ROOT_KEY, PersistentDataType.STRING));
            Float orientation = customBlockData.get(ORIENTATION_KEY, PersistentDataType.FLOAT);

            final String itemID = customBlockData.get(FURNITURE_KEY, PersistentDataType.STRING);
            if (!OraxenItems.exists(itemID)) return;
            final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(itemID);
            ItemFrame frame = mechanic.getItemFrame(hitBlock, furnitureLocation, orientation);

            if (event.getEntity() instanceof Explosive) {
                mechanic.getDrop().furnitureSpawns(frame, new ItemStack(Material.AIR));
                mechanic.removeSolid(hitBlock.getWorld(), furnitureLocation, orientation);
            } else event.setCancelled(true);
        }
        if (hitEntity instanceof ItemFrame frame) {
            final PersistentDataContainer container = frame.getPersistentDataContainer();
            if (container.has(FURNITURE_KEY, PersistentDataType.STRING)) {
                final String itemID = container.get(FURNITURE_KEY, PersistentDataType.STRING);
                if (!OraxenItems.exists(itemID)) return;
                final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(itemID);
                if (hitEntity instanceof Explosive)
                    mechanic.getDrop().furnitureSpawns(frame, new ItemStack(Material.AIR));
                else event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnitureBreak(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block.getType() != Material.BARRIER || event.getPlayer().getGameMode() != GameMode.CREATIVE) return;

        final PersistentDataContainer customBlockData = new CustomBlockData(block, OraxenPlugin.get());
        if (!customBlockData.has(FURNITURE_KEY, PersistentDataType.STRING)) return;

        final String mechanicID = customBlockData.get(FURNITURE_KEY, PersistentDataType.STRING);
        final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(mechanicID);
        final BlockLocation rootBlockLocation = new BlockLocation(customBlockData.get(ROOT_KEY, PersistentDataType.STRING));

        OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic, event.getPlayer(), block);
        OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
        if (furnitureBreakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        mechanic.removeSolid(block.getWorld(), rootBlockLocation, customBlockData.get(ORIENTATION_KEY, PersistentDataType.FLOAT));
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
        final Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        if (block == null || block.getType() != Material.BARRIER || event.getPlayer().isSneaking()) return;

        final FurnitureMechanic mechanic = getFurnitureMechanic(block);

        Utils.sendAnimation(event.getPlayer(), event.getHand());

        if (mechanic != null) {
            // Call the oraxen furniture event
            final OraxenFurnitureInteractEvent furnitureInteractEvent = new OraxenFurnitureInteractEvent(mechanic, block, event.getPlayer());
            Bukkit.getPluginManager().callEvent(furnitureInteractEvent);

            if (furnitureInteractEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            if (mechanic.hasClickActions())
                mechanic.runClickActions(event.getPlayer());
        }

        final ArmorStand seat = getSeat(block.getLocation());
        if (seat == null || !seat.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) return;

        final String entityId = seat.getPersistentDataContainer().get(SEAT_KEY, PersistentDataType.STRING);
        final Entity stand = Bukkit.getEntity(UUID.fromString(entityId));

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
            FurnitureMechanic furnitureMechanic = getFurnitureMechanic(block);
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStepFall(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;
        Location eLoc = entity.getLocation();
        if (!isLoaded(event.getLocation()) || !isLoaded(eLoc)) return;

        GameEvent gameEvent = event.getEvent();
        Block block = entity.getLocation().getBlock();
        Block blockBelow = block.getRelative(BlockFace.DOWN);
        SoundGroup soundGroup = blockBelow.getBlockData().getSoundGroup();

        if (soundGroup.getStepSound() != Sound.BLOCK_STONE_STEP) return;
        if (!BlockHelpers.REPLACEABLE_BLOCKS.contains(block.getType()) || block.getType() == Material.TRIPWIRE) return;
        FurnitureMechanic mechanic = getFurnitureMechanic(blockBelow);

        String sound;
        if (gameEvent == GameEvent.STEP) {
            sound = (blockBelow.getType() == Material.BARRIER && mechanic != null && mechanic.hasStepSound())
                    ? mechanic.getStepSound() : VANILLA_STONE_STEP;
        } else if (gameEvent == GameEvent.HIT_GROUND) {
            sound = (blockBelow.getType() == Material.BARRIER && mechanic != null && mechanic.hasFallSound())
                    ? mechanic.getFallSound() : VANILLA_STONE_FALL;
        } else return;

        BlockHelpers.playCustomBlockSound(entity.getLocation(), sound, SoundCategory.PLAYERS);
    }

    private boolean isStandingInside(final Player player, final Block block) {
        final Location playerLocation = player.getLocation();
        final Location blockLocation = block.getLocation();
        return playerLocation.getBlockX() == blockLocation.getBlockX()
                && (playerLocation.getBlockY() == blockLocation.getBlockY()
                || playerLocation.getBlockY() + 1 == blockLocation.getBlockY())
                && playerLocation.getBlockZ() == blockLocation.getBlockZ();
    }

    public FurnitureMechanic getFurnitureMechanic(Block block) {
        if (block.getType() != Material.BARRIER) return null;
        final PersistentDataContainer customBlockData = new CustomBlockData(block, OraxenPlugin.get());
        final String mechanicID = customBlockData.get(FURNITURE_KEY, PersistentDataType.STRING);
        return (FurnitureMechanic) factory.getMechanic(mechanicID);
    }
}
