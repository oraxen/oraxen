package io.th0rgal.oraxen.mechanics.provided.gameplay.shaped;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.protectionlib.ProtectionLib;
import org.jetbrains.annotations.Nullable;
import org.apache.commons.lang3.Range;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.Chunk;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles placement, breaking, and interactions for shaped blocks (stairs, slabs, doors, trapdoors, grates, bulbs).
 *
 * Custom blocks use WAXED copper materials with custom textures.
 * Vanilla waxed copper is converted to NON-WAXED copper and marked to prevent oxidation,
 * preserving vanilla copper functionality while avoiding texture conflicts.
 *
 * Bulbs (copper bulbs) are particularly useful for transparent full blocks like custom leaves.
 */
public class ShapedBlockMechanicListener implements Listener {

    private static final NamespacedKey VANILLA_WAXED_KEY = new NamespacedKey(OraxenPlugin.get(), "vanilla_waxed");
    private final ShapedBlockMechanicFactory factory;

    public ShapedBlockMechanicListener(ShapedBlockMechanicFactory factory) {
        this.factory = factory;
    }

    // ==================== VANILLA COPPER HANDLING ====================

    /**
     * Convert vanilla waxed copper blocks to non-waxed copper when placed.
     * The block is marked as "vanilla_waxed" to prevent oxidation while keeping vanilla appearance.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceVanillaWaxed(BlockPlaceEvent event) {
        if (!factory.convertVanillaWaxed()) return;

        Block placed = event.getBlockPlaced();
        Material material = placed.getType();

        // Check if this is a waxed copper material used by shaped blocks
        if (!ShapedBlockType.isWaxedCopper(material)) return;

        // Check if the item used is a custom Oraxen item (don't convert those)
        ItemStack item = event.getItemInHand();
        if (OraxenItems.exists(item)) return;

        // Convert waxed to non-waxed equivalent
        Material vanillaMaterial = ShapedBlockType.toVanilla(material);
        if (vanillaMaterial == null) return;

        // Copy block data to new material
        BlockData oldData = placed.getBlockData();
        BlockData newData = vanillaMaterial.createBlockData();
        copyBlockData(oldData, newData);
        placed.setBlockData(newData, false);

        // Mark as vanilla waxed to prevent oxidation
        setVanillaWaxed(placed, true);
    }

    /**
     * Handle honeycomb waxing of vanilla (non-waxed) copper blocks.
     * Instead of converting to waxed copper, mark as "vanilla_waxed".
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWaxVanillaCopper(PlayerInteractEvent event) {
        if (!factory.convertVanillaWaxed()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.useItemInHand() == Event.Result.DENY) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.HONEYCOMB) return;

        // Check if it's a vanilla copper block (not already waxed)
        if (!ShapedBlockType.isVanillaCopper(block.getType())) return;

        // Don't wax if already marked as vanilla waxed
        if (isVanillaWaxed(block)) return;

        // Cancel vanilla waxing behavior
        event.setCancelled(true);

        // Mark as waxed instead of converting material
        setVanillaWaxed(block, true);

        // Consume honeycomb
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }

        // Play wax sound
        block.getWorld().playSound(block.getLocation(), Sound.ITEM_HONEYCOMB_WAX_ON, 1.0f, 1.0f);
    }

    /**
     * Handle axe scraping of vanilla waxed copper blocks.
     * Removes the "vanilla_waxed" marker, allowing oxidation again.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUnwaxVanillaCopper(PlayerInteractEvent event) {
        if (!factory.convertVanillaWaxed()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.useItemInHand() == Event.Result.DENY) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        ItemStack item = event.getItem();
        if (item == null || !Tag.ITEMS_AXES.isTagged(item.getType())) return;

        // Check if it's a vanilla copper block that's marked as waxed
        if (!ShapedBlockType.isVanillaCopper(block.getType())) return;
        if (!isVanillaWaxed(block)) return;

        // Cancel vanilla behavior
        event.setCancelled(true);

        // Remove waxed marker
        setVanillaWaxed(block, false);

        // Play scrape sound
        block.getWorld().playSound(block.getLocation(), Sound.ITEM_AXE_WAX_OFF, 1.0f, 1.0f);
    }

    /**
     * Prevent unwaxing of custom shaped blocks with an axe.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUnwaxCustomBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.useItemInHand() == Event.Result.DENY) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        ItemStack item = event.getItem();
        if (item == null || !Tag.ITEMS_AXES.isTagged(item.getType())) return;

        // Check if it's a custom shaped block (waxed copper)
        if (!ShapedBlockType.isWaxedCopper(block.getType())) return;

        // Prevent unwaxing custom blocks
        event.setCancelled(true);
    }

    /**
     * Prevent oxidation of both custom blocks and vanilla waxed copper.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOxidize(BlockFormEvent event) {
        if (!factory.convertVanillaWaxed()) return;

        Block block = event.getBlock();
        Material newType = event.getNewState().getType();

        // Prevent oxidation of custom shaped blocks (waxed copper)
        if (ShapedBlockType.isWaxedCopper(block.getType())) {
            event.setCancelled(true);
            return;
        }

        // Prevent oxidation of vanilla copper marked as waxed
        if (ShapedBlockType.isVanillaCopper(block.getType()) && isVanillaWaxed(block)) {
            event.setCancelled(true);
        }
    }

    // ==================== WORLD GENERATION ====================

    /**
     * Handle waxed copper blocks in newly generated chunks (e.g., Trial Chambers).
     * Converts them to non-waxed variants with oxidation prevention marker.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!factory.handleWorldGeneration()) return;
        if (!event.isNewChunk()) return;

        Chunk chunk = event.getChunk();

        // Schedule conversion for next tick to ensure chunk is fully loaded
        SchedulerUtil.runTaskLater(1L, () -> convertWaxedCopperInChunk(chunk));
    }

    /**
     * Convert all waxed copper blocks in a chunk to non-waxed variants.
     */
    private void convertWaxedCopperInChunk(Chunk chunk) {
        int minY = chunk.getWorld().getMinHeight();
        int maxY = chunk.getWorld().getMaxHeight();

        int converted = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material material = block.getType();

                    if (!ShapedBlockType.isWaxedCopper(material)) continue;

                    Material vanillaMaterial = ShapedBlockType.toVanilla(material);
                    if (vanillaMaterial == null) continue;

                    // Copy block data to new material
                    BlockData oldData = block.getBlockData();
                    BlockData newData = vanillaMaterial.createBlockData();
                    copyBlockData(oldData, newData);
                    block.setBlockData(newData, false);

                    // Mark as vanilla waxed to prevent oxidation
                    setVanillaWaxed(block, true);
                    converted++;
                }
            }
        }

        if (converted > 0) {
            Logs.logInfo("[ShapedBlock] Converted " + converted + " waxed copper blocks in chunk " +
                chunk.getX() + ", " + chunk.getZ());
        }
    }

    // ==================== CUSTOM BLOCK PLACEMENT ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceCustomShapedBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return;

        String itemId = OraxenItems.getIdByItem(item);
        if (itemId == null) return;

        Mechanic mechanic = factory.getMechanic(itemId);
        if (!(mechanic instanceof ShapedBlockMechanic shapedMechanic)) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        BlockFace face = event.getBlockFace();
        Block targetBlock = getTargetBlock(clickedBlock, face);

        // Handle slab double placement
        if (shapedMechanic.getBlockType() == ShapedBlockType.SLAB) {
            if (handleSlabPlacement(event, player, item, shapedMechanic, clickedBlock, face, targetBlock)) {
                return;
            }
        }

        // Validate placement
        if (!canPlaceBlock(player, targetBlock, clickedBlock, shapedMechanic)) return;

        // Additional door validation
        if (shapedMechanic.getBlockType() == ShapedBlockType.DOOR) {
            if (!canPlaceDoor(player, targetBlock)) return;
        }

        // Cancel the normal interaction and place the block
        event.setCancelled(true);
        Location interactionPoint = event.getInteractionPoint();
        placeShapedBlock(player, item, targetBlock, clickedBlock, face, shapedMechanic, itemId, interactionPoint);
    }

    private Block getTargetBlock(Block clickedBlock, BlockFace face) {
        if (BlockHelpers.isReplaceable(clickedBlock.getType())) {
            return clickedBlock;
        }
        return clickedBlock.getRelative(face);
    }

    private boolean canPlaceBlock(Player player, Block targetBlock, Block clickedBlock, ShapedBlockMechanic mechanic) {
        if (!BlockHelpers.isReplaceable(targetBlock.getType())) return false;

        Range<Integer> worldHeightRange = Range.of(
            targetBlock.getWorld().getMinHeight(),
            targetBlock.getWorld().getMaxHeight() - 1
        );
        if (!worldHeightRange.contains(targetBlock.getY())) return false;

        if (BlockHelpers.isStandingInside(player, targetBlock)) return false;
        if (!ProtectionLib.canBuild(player, targetBlock.getLocation())) return false;

        if (mechanic.hasLimitedPlacing()) {
            if (!mechanic.getLimitedPlacing().checkLimitedMechanic(clickedBlock)) return false;
        }
        return true;
    }

    private boolean canPlaceDoor(Player player, Block targetBlock) {
        Block upperBlock = targetBlock.getRelative(BlockFace.UP);
        if (!BlockHelpers.isReplaceable(upperBlock.getType())) return false;
        if (BlockHelpers.isStandingInside(player, upperBlock)) return false;
        if (!ProtectionLib.canBuild(player, upperBlock.getLocation())) return false;
        return true;
    }

    private void placeShapedBlock(Player player, ItemStack item, Block targetBlock, Block clickedBlock,
                                   BlockFace face, ShapedBlockMechanic shapedMechanic, String itemId,
                                   @Nullable Location interactionPoint) {
        boolean isWaterlogged = targetBlock.getType() == Material.WATER;
        Material placeMaterial = shapedMechanic.getPlacedMaterial();

        // Capture replaced state BEFORE modifying the block (for BlockPlaceEvent)
        org.bukkit.block.BlockState replacedState = targetBlock.getState();

        targetBlock.setType(placeMaterial, false);
        applyBlockData(targetBlock, player, face, shapedMechanic, isWaterlogged, interactionPoint);
        markAsCustomBlock(targetBlock, shapedMechanic);

        // Handle door upper block
        if (shapedMechanic.getBlockType() == ShapedBlockType.DOOR) {
            Block upperBlock = targetBlock.getRelative(BlockFace.UP);
            upperBlock.setType(placeMaterial, false);
            applyDoorUpperData(upperBlock, player);
            markAsCustomBlock(upperBlock, shapedMechanic);
        }

        // Fire BlockPlaceEvent for compatibility (with correct replaced state)
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(
            targetBlock, replacedState, clickedBlock, item, player, true, EquipmentSlot.HAND
        );
        Bukkit.getPluginManager().callEvent(placeEvent);

        if (placeEvent.isCancelled()) {
            revertPlacement(targetBlock, shapedMechanic);
            return;
        }

        playPlaceSound(targetBlock, shapedMechanic);

        // Optional light support via LightMechanic (places minecraft:light blocks nearby).
        // This does not change the underlying placed block's own light level, but still
        // provides illumination around the custom block.
        if (shapedMechanic.hasLight()) {
            shapedMechanic.getLight().createBlockLight(targetBlock);
            if (shapedMechanic.getBlockType() == ShapedBlockType.DOOR) {
                shapedMechanic.getLight().createBlockLight(targetBlock.getRelative(BlockFace.UP));
            }
        }

        consumeItem(player, item);
        Logs.logSuccess("[ShapedBlock] Placed " + itemId + " as " + shapedMechanic.getBlockType());
    }

    private void revertPlacement(Block targetBlock, ShapedBlockMechanic mechanic) {
        // Clean up PDC data before removing blocks
        CustomBlockData blockData = new CustomBlockData(targetBlock, OraxenPlugin.get());
        blockData.remove(ShapedBlockMechanic.SHAPED_BLOCK_KEY);

        targetBlock.setType(Material.AIR);
        if (mechanic.getBlockType() == ShapedBlockType.DOOR) {
            Block upperBlock = targetBlock.getRelative(BlockFace.UP);
            CustomBlockData upperBlockData = new CustomBlockData(upperBlock, OraxenPlugin.get());
            upperBlockData.remove(ShapedBlockMechanic.SHAPED_BLOCK_KEY);
            upperBlock.setType(Material.AIR);
        }
    }

    private void playPlaceSound(Block block, ShapedBlockMechanic mechanic) {
        if (mechanic.hasBlockSounds()) {
            BlockSounds sounds = mechanic.getBlockSounds();
            if (sounds.hasPlaceSound()) {
                BlockHelpers.playCustomBlockSound(block.getLocation(), sounds.getPlaceSound(), sounds.getPlaceVolume(), sounds.getPlacePitch());
            }
        } else {
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_COPPER_PLACE, 1.0f, 1.0f);
        }
    }

    private void consumeItem(Player player, ItemStack item) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }

    // ==================== CUSTOM BLOCK BREAKING ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakCustomShapedBlock(BlockBreakEvent event) {
        Block block = event.getBlock();
        ShapedBlockMechanic mechanic = getMechanicFromBlock(block);

        if (mechanic == null) return;

        Player player = event.getPlayer();

        // Check protection plugins before allowing break
        if (!ProtectionLib.canBreak(player, block.getLocation())) {
            event.setCancelled(true);
            return;
        }

        // Handle drops
        event.setDropItems(false);
        if (player.getGameMode() != GameMode.CREATIVE) {
            Drop drop = mechanic.getDrop();
            if (drop != null) {
                // Double slabs should drop twice
                int dropCount = 1;
                if (mechanic.getBlockType() == ShapedBlockType.SLAB) {
                    BlockData data = block.getBlockData();
                    if (data instanceof Slab slab && slab.getType() == Slab.Type.DOUBLE) {
                        dropCount = 2;
                    }
                }
                ItemStack tool = player.getInventory().getItemInMainHand();
                for (int i = 0; i < dropCount; i++) {
                    drop.spawns(block.getLocation(), tool);
                }
            }
        }

        // Play break sound
        if (mechanic.hasBlockSounds()) {
            BlockSounds sounds = mechanic.getBlockSounds();
            if (sounds.hasBreakSound()) {
                BlockHelpers.playCustomBlockSound(block.getLocation(), sounds.getBreakSound(), sounds.getBreakVolume(), sounds.getBreakPitch());
            }
        }

        // Remove any minecraft:light blocks placed by this mechanic
        if (mechanic.hasLight()) {
            mechanic.getLight().removeBlockLight(block);
        }

        // Clean up custom block data
        CustomBlockData blockData = new CustomBlockData(block, OraxenPlugin.get());
        blockData.remove(ShapedBlockMechanic.SHAPED_BLOCK_KEY);

        // For doors, also break the other half
        if (mechanic.getBlockType() == ShapedBlockType.DOOR) {
            BlockData data = block.getBlockData();
            if (data instanceof org.bukkit.block.data.type.Door door) {
                Block otherHalf = door.getHalf() == org.bukkit.block.data.Bisected.Half.BOTTOM
                    ? block.getRelative(BlockFace.UP)
                    : block.getRelative(BlockFace.DOWN);
                if (mechanic.hasLight()) {
                    mechanic.getLight().removeBlockLight(otherHalf);
                }
                // Clean up the other half's data
                CustomBlockData otherBlockData = new CustomBlockData(otherHalf, OraxenPlugin.get());
                otherBlockData.remove(ShapedBlockMechanic.SHAPED_BLOCK_KEY);
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private boolean handleSlabPlacement(PlayerInteractEvent event, Player player, ItemStack item,
                                        ShapedBlockMechanic shapedMechanic, Block clickedBlock,
                                        BlockFace face, Block targetBlock) {
        // Check if clicking on existing slab of same type to make double slab
        if (clickedBlock.getType() == shapedMechanic.getPlacedMaterial()) {
            BlockData data = clickedBlock.getBlockData();
            if (data instanceof Slab slab) {
                if (slab.getType() != Slab.Type.DOUBLE) {
                    boolean shouldDouble = false;
                    if (slab.getType() == Slab.Type.BOTTOM && face == BlockFace.UP) {
                        shouldDouble = true;
                    } else if (slab.getType() == Slab.Type.TOP && face == BlockFace.DOWN) {
                        shouldDouble = true;
                    }

                    if (shouldDouble) {
                        if (!ProtectionLib.canBuild(player, clickedBlock.getLocation())) {
                            return false;
                        }

                        event.setCancelled(true);
                        slab.setType(Slab.Type.DOUBLE);
                        clickedBlock.setBlockData(slab);

                        if (shapedMechanic.hasBlockSounds()) {
                            BlockSounds sounds = shapedMechanic.getBlockSounds();
                            if (sounds.hasPlaceSound()) {
                                BlockHelpers.playCustomBlockSound(clickedBlock.getLocation(), sounds.getPlaceSound(), sounds.getPlaceVolume(), sounds.getPlacePitch());
                            }
                        } else {
                            clickedBlock.getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_COPPER_PLACE, 1.0f, 1.0f);
                        }

                        if (player.getGameMode() != GameMode.CREATIVE) {
                            item.setAmount(item.getAmount() - 1);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void applyBlockData(Block block, Player player, BlockFace clickedFace, ShapedBlockMechanic mechanic,
                                 boolean isWaterlogged, @Nullable Location interactionPoint) {
        BlockData data = block.getBlockData();

        switch (mechanic.getBlockType()) {
            case STAIR -> applyStairData(data, player, clickedFace, interactionPoint);
            case SLAB -> applySlabData(data, player, clickedFace, interactionPoint);
            case DOOR -> applyDoorData(data, player);
            case TRAPDOOR -> applyTrapdoorData(data, player, clickedFace);
            case GRATE -> { /* Grates have no directional data */ }
            case BULB -> applyBulbData(data);
        }

        if (isWaterlogged && data instanceof Waterlogged waterloggedData) {
            waterloggedData.setWaterlogged(true);
        }

        block.setBlockData(data, false);
    }

    private void applyStairData(BlockData data, Player player, BlockFace clickedFace, @Nullable Location interactionPoint) {
        if (data instanceof org.bukkit.block.data.type.Stairs stairs) {
            BlockFace playerFacing = player.getFacing();
            stairs.setFacing(playerFacing);

            if (clickedFace == BlockFace.UP) {
                stairs.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
            } else if (clickedFace == BlockFace.DOWN) {
                stairs.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
            } else {
                // Side face: use click position on the block to determine top/bottom
                double clickY = interactionPoint != null ? interactionPoint.getY() % 1 : 0.5;
                stairs.setHalf(clickY > 0.5 ?
                    org.bukkit.block.data.Bisected.Half.TOP :
                    org.bukkit.block.data.Bisected.Half.BOTTOM);
            }
        }
    }

    private void applySlabData(BlockData data, Player player, BlockFace clickedFace, @Nullable Location interactionPoint) {
        if (data instanceof Slab slab) {
            if (clickedFace == BlockFace.UP) {
                slab.setType(Slab.Type.BOTTOM);
            } else if (clickedFace == BlockFace.DOWN) {
                slab.setType(Slab.Type.TOP);
            } else {
                // Side face: use click position on the block to determine top/bottom
                double clickY = interactionPoint != null ? interactionPoint.getY() % 1 : 0.5;
                slab.setType(clickY > 0.5 ? Slab.Type.TOP : Slab.Type.BOTTOM);
            }
        }
    }

    private void applyDoorData(BlockData data, Player player) {
        if (data instanceof org.bukkit.block.data.type.Door door) {
            door.setFacing(player.getFacing());
            door.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
            door.setOpen(false);
            door.setHinge(org.bukkit.block.data.type.Door.Hinge.LEFT);
        }
    }

    private void applyDoorUpperData(Block block, Player player) {
        BlockData data = block.getBlockData();
        if (data instanceof org.bukkit.block.data.type.Door door) {
            door.setFacing(player.getFacing());
            door.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
            door.setOpen(false);
            door.setHinge(org.bukkit.block.data.type.Door.Hinge.LEFT);
            block.setBlockData(data, false);
        }
    }

    private void applyTrapdoorData(BlockData data, Player player, BlockFace clickedFace) {
        if (data instanceof org.bukkit.block.data.type.TrapDoor trapdoor) {
            trapdoor.setFacing(player.getFacing());
            trapdoor.setOpen(false);
            if (clickedFace == BlockFace.UP) {
                trapdoor.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
            } else if (clickedFace == BlockFace.DOWN) {
                trapdoor.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
            } else {
                // Side face: determine TOP/BOTTOM based on click position
                double clickY = player.getEyeLocation().getY() % 1;
                trapdoor.setHalf(clickY > 0.5 ?
                    org.bukkit.block.data.Bisected.Half.TOP :
                    org.bukkit.block.data.Bisected.Half.BOTTOM);
            }
        }
    }

    private void applyBulbData(BlockData data) {
        // Copper bulbs have lit and powered states
        // For custom blocks (like leaves), we want them always unlit and unpowered
        if (data instanceof org.bukkit.block.data.type.CopperBulb bulb) {
            bulb.setLit(false);
            bulb.setPowered(false);
        }
    }

    private void markAsCustomBlock(Block block, ShapedBlockMechanic mechanic) {
        CustomBlockData blockData = new CustomBlockData(block, OraxenPlugin.get());
        blockData.set(ShapedBlockMechanic.SHAPED_BLOCK_KEY, PersistentDataType.STRING, mechanic.getItemID());
    }

    /**
     * Copy block data properties from one BlockData to another (same block type family).
     */
    private void copyBlockData(BlockData from, BlockData to) {
        if (from instanceof org.bukkit.block.data.type.Stairs fromStairs && to instanceof org.bukkit.block.data.type.Stairs toStairs) {
            copyStairsData(fromStairs, toStairs);
        } else if (from instanceof Slab fromSlab && to instanceof Slab toSlab) {
            copySlabData(fromSlab, toSlab);
        } else if (from instanceof org.bukkit.block.data.type.Door fromDoor && to instanceof org.bukkit.block.data.type.Door toDoor) {
            copyDoorData(fromDoor, toDoor);
        } else if (from instanceof org.bukkit.block.data.type.TrapDoor fromTrap && to instanceof org.bukkit.block.data.type.TrapDoor toTrap) {
            copyTrapDoorData(fromTrap, toTrap);
        } else if (from instanceof org.bukkit.block.data.type.CopperBulb fromBulb && to instanceof org.bukkit.block.data.type.CopperBulb toBulb) {
            copyBulbData(fromBulb, toBulb);
        } else {
            copyWaterloggedData(from, to);
        }
    }

    private void copyStairsData(org.bukkit.block.data.type.Stairs from, org.bukkit.block.data.type.Stairs to) {
        to.setFacing(from.getFacing());
        to.setHalf(from.getHalf());
        to.setShape(from.getShape());
        copyWaterloggedData(from, to);
    }

    private void copySlabData(Slab from, Slab to) {
        to.setType(from.getType());
        copyWaterloggedData(from, to);
    }

    private void copyDoorData(org.bukkit.block.data.type.Door from, org.bukkit.block.data.type.Door to) {
        to.setFacing(from.getFacing());
        to.setHalf(from.getHalf());
        to.setHinge(from.getHinge());
        to.setOpen(from.isOpen());
        to.setPowered(from.isPowered());
    }

    private void copyTrapDoorData(org.bukkit.block.data.type.TrapDoor from, org.bukkit.block.data.type.TrapDoor to) {
        to.setFacing(from.getFacing());
        to.setHalf(from.getHalf());
        to.setOpen(from.isOpen());
        to.setPowered(from.isPowered());
        copyWaterloggedData(from, to);
    }

    private void copyBulbData(org.bukkit.block.data.type.CopperBulb from, org.bukkit.block.data.type.CopperBulb to) {
        to.setLit(from.isLit());
        to.setPowered(from.isPowered());
    }

    private void copyWaterloggedData(BlockData from, BlockData to) {
        if (from instanceof Waterlogged fromWL && to instanceof Waterlogged toWL) {
            toWL.setWaterlogged(fromWL.isWaterlogged());
        }
    }

    // ==================== VANILLA WAXED MARKER METHODS ====================

    /**
     * Check if a block is marked as vanilla waxed (non-waxed copper that should not oxidize).
     */
    public static boolean isVanillaWaxed(Block block) {
        CustomBlockData blockData = new CustomBlockData(block, OraxenPlugin.get());
        return blockData.getOrDefault(VANILLA_WAXED_KEY, PersistentDataType.BOOLEAN, false);
    }

    /**
     * Set the vanilla waxed marker on a block.
     */
    public static void setVanillaWaxed(Block block, boolean waxed) {
        CustomBlockData blockData = new CustomBlockData(block, OraxenPlugin.get());
        if (waxed) {
            blockData.set(VANILLA_WAXED_KEY, PersistentDataType.BOOLEAN, true);
        } else {
            blockData.remove(VANILLA_WAXED_KEY);
        }
    }

    /**
     * Get the mechanic for a placed shaped block.
     */
    public ShapedBlockMechanic getMechanicFromBlock(Block block) {
        Material material = block.getType();

        // First check if it's a registered shaped block material
        if (!factory.isCustomShapedBlock(material)) return null;

        // Check if it has our custom marker
        CustomBlockData blockData = new CustomBlockData(block, OraxenPlugin.get());
        String itemId = blockData.get(ShapedBlockMechanic.SHAPED_BLOCK_KEY, PersistentDataType.STRING);
        if (itemId == null) return null;

        Mechanic mechanic = factory.getMechanic(itemId);
        if (mechanic instanceof ShapedBlockMechanic shapedMechanic) {
            return shapedMechanic;
        }
        return null;
    }
}
