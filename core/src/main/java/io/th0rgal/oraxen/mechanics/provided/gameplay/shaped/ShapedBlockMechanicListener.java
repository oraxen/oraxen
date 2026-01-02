package io.th0rgal.oraxen.mechanics.provided.gameplay.shaped;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.protectionlib.ProtectionLib;
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
 * Handles placement, breaking, and interactions for shaped blocks (stairs, slabs, doors, trapdoors, grates).
 *
 * Custom blocks use WAXED copper materials with custom textures.
 * Vanilla waxed copper is converted to NON-WAXED copper and marked to prevent oxidation,
 * preserving vanilla copper functionality while avoiding texture conflicts.
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
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
            convertWaxedCopperInChunk(chunk);
        }, 1L);
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
        Block targetBlock;

        // Determine target block - check if clicked block is replaceable
        if (BlockHelpers.isReplaceable(clickedBlock.getType())) {
            targetBlock = clickedBlock;
        } else {
            targetBlock = clickedBlock.getRelative(face);
        }

        // Handle slab double placement
        if (shapedMechanic.getBlockType() == ShapedBlockType.SLAB) {
            if (handleSlabPlacement(event, player, item, shapedMechanic, clickedBlock, face, targetBlock)) {
                return;
            }
        }

        // Check if target is replaceable (air, water, grass, etc.)
        if (!BlockHelpers.isReplaceable(targetBlock.getType())) {
            return;
        }

        // Check world height bounds
        Range<Integer> worldHeightRange = Range.of(targetBlock.getWorld().getMinHeight(),
                targetBlock.getWorld().getMaxHeight() - 1);
        if (!worldHeightRange.contains(targetBlock.getY())) {
            return;
        }

        // Check if player is standing inside target block
        if (BlockHelpers.isStandingInside(player, targetBlock)) {
            return;
        }

        // Check protection plugins (WorldGuard, GriefPrevention, etc.)
        if (!ProtectionLib.canBuild(player, targetBlock.getLocation())) {
            return;
        }

        // Check limited placing
        if (shapedMechanic.hasLimitedPlacing()) {
            if (!shapedMechanic.getLimitedPlacing().checkLimitedMechanic(clickedBlock)) {
                return;
            }
        }

        // Check if placing in water for waterlogging
        boolean isWaterlogged = targetBlock.getType() == Material.WATER;

        // Special handling for doors - they need 2 blocks
        if (shapedMechanic.getBlockType() == ShapedBlockType.DOOR) {
            Block upperBlock = targetBlock.getRelative(BlockFace.UP);
            // Check if there's room for the upper part
            if (!BlockHelpers.isReplaceable(upperBlock.getType())) {
                return;
            }
            // Check if player would be inside upper block
            if (BlockHelpers.isStandingInside(player, upperBlock)) {
                return;
            }
            // Check protection for upper block too
            if (!ProtectionLib.canBuild(player, upperBlock.getLocation())) {
                return;
            }
        }

        // Cancel the normal interaction
        event.setCancelled(true);

        // Place the block
        Material placeMaterial = shapedMechanic.getPlacedMaterial();
        targetBlock.setType(placeMaterial, false);

        // Apply block data based on player direction
        applyBlockData(targetBlock, player, face, shapedMechanic, isWaterlogged);

        // Mark as custom shaped block
        markAsCustomBlock(targetBlock, shapedMechanic);

        // For doors, place the upper block too
        if (shapedMechanic.getBlockType() == ShapedBlockType.DOOR) {
            Block upperBlock = targetBlock.getRelative(BlockFace.UP);
            upperBlock.setType(placeMaterial, false);
            applyDoorUpperData(upperBlock, player);
            markAsCustomBlock(upperBlock, shapedMechanic);
        }

        // Fire BlockPlaceEvent for compatibility
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(
            targetBlock, targetBlock.getState(), clickedBlock, item, player, true, EquipmentSlot.HAND
        );
        Bukkit.getPluginManager().callEvent(placeEvent);

        if (placeEvent.isCancelled()) {
            targetBlock.setType(Material.AIR);
            if (shapedMechanic.getBlockType() == ShapedBlockType.DOOR) {
                targetBlock.getRelative(BlockFace.UP).setType(Material.AIR);
            }
            return;
        }

        // Play sound
        if (shapedMechanic.hasBlockSounds()) {
            BlockSounds sounds = shapedMechanic.getBlockSounds();
            if (sounds.hasPlaceSound()) {
                BlockHelpers.playCustomBlockSound(targetBlock.getLocation(), sounds.getPlaceSound(), sounds.getPlaceVolume(), sounds.getPlacePitch());
            }
        } else {
            targetBlock.getWorld().playSound(targetBlock.getLocation(), Sound.BLOCK_COPPER_PLACE, 1.0f, 1.0f);
        }

        // Remove item from inventory
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }

        Logs.logSuccess("[ShapedBlock] Placed " + itemId + " as " + shapedMechanic.getBlockType());
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
                drop.spawns(block.getLocation(), player.getInventory().getItemInMainHand());
            }
        }

        // Play break sound
        if (mechanic.hasBlockSounds()) {
            BlockSounds sounds = mechanic.getBlockSounds();
            if (sounds.hasBreakSound()) {
                BlockHelpers.playCustomBlockSound(block.getLocation(), sounds.getBreakSound(), sounds.getBreakVolume(), sounds.getBreakPitch());
            }
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

    private void applyBlockData(Block block, Player player, BlockFace clickedFace, ShapedBlockMechanic mechanic, boolean isWaterlogged) {
        BlockData data = block.getBlockData();

        switch (mechanic.getBlockType()) {
            case STAIR -> applyStairData(data, player, clickedFace);
            case SLAB -> applySlabData(data, clickedFace);
            case DOOR -> applyDoorData(data, player);
            case TRAPDOOR -> applyTrapdoorData(data, player, clickedFace);
            case GRATE -> { /* Grates have no directional data */ }
        }

        if (isWaterlogged && data instanceof Waterlogged waterloggedData) {
            waterloggedData.setWaterlogged(true);
        }

        block.setBlockData(data, false);
    }

    private void applyStairData(BlockData data, Player player, BlockFace clickedFace) {
        if (data instanceof org.bukkit.block.data.type.Stairs stairs) {
            BlockFace playerFacing = player.getFacing();
            stairs.setFacing(playerFacing);

            if (clickedFace == BlockFace.UP) {
                stairs.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
            } else if (clickedFace == BlockFace.DOWN) {
                stairs.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
            } else {
                Location eyeLoc = player.getEyeLocation();
                double clickY = eyeLoc.getY() % 1;
                stairs.setHalf(clickY > 0.5 ?
                    org.bukkit.block.data.Bisected.Half.TOP :
                    org.bukkit.block.data.Bisected.Half.BOTTOM);
            }
        }
    }

    private void applySlabData(BlockData data, BlockFace clickedFace) {
        if (data instanceof Slab slab) {
            if (clickedFace == BlockFace.UP) {
                slab.setType(Slab.Type.BOTTOM);
            } else if (clickedFace == BlockFace.DOWN) {
                slab.setType(Slab.Type.TOP);
            } else {
                slab.setType(Slab.Type.BOTTOM);
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
            if (clickedFace == BlockFace.DOWN) {
                trapdoor.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
            } else {
                trapdoor.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
            }
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
        // Copy stair properties
        if (from instanceof org.bukkit.block.data.type.Stairs fromStairs && to instanceof org.bukkit.block.data.type.Stairs toStairs) {
            toStairs.setFacing(fromStairs.getFacing());
            toStairs.setHalf(fromStairs.getHalf());
            toStairs.setShape(fromStairs.getShape());
            if (from instanceof Waterlogged fromWL && to instanceof Waterlogged toWL) {
                toWL.setWaterlogged(fromWL.isWaterlogged());
            }
        }
        // Copy slab properties
        else if (from instanceof Slab fromSlab && to instanceof Slab toSlab) {
            toSlab.setType(fromSlab.getType());
            if (from instanceof Waterlogged fromWL && to instanceof Waterlogged toWL) {
                toWL.setWaterlogged(fromWL.isWaterlogged());
            }
        }
        // Copy door properties
        else if (from instanceof org.bukkit.block.data.type.Door fromDoor && to instanceof org.bukkit.block.data.type.Door toDoor) {
            toDoor.setFacing(fromDoor.getFacing());
            toDoor.setHalf(fromDoor.getHalf());
            toDoor.setHinge(fromDoor.getHinge());
            toDoor.setOpen(fromDoor.isOpen());
        }
        // Copy trapdoor properties
        else if (from instanceof org.bukkit.block.data.type.TrapDoor fromTrap && to instanceof org.bukkit.block.data.type.TrapDoor toTrap) {
            toTrap.setFacing(fromTrap.getFacing());
            toTrap.setHalf(fromTrap.getHalf());
            toTrap.setOpen(fromTrap.isOpen());
            if (from instanceof Waterlogged fromWL && to instanceof Waterlogged toWL) {
                toWL.setWaterlogged(fromWL.isWaterlogged());
            }
        }
        // Copy grate properties (waterlogged only)
        else if (from instanceof Waterlogged fromWL && to instanceof Waterlogged toWL) {
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
