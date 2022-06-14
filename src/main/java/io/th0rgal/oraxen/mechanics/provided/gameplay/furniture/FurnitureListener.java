package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.events.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.events.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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
            if (!getNoteBlockMechanic(farm).hasDryout()) return;
            if (!getNoteBlockMechanic(farm).getDryout().isFarmBlock()) return;
        }

        target.setType(Material.AIR, false);
        assert item != null;
        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, target.getState(), placedAgainst,
                item, player,
                true, Objects.requireNonNull(event.getHand()));

        final Rotation rotation = mechanic.hasRotation()
                ? mechanic.getRotation()
                : getRotation(player.getEyeLocation().getYaw(),
                mechanic.hasBarriers()
                        && mechanic.getBarriers().size() > 1);

        final float yaw = mechanic.getYaw(rotation) + mechanic.getSeatYaw();

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
        if (factory.isNotImplementedIn(itemID))
            return null;

        if (isStandingInside(player, target)
                || !ProtectionLib.canBuild(player, target.getLocation()))
            return null;

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

                    OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic,player);
                    OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
                    if (furnitureBreakEvent.isCancelled()) {
                        return;
                    }

                    mechanic.removeAirFurniture(frame);
                    if (player.getGameMode() != GameMode.CREATIVE)
                        mechanic.getDrop().spawns(frame.getLocation(), player.getInventory().getItemInMainHand());
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
        if (block.getType() != Material.BARRIER || event.getPlayer().getGameMode() != GameMode.CREATIVE)
            return;

        final PersistentDataContainer customBlockData = new CustomBlockData(block, OraxenPlugin.get());
        if (!customBlockData.has(FURNITURE_KEY, PersistentDataType.STRING))
            return;
        final String mechanicID = customBlockData.get(FURNITURE_KEY, PersistentDataType.STRING);
        final FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(mechanicID);
        final BlockLocation rootBlockLocation = new BlockLocation(customBlockData.get(ROOT_KEY,
                PersistentDataType.STRING));

        OraxenFurnitureBreakEvent furnitureBreakEvent = new OraxenFurnitureBreakEvent(mechanic,event.getPlayer(), block);
        OraxenPlugin.get().getServer().getPluginManager().callEvent(furnitureBreakEvent);
        if (furnitureBreakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        mechanic.removeSolid(block.getWorld(), rootBlockLocation, customBlockData
                .get(ORIENTATION_KEY, PersistentDataType.FLOAT));
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
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final Block block = event.getClickedBlock();

        if (block == null || block.getType() != Material.BARRIER || event.getPlayer().isSneaking()) {
            return;
        }

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
            final Block block = player.rayTraceBlocks(6.0).getHitBlock();
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

    public NoteBlockMechanic getNoteBlockMechanic(Block block) {
        if (block.getType() != Material.NOTE_BLOCK) return null;
        final NoteBlock noteBlock = (NoteBlock) block.getBlockData();
        return NoteBlockMechanicFactory
                .getBlockMechanic((int) (noteBlock.getInstrument().getType()) * 25
                        + (int) noteBlock.getNote().getId() + (noteBlock.isPowered() ? 400 : 0) - 26);
    }

}
