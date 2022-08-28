package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import com.jeff_media.customblockdata.CustomBlockData;
import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.lightapi.WrappedLightAPI;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Objects;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic.SAPLING_KEY;
import static io.th0rgal.oraxen.utils.BlockHelpers.isLoaded;

public class StringBlockMechanicListener implements Listener {

    private final MechanicFactory factory;

    public StringBlockMechanicListener(final StringBlockMechanicFactory factory) {
        this.factory = factory;
        BreakerSystem.MODIFIERS.add(getHardnessModifier());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void tripwireEvent(BlockPhysicsEvent event) {
        if (event.getChangedType() == Material.TRIPWIRE)
            event.setCancelled(true);

        for (BlockFace f : BlockFace.values()) {
            if (!f.isCartesian() || f.getModY() != 0 || f == BlockFace.SELF) continue; // Only take N/S/W/E
            final Block changed = event.getBlock().getRelative(f);
            if (changed.getType() != Material.TRIPWIRE) continue;

            final BlockData data = changed.getBlockData().clone();
            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                    changed.setBlockData(data, false), 1L);
        }
    }

    // Paper Only
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnteringTripwire(EntityInsideBlockEvent event) {
        if (event.getBlock().getType() == Material.TRIPWIRE)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingString(final BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.TRIPWIRE
                || OraxenItems.exists(OraxenItems.getIdByItem(event.getItemInHand())))
            return;

        // Placing string, meant for the first blockstate as invisible string
        if (event.getBlockPlaced().getType() == Material.TRIPWIRE)
            event.getBlock().setBlockData(Bukkit.createBlockData(Material.TRIPWIRE), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        var tripwireList = event.getBlocks().stream().filter(block -> block.getType().equals(Material.TRIPWIRE)).toList();

        for (Block block : tripwireList) {
            final StringBlockMechanic stringBlockMechanic = getStringMechanic(block);

            block.setType(Material.AIR, false);

            if (stringBlockMechanic == null) return;
            if (stringBlockMechanic.hasBreakSound())
                BlockHelpers.playCustomBlockSound(block.getLocation(), stringBlockMechanic.getBreakSound());
            if (stringBlockMechanic.getLight() != -1)
                WrappedLightAPI.removeBlockLight(block.getLocation());
            stringBlockMechanic.getDrop().spawns(block.getLocation(), new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && block != null
                && block.getType() == Material.TRIPWIRE) {
            final ItemStack clicked = event.getItem();
            event.setCancelled(true);
            if (clicked == null)
                return;
            Material type = clicked.getType();
            if (clicked.getType().isInteractable())
                return;
            if (type == Material.LAVA_BUCKET)
                type = Material.LAVA;
            if (type == Material.WATER_BUCKET)
                type = Material.WATER;
            if (type.isBlock())
                makePlayerPlaceBlock(event.getPlayer(), event.getHand(), event.getItem(), block, event.getBlockFace(), Bukkit.createBlockData(type));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Block blockAbove = block.getRelative(BlockFace.UP);
        final Player player = event.getPlayer();
        for (BlockFace face : BlockFace.values()) {
            if (face == BlockFace.SELF && !face.isCartesian()) continue;
            if (block.getType() == Material.TRIPWIRE || block.getType() == Material.NOTE_BLOCK) break;
            if (block.getRelative(face).getType() == Material.TRIPWIRE) {
                if (getStringMechanic(block.getRelative(face)) != null && player.getGameMode() != GameMode.CREATIVE)
                    for (ItemStack item : block.getDrops())
                        player.getWorld().dropItemNaturally(block.getLocation(), item);
                block.setType(Material.AIR, false);
                if (BlockHelpers.REPLACEABLE_BLOCKS.contains(blockAbove.getType())) blockAbove.breakNaturally();
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                        fixClientsideUpdate(block.getLocation()), 1);
            }
        }


        if (block.getType() == Material.TRIPWIRE) {
            final StringBlockMechanic stringBlockMechanic = getStringMechanic(block);
            if (stringBlockMechanic == null) return;
            event.setCancelled(true);
            breakStringBlock(block, stringBlockMechanic, player.getInventory().getItemInMainHand());
            event.setDropItems(false);
        } else if (blockAbove.getType() == Material.TRIPWIRE) {
            ItemStack item = player.getInventory().getItemInMainHand();
            final StringBlockMechanic stringBlockMechanic = getStringMechanic(blockAbove);
            if (stringBlockMechanic == null) return;
            event.setCancelled(true);
            block.breakNaturally(item);
            breakStringBlock(blockAbove, stringBlockMechanic, item);
            event.setDropItems(false);
        }
    }

    @EventHandler
    public void onExplosionDestroy(EntityExplodeEvent event) {
        List<Block> blockList = event.blockList().stream().filter(block -> block.getType().equals(Material.TRIPWIRE)).toList();
        blockList.forEach(block -> {
            final StringBlockMechanic stringBlockMechanic = getStringMechanic(block);
            if (stringBlockMechanic == null) return;

            stringBlockMechanic.getDrop().spawns(block.getLocation(), new ItemStack(Material.AIR));
            block.setType(Material.AIR, false);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        final Block placedAgainst = event.getClickedBlock();
        final Player player = event.getPlayer();

        if (placedAgainst == null) return;
        if (placedAgainst.getType().isInteractable() && !player.isSneaking()) {
            if (placedAgainst.getType() == Material.NOTE_BLOCK && getNoteBlockMechanic(placedAgainst) == null) return;
            else if (placedAgainst.getType() != Material.NOTE_BLOCK) return;
        }

        if (item != null && item.getType().isBlock() && !factory.isNotImplementedIn(itemID)) {
            for (BlockFace face : BlockFace.values()) {
                if (!face.isCartesian() || face.getModZ() != 0) continue;
                final Block relative = placedAgainst.getRelative(face);
                if (relative.getType() == Material.NOTE_BLOCK)
                    if (getNoteBlockMechanic(relative) == null) continue;
                if (relative.getType() == Material.TRIPWIRE)
                    if (getStringMechanic(relative) == null) continue;
                if (item.getItemMeta() instanceof BlockStateMeta) continue;
                if (item.getType().hasGravity()) continue;
                if (item.getType().toString().endsWith("SLAB")) continue;

                makePlayerPlaceBlock(player, event.getHand(), item, placedAgainst, event.getBlockFace(), Bukkit.createBlockData(item.getType()));
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                        fixClientsideUpdate(placedAgainst.getLocation()), 1L);
            }
        }

        if (factory.isNotImplementedIn(itemID)) return;
        // determines the new block data of the block
        StringBlockMechanic mechanic = (StringBlockMechanic) factory.getMechanic(itemID);
        final int customVariation = mechanic.getCustomVariation();

        Block placedBlock = makePlayerPlaceBlock(player, event.getHand(), event.getItem(),
                placedAgainst, event.getBlockFace(),
                StringBlockMechanicFactory.createTripwireData(customVariation));
        if (placedBlock == null)
            return;
        if (mechanic.hasPlaceSound())
            BlockHelpers.playCustomBlockSound(placedBlock.getLocation(), mechanic.getPlaceSound());
        if (mechanic.getLight() != -1)
            WrappedLightAPI.createBlockLight(placedBlock.getLocation(), mechanic.getLight());
        if (mechanic.isSapling()) {
            SaplingMechanic sapling = mechanic.getSaplingMechanic();
            final PersistentDataContainer pdc = new CustomBlockData(placedBlock, OraxenPlugin.get());
            if (mechanic.getSaplingMechanic().canGrowNaturally())
                pdc.set(SAPLING_KEY, PersistentDataType.INTEGER, sapling.getNaturalGrowthTime());
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWaterUpdate(final BlockFromToEvent event) {
        if (!event.getBlock().isLiquid()) return;
        for (BlockFace f : BlockFace.values()) {
            if (!f.isCartesian() || f == BlockFace.SELF) continue; // Only take N/S/W/E
            final Block changed = event.getToBlock().getRelative(f);
            if (changed.getType() == Material.TRIPWIRE) {
                breakStringBlock(changed, getStringMechanic(changed), new ItemStack(Material.AIR));
                changed.setType(Material.AIR, false);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStepFall(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;
        if (!isLoaded(event.getLocation())) return;
        GameEvent gameEvent = event.getEvent();
        Block block = entity.getLocation().getBlock();
        StringBlockMechanic mechanic = getStringMechanic(block);
        String sound;

        if (mechanic == null) return;
        if (gameEvent == GameEvent.STEP && mechanic.hasStepSound()) sound = mechanic.getStepSound();
        else if (gameEvent == GameEvent.HIT_GROUND && mechanic.hasStepSound()) sound = mechanic.getFallSound();
        else return;
        BlockHelpers.playCustomBlockSound(entity.getLocation(), sound, SoundCategory.PLAYERS);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(final InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE) return;
        final Player player = (Player) event.getInventory().getHolder();
        if (player == null) return;
        if (event.getCursor().getType() == Material.STRING) {
            final RayTraceResult rayTraceResult = player.rayTraceBlocks(6.0);
            if (rayTraceResult == null) return;
            final Block block = rayTraceResult.getHitBlock();
            if (block == null) return;
            StringBlockMechanic stringBlockMechanic = getStringMechanic(block);
            if (stringBlockMechanic == null) return;
            ItemStack item = OraxenItems.getItemById(stringBlockMechanic.getItemID()).build();
            for (int i = 0; i <= 8; i++) {
                if (player.getInventory().getItem(i) == null) continue;
                if (Objects.equals(OraxenItems.getIdByItem(player.getInventory().getItem(i)), stringBlockMechanic.getItemID())) {
                    player.getInventory().setHeldItemSlot(i);
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCursor(item);
        }
    }

    public static StringBlockMechanic getStringMechanic(Block block) {
        if (block.getType() == Material.TRIPWIRE) {
            final Tripwire tripwire = (Tripwire) block.getBlockData();
            return StringBlockMechanicFactory.getBlockMechanic(StringBlockMechanicFactory.getCode(tripwire));
        } else return null;
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                if (block.getType() != Material.TRIPWIRE)
                    return false;
                final StringBlockMechanic tripwireMechanic = getStringMechanic(block);
                return tripwireMechanic != null && tripwireMechanic.hasHardness;
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                final StringBlockMechanic tripwireMechanic = getStringMechanic(block);
                if (tripwireMechanic == null) return 0;
                final long period = tripwireMechanic.getPeriod();
                double modifier = 1;
                if (tripwireMechanic.getDrop().canDrop(tool)) {
                    modifier *= 0.4;
                    final int diff = tripwireMechanic.getDrop().getDiff(tool);
                    if (diff >= 1)
                        modifier *= Math.pow(0.9, diff);
                }
                return (long) (period * modifier);
            }
        };
    }

    private boolean isStandingInside(final Entity entity, final Block block) {
        final Location entityLocation = entity.getLocation();
        final Location blockLocation = block.getLocation();
        return BlockHelpers.toBlockLocation(entityLocation).equals(BlockHelpers.toBlockLocation(blockLocation)) ||
                BlockHelpers.toBlockLocation(entityLocation).equals(BlockHelpers.toBlockLocation(blockLocation).add(0, 1.0, 0));
    }

    private Block makePlayerPlaceBlock(final Player player, final EquipmentSlot hand, final ItemStack item,
                                       final Block placedAgainst, final BlockFace face, final BlockData newBlock) {
        final Block target;
        final Material type = placedAgainst.getType();
        if (BlockHelpers.REPLACEABLE_BLOCKS.contains(type))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(face);
            if (!target.getType().isAir() && target.getType() != Material.WATER && target.getType() != Material.LAVA)
                return null;
        }
        if (isStandingInside(player, target) || !ProtectionLib.canBuild(player, target.getLocation()))
            return null;

        // determines the old informations of the block
        final BlockData curentBlockData = target.getBlockData();
        target.setBlockData(newBlock, false);
        final BlockState currentBlockState = target.getState();

        final BlockPlaceEvent blockPlaceEvent =
                new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player, true, hand);
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);

        if (player.getGameMode() == GameMode.ADVENTURE) blockPlaceEvent.setCancelled(true);
        if (BlockHelpers.correctAllBlockStates(target, player, face, item)) blockPlaceEvent.setCancelled(true);

        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return null;
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);

        return target;
    }

    private void breakStringBlock(Block block, StringBlockMechanic mechanic, ItemStack item) {
        if (mechanic == null) return;
        if (mechanic.hasBreakSound())
            BlockHelpers.playCustomBlockSound(block.getLocation(), mechanic.getBreakSound());
        if (mechanic.getLight() != -1)
            WrappedLightAPI.removeBlockLight(block.getLocation());
        mechanic.getDrop().spawns(block.getLocation(), item);
        block.setType(Material.AIR, false);
        final Block blockAbove = block.getRelative(BlockFace.UP);
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
            fixClientsideUpdate(block.getLocation());
            if (blockAbove.getType() == Material.TRIPWIRE)
                breakStringBlock(blockAbove, getStringMechanic(blockAbove), new ItemStack(Material.AIR));
        }, 1L);
    }

    private static void fixClientsideUpdate(Location blockLoc) {
        Block blockBelow = blockLoc.clone().subtract(0, 1, 0).getBlock();
        Block blockAbove = blockLoc.clone().add(0, 1, 0).getBlock();
        Location loc = blockLoc.add(5, 0, 5);
        List<Entity> players =
                Objects.requireNonNull(blockLoc.getWorld()).getNearbyEntities(blockLoc, 20, 20, 20)
                        .stream().filter(entity -> entity.getType() == EntityType.PLAYER).toList();

        if (players.isEmpty()) return;
        if (blockBelow.getType() == Material.TRIPWIRE) {
            for (Entity e : players)
                ((Player) e).sendBlockChange(blockBelow.getLocation(), blockBelow.getBlockData());
        }

        if (blockAbove.getType() == Material.TRIPWIRE) {
            for (Entity e : players)
                ((Player) e).sendBlockChange(blockAbove.getLocation(), blockAbove.getBlockData());
        }

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (loc.getBlock().getType() == Material.TRIPWIRE) {
                    for (Entity e : players)
                        ((Player) e).sendBlockChange(loc, loc.getBlock().getBlockData());
                }
                loc = loc.subtract(0, 0, 1);
            }
            loc = loc.add(-1, 0, 9);
        }
    }

    private boolean checkSurroundingBlocks(Block block) {
        for (BlockFace face : BlockFace.values()) {
            if (face == BlockFace.SELF) continue;
            if (block.getRelative(face).getType() == Material.TRIPWIRE) return true;
        }
        return false;
    }
}
