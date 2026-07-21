package io.th0rgal.oraxen.api;

import com.jeff_media.morepersistentdatatypes.DataType;
import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockBreakEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.shapedblock.OraxenShapedBlockBreakEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockBreaking;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockDurability;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanicListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockType;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.togglelight.ToggleLightMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.togglelight.ToggleLightMechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.Effect;
import org.bukkit.GameEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import io.th0rgal.oraxen.utils.drops.Loot;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic.SAPLING_KEY;

public class OraxenBlocks {

    /**
     * Get all OraxenItem ID's that have either a NoteBlockMechanic or a StringBlockMechanic
     *
     * @return A set of all OraxenItem ID's that have either a NoteBlockMechanic or a StringBlockMechanic
     */
    public static Set<String> getBlockIDs() {
        return Arrays.stream(OraxenItems.getItemNames()).filter(OraxenBlocks::isOraxenBlock).collect(Collectors.toSet());
    }

    /**
     * Get all OraxenItem ID's that have a NoteBlockMechanic
     *
     * @return A set of all OraxenItem ID's that have a NoteBlockMechanic
     */
    public static Set<String> getNoteBlockIDs() {
        return Arrays.stream(OraxenItems.getItemNames()).filter(OraxenBlocks::isOraxenNoteBlock).collect(Collectors.toSet());
    }

    /**
     * Get all OraxenItem ID's that have a StringBlockMechanic
     *
     * @return A set of all OraxenItem ID's that have a StringBlockMechanic
     */
    public static Set<String> getStringBlockIDs() {
        return Arrays.stream(OraxenItems.getItemNames()).filter(OraxenBlocks::isOraxenStringBlock).collect(Collectors.toSet());
    }

    /**
     * Get all OraxenItem ID's that have a ChorusBlockMechanic
     *
     * @return A set of all OraxenItem ID's that have a ChorusBlockMechanic
     */
    public static Set<String> getChorusBlockIDs() {
        return Arrays.stream(OraxenItems.getItemNames()).filter(OraxenBlocks::isOraxenChorusBlock).collect(Collectors.toSet());
    }

    /**
     * Check if a block is an instance of an OraxenBlock
     *
     * @param block The block to check
     * @return true if the block is an instance of an OraxenBlock, otherwise false
     */
    public static boolean isOraxenBlock(Block block) {
        if (block == null) return false;
        return switch (block.getType()) {
            case NOTE_BLOCK -> getNoteBlockMechanic(block) != null;
            case TRIPWIRE -> getStringMechanic(block) != null;
            case CHORUS_PLANT -> getChorusMechanic(block) != null;
            default -> getShapedMechanic(block) != null;
        };
    }

    /**
     * Check if an itemID is an instance of an OraxenBlock
     *
     * @param itemId The ID to check
     * @return true if the itemID is an instance of an OraxenBlock, otherwise false
     */
    public static boolean isOraxenBlock(String itemId) {
        return OraxenItems.hasMechanic(itemId, "block")
                || isOraxenNoteBlock(itemId)
                || isOraxenStringBlock(itemId)
                || isOraxenChorusBlock(itemId);
    }

    /**
     * Check if a block is an instance of a NoteBlock
     *
     * @param block The block to check
     * @return true if the block is an instance of an NoteBlock, otherwise false
     */
    public static boolean isOraxenNoteBlock(Block block) {
        return block.getType() == Material.NOTE_BLOCK && getNoteBlockMechanic(block) != null;
    }

    /**
     * Check if an itemID has a NoteBlockMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a NoteBlockMechanic, otherwise false
     */
    public static boolean isOraxenNoteBlock(String itemID) {
        return NoteBlockMechanicFactory.isEnabled() && !NoteBlockMechanicFactory.getInstance().isNotImplementedIn(itemID);
    }

    public static boolean isOraxenNoteBlock(ItemStack item) {
        return isOraxenNoteBlock(OraxenItems.getIdByItem(item));
    }

    /**
     * Check if a block is an instance of a StringBlock
     *
     * @param block The block to check
     * @return true if the block is an instance of a StringBlock, otherwise false
     */
    public static boolean isOraxenStringBlock(Block block) {
        return block.getType() == Material.TRIPWIRE && getStringMechanic(block) != null;
    }

    /**
     * Check if an itemID has a StringBlockMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a StringBlockMechanic, otherwise false
     */
    public static boolean isOraxenStringBlock(String itemID) {
        return StringBlockMechanicFactory.isEnabled() && !StringBlockMechanicFactory.getInstance().isNotImplementedIn(itemID);
    }

    /**
     * Check if a block is an instance of a ChorusBlock
     *
     * @param block The block to check
     * @return true if the block is an instance of a ChorusBlock, otherwise false
     */
    public static boolean isOraxenChorusBlock(Block block) {
        return block.getType() == Material.CHORUS_PLANT && getChorusMechanic(block) != null;
    }

    /**
     * Check if an itemID has a ChorusBlockMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a ChorusBlockMechanic, otherwise false
     */
    public static boolean isOraxenChorusBlock(String itemID) {
        return ChorusBlockMechanicFactory.isEnabled() && !ChorusBlockMechanicFactory.getInstance().isNotImplementedIn(itemID);
    }

    public static void place(String itemID, Location location) {

        if (isOraxenNoteBlock(itemID)) {
            placeNoteBlock(location, itemID);
        } else if (isOraxenStringBlock(itemID)) {
            placeStringBlock(location, itemID);
        } else if (isOraxenChorusBlock(itemID)) {
            placeChorusBlock(location, itemID);
        } else if (getShapedMechanic(itemID) != null) {
            placeShapedBlock(location, itemID);
        }
    }

    /**
     * Get the BlockData assosiated with
     *
     * @param itemID The ItemID of the OraxenBlock
     * @return The BlockData assosiated with the ItemID, can be null
     */
    @Nullable
    public static BlockData getOraxenBlockData(String itemID) {
        if (isOraxenNoteBlock(itemID)) {
            return NoteBlockMechanicFactory.getInstance().createNoteBlockData(itemID);
        } else if (isOraxenStringBlock(itemID)) {
            return StringBlockMechanicFactory.getInstance().createTripwireData(itemID);
        } else if (isOraxenChorusBlock(itemID)) {
            return ChorusBlockMechanicFactory.getInstance().createChorusData(itemID);
        } else {
            ShapedBlockMechanic shapedMechanic = getShapedMechanic(itemID);
            if (shapedMechanic != null) return shapedMechanic.getPlacedMaterial().createBlockData();
        }
        return null;
    }

    private static void placeNoteBlock(Location location, String itemID) {
        NoteBlockMechanicFactory.setBlockModel(location.getBlock(), itemID);
        Block block = location.getBlock();
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (mechanic == null) return;

        createInitialLight(block, mechanic.getItemID());

        if (mechanic.hasDryout() && mechanic.getDryout().isFarmBlock()) {
            pdc.set(FARMBLOCK_KEY, PersistentDataType.STRING, mechanic.getItemID());
        }

        if (mechanic.isStorage() && mechanic.getStorage().getStorageType() == StorageMechanic.StorageType.STORAGE) {
            pdc.set(StorageMechanic.STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
        }
        checkNoteBlockAbove(location);
    }

    private static void checkNoteBlockAbove(final Location loc) {
        final Block block = loc.getBlock().getRelative(BlockFace.UP);
        if (block.getType() == Material.NOTE_BLOCK)
            block.getState().update(true, true);
        final Block nextBlock = loc.getBlock().getRelative(BlockFace.UP, 2);
        if (nextBlock.getType() == Material.NOTE_BLOCK)
            checkNoteBlockAbove(block.getLocation());
    }

    private static void placeStringBlock(Location location, String itemID) {
        Block block = location.getBlock();
        Block blockAbove = block.getRelative(BlockFace.UP);
        StringBlockMechanicFactory.setBlockModel(block, itemID);
        StringBlockMechanic mechanic = getStringMechanic(location.getBlock());
        if (mechanic == null) return;
        if (mechanic.isTall()) {
            if (!BlockHelpers.REPLACEABLE_BLOCKS.contains(blockAbove.getType())) return;
            else blockAbove.setType(Material.TRIPWIRE);
        }

        createInitialLight(block, mechanic.getItemID());
        if (mechanic.isSapling()) {
            SaplingMechanic sapling = mechanic.getSaplingMechanic();
            if (sapling != null && sapling.canGrowNaturally())
                BlockHelpers.getPDC(block).set(SAPLING_KEY, PersistentDataType.INTEGER, sapling.getNaturalGrowthTime());
        }
    }

    private static void placeChorusBlock(Location location, String itemID) {
        Block block = location.getBlock();
        ChorusBlockMechanicFactory.setBlockModel(block, itemID);
        ChorusBlockMechanic mechanic = getChorusMechanic(block);
        if (mechanic == null) return;

        createInitialLight(block, mechanic.getItemID());
    }

    private static void placeShapedBlock(Location location, String itemID) {
        ShapedBlockMechanic mechanic = getShapedMechanic(itemID);
        if (mechanic == null) return;

        Block block = location.getBlock();
        Block upperBlock = block.getRelative(BlockFace.UP);
        if (mechanic.getBlockType() == ShapedBlockType.DOOR
                && !BlockHelpers.REPLACEABLE_BLOCKS.contains(upperBlock.getType())) return;

        block.setType(mechanic.getPlacedMaterial(), false);
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Door door) {
            door.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
            block.setBlockData(door, false);
        }
        ShapedBlockMechanic.setItemId(new CustomBlockData(block, OraxenPlugin.get()), mechanic.getItemID());
        createInitialLight(block, mechanic.getItemID());

        if (mechanic.getBlockType() != ShapedBlockType.DOOR) return;

        upperBlock.setType(mechanic.getPlacedMaterial(), false);
        if (upperBlock.getBlockData() instanceof org.bukkit.block.data.type.Door door) {
            door.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
            upperBlock.setBlockData(door, false);
        }
        ShapedBlockMechanic.setItemId(new CustomBlockData(upperBlock, OraxenPlugin.get()), mechanic.getItemID());
        createInitialLight(upperBlock, mechanic.getItemID());
    }

    /**
     * Breaks an OraxenBlock at the given location
     *
     * @param location The location of the OraxenBlock
     * @param player   The player that broke the block, can be null
     * @return True if the block was broken, false if the block was not an OraxenBlock or could not be broken
     */
    public static boolean remove(Location location, @Nullable Player player) {
        return remove(location, player, null);
    }

    /**
     * Breaks an OraxenBlock at the given location
     *
     * @param location  The location of the OraxenBlock
     * @param player    The player that broke the block, can be null
     * @param forceDrop Whether to force the block to drop, even if player is null or in creative mode
     * @return True if the block was broken, false if the block was not an OraxenBlock or could not be broken
     */
    public static boolean remove(Location location, @Nullable Player player, boolean forceDrop) {
        Block block = location.getBlock();

        NoteBlockMechanic noteMechanic = getNoteBlockMechanic(block);
        StringBlockMechanic stringMechanic = getStringMechanic(block);
        ChorusBlockMechanic chorusMechanic = getChorusMechanic(block);
        ShapedBlockMechanic shapedMechanic = getShapedMechanic(block);
        ItemStack tool = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        Drop overrideDrop = !forceDrop ? null
                : noteMechanic != null ? noteMechanic.getDrop(tool)
                : stringMechanic != null ? stringMechanic.getDrop(tool)
                : chorusMechanic != null ? chorusMechanic.getDrop(tool)
                : shapedMechanic != null ? shapedMechanic.getDrop(tool)
                : null;
        return remove(location, player, overrideDrop);
    }

    /**
     * Breaks an OraxenBlock at the given location
     *
     * @param location     The location of the OraxenBlock
     * @param player       The player that broke the block, can be null
     * @param overrideDrop Drop to override the default drop, can be null
     * @return True if the block was broken, false if the block was not an OraxenBlock or could not be broken
     */
    public static boolean remove(Location location, @Nullable Player player, @Nullable Drop overrideDrop) {
        Block block = location.getBlock();

        if (isOraxenNoteBlock(block)) return removeNoteBlock(block, player, overrideDrop);
        if (isOraxenStringBlock(block)) return removeStringBlock(block, player, overrideDrop);
        if (isOraxenChorusBlock(block)) return removeChorusBlock(block, player, overrideDrop);
        if (getShapedMechanic(block) != null) return removeShapedBlock(block, player, overrideDrop);
        return false;
    }

    /**
     * Shared block removal logic. Fires the appropriate break event, handles drops,
     * light removal, storage cleanup, and sets the block to AIR.
     */
    private static <M extends Mechanic, E extends org.bukkit.event.Event & org.bukkit.event.Cancellable> boolean removeCustomBlock(
            Block block, @Nullable Player player, @Nullable Drop overrideDrop,
            M mechanic, String itemID, Drop defaultDrop,
            BiFunction<M, Player, E> breakEvent, Function<E, Drop> getEventDrop, BiConsumer<E, Drop> setEventDrop,
            @Nullable Runnable preRemove, @Nullable Runnable postRemove) {

        ItemStack itemInHand = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        Drop drop = overrideDrop != null ? overrideDrop : defaultDrop;

        if (player != null) {
            E event = breakEvent.apply(mechanic, player);
            setEventDrop.accept(event, drop);
            if (!EventUtils.callEvent(event)) return false;
            drop = resolveDropAfterEvent(player, getEventDrop.apply(event), overrideDrop != null);
            sendBreakEffects(block, player);
            BlockDurability.applyConfigured(player, itemInHand, getDurabilityAction(mechanic, itemInHand));
        }

        if (drop != null) drop.spawns(block.getLocation(), itemInHand);
        removeLight(block, itemID);
        dropStorageIfPresent(mechanic, block);
        if (preRemove != null) preRemove.run();
        block.setType(Material.AIR);
        if (postRemove != null) postRemove.run();
        return true;
    }

    @Nullable
    static Drop resolveDropAfterEvent(Player player, @Nullable Drop eventDrop, boolean forcedDrop) {
        if (!forcedDrop && player.getGameMode() == GameMode.CREATIVE) return null;
        return eventDrop;
    }

    @Nullable
    private static BlockBreaking.DurabilityAction getDurabilityAction(Mechanic mechanic, ItemStack tool) {
        if (mechanic instanceof NoteBlockMechanic noteBlockMechanic) return noteBlockMechanic.getDurabilityAction(tool);
        if (mechanic instanceof StringBlockMechanic stringBlockMechanic) return stringBlockMechanic.getDurabilityAction(tool);
        if (mechanic instanceof ChorusBlockMechanic chorusBlockMechanic) return chorusBlockMechanic.getDurabilityAction(tool);
        if (mechanic instanceof ShapedBlockMechanic shapedBlockMechanic) return shapedBlockMechanic.getDurabilityAction(tool);
        return null;
    }

    private static void sendBreakEffects(Block block, Player player) {
        if (VersionUtil.isPaperServer())
            block.getWorld().sendGameEvent(player, GameEvent.BLOCK_DESTROY, block.getLocation().toVector());
        if (block.getType() == Material.NOTE_BLOCK && VersionUtil.atOrAbove("1.20"))
            block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getBlockData());
    }

    private static void dropStorageIfPresent(Mechanic mechanic, Block block) {
        StorageMechanic storage = getStorageMechanic(mechanic);
        if (storage != null && storage.getStorageType() == StorageMechanic.StorageType.STORAGE)
            storage.dropStorageContent(block);
    }

    @Nullable
    private static StorageMechanic getStorageMechanic(Mechanic mechanic) {
        if (mechanic instanceof NoteBlockMechanic nbm && nbm.isStorage()) return nbm.getStorage();
        if (mechanic instanceof StringBlockMechanic sbm && sbm.isStorage()) return sbm.getStorage();
        if (mechanic instanceof ChorusBlockMechanic cbm && cbm.isStorage()) return cbm.getStorage();
        return null;
    }

    private static boolean removeNoteBlock(Block block, @Nullable Player player, Drop overrideDrop) {
        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (mechanic == null) return false;
        if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock())
            mechanic = mechanic.getDirectional().getParentMechanic();

        Location loc = block.getLocation();
        ItemStack tool = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        return removeCustomBlock(block, player, overrideDrop,
                mechanic, mechanic.getItemID(), mechanic.getDrop(tool),
                (m, p) -> new OraxenNoteBlockBreakEvent(m, block, p),
                OraxenNoteBlockBreakEvent::getDrop,
                OraxenNoteBlockBreakEvent::setDrop,
                null,
                () -> checkNoteBlockAbove(loc));
    }

    private static boolean removeStringBlock(Block block, @Nullable Player player, @Nullable Drop overrideDrop) {
        StringBlockMechanic mechanic = getStringMechanic(block);
        if (mechanic == null) return false;

        // For stackable blocks, multiply drops by the stack level
        ItemStack tool = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        Drop effectiveDrop = mechanic.getDrop(tool);
        if (mechanic.isStackable() && overrideDrop == null && block.getBlockData() instanceof org.bukkit.block.data.type.Tripwire tripwire) {
            int variation = StringBlockMechanicFactory.getCode(tripwire);
            int multiplier = mechanic.getStackDropMultiplier(variation);
            if (multiplier > 1) {
                List<Loot> multipliedLoots = new ArrayList<>();
                for (Loot loot : effectiveDrop.getLoots()) {
                    for (int i = 0; i < multiplier; i++) {
                        multipliedLoots.add(loot);
                    }
                }
                effectiveDrop = Drop.clone(effectiveDrop, multipliedLoots);
            }
        }

        final Block blockAbove = block.getRelative(BlockFace.UP);
        final Drop dropForEvent = effectiveDrop;
        return removeCustomBlock(block, player, overrideDrop,
                mechanic, mechanic.getItemID(), dropForEvent,
                (m, p) -> new OraxenStringBlockBreakEvent(m, block, p),
                OraxenStringBlockBreakEvent::getDrop,
                OraxenStringBlockBreakEvent::setDrop,
                () -> {
                    if (mechanic.isTall()) blockAbove.setType(Material.AIR);
                },
                () -> {
                    SchedulerUtil.runAtLocationLater(block.getLocation(), 1L, () -> {
                        StringBlockMechanicListener.fixClientsideUpdate(block.getLocation());
                        if (blockAbove.getType() == Material.TRIPWIRE)
                            removeStringBlock(blockAbove, player, overrideDrop);
                    });
                });
    }

    private static boolean removeChorusBlock(Block block, @Nullable Player player, @Nullable Drop overrideDrop) {
        ChorusBlockMechanic mechanic = getChorusMechanic(block);
        if (mechanic == null) return false;

        ItemStack tool = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        return removeCustomBlock(block, player, overrideDrop,
                mechanic, mechanic.getItemID(), mechanic.getDrop(tool),
                (m, p) -> new OraxenChorusBlockBreakEvent(m, block, p),
                OraxenChorusBlockBreakEvent::getDrop,
                OraxenChorusBlockBreakEvent::setDrop,
                null,
                () -> SchedulerUtil.runAtLocationLater(block.getLocation(), 1L,
                        () -> ChorusBlockMechanicListener.fixClientsideUpdate(block.getLocation())));
    }

    private static boolean removeShapedBlock(Block block, @Nullable Player player, @Nullable Drop overrideDrop) {
        ShapedBlockMechanic mechanic = getShapedMechanic(block);
        if (mechanic == null) return false;

        ItemStack itemInHand = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        Drop drop = overrideDrop != null ? overrideDrop : mechanic.getDrop(itemInHand);

        if (player != null) {
            OraxenShapedBlockBreakEvent event = new OraxenShapedBlockBreakEvent(mechanic, block, player);
            event.setDrop(drop);
            if (!EventUtils.callEvent(event)) return false;
            drop = resolveDropAfterEvent(player, event.getDrop(), overrideDrop != null);
            sendBreakEffects(block, player);
            BlockDurability.applyConfigured(player, itemInHand, mechanic.getDurabilityAction(itemInHand));
        }
        if (drop != null) {
            int dropCount = getShapedDropCount(block);
            for (int i = 0; i < dropCount; i++) {
                drop.spawns(block.getLocation(), itemInHand);
            }
        }

        removeLight(block, mechanic.getItemID());
        clearShapedBlockData(block);
        clearShapedDoorHalf(block, mechanic);
        block.setType(Material.AIR);
        return true;
    }

    private static int getShapedDropCount(Block block) {
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Slab slab
                && slab.getType() == org.bukkit.block.data.type.Slab.Type.DOUBLE) {
            return 2;
        }
        return 1;
    }

    private static void clearShapedDoorHalf(Block block, ShapedBlockMechanic mechanic) {
        if (mechanic.getBlockType() != io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockType.DOOR) return;
        if (!(block.getBlockData() instanceof org.bukkit.block.data.type.Door door)) return;

        Block otherHalf = door.getHalf() == org.bukkit.block.data.Bisected.Half.BOTTOM
                ? block.getRelative(BlockFace.UP)
                : block.getRelative(BlockFace.DOWN);
        if (!(otherHalf.getBlockData() instanceof org.bukkit.block.data.type.Door otherDoor)) return;
        if (otherDoor.getHalf() == door.getHalf()) return;

        ShapedBlockMechanic otherMechanic = getShapedMechanic(otherHalf);
        if (otherMechanic == null || !otherMechanic.getItemID().equals(mechanic.getItemID())) return;

        removeLight(otherHalf, mechanic.getItemID());
        clearShapedBlockData(otherHalf);
        otherHalf.setType(Material.AIR);
    }

    private static void clearShapedBlockData(Block block) {
        ShapedBlockMechanic.removeItemId(new CustomBlockData(block, OraxenPlugin.get()));
    }

    /**
     * Get the OraxenBlock at a location
     *
     * @param location The location to check
     * @return The Mechanic of the OraxenBlock at the location, or null if there is no OraxenBlock at the location.
     * Keep in mind that this method returns the base Mechanic, not the type. Therefore, you will need to cast this to the type you need
     */
    public static Mechanic getOraxenBlock(Location location) {
        return !isOraxenBlock(location.getBlock()) ? null :
                switch (location.getBlock().getType()) {
                    case NOTE_BLOCK -> getNoteBlockMechanic(location.getBlock());
                    case TRIPWIRE -> getStringMechanic(location.getBlock());
                    case CHORUS_PLANT -> getChorusMechanic(location.getBlock());
                    default -> getShapedMechanic(location.getBlock());
                };
    }

    public static Mechanic getOraxenBlock(BlockData blockData) {
        return switch (blockData.getMaterial()) {
            case NOTE_BLOCK -> getNoteBlockMechanic(blockData);
            case TRIPWIRE -> getStringMechanic(blockData);
            case CHORUS_PLANT -> getChorusMechanic(blockData);
            default -> null;
        };
    }

    public static NoteBlockMechanic getNoteBlockMechanic(BlockData data) {
        if (!NoteBlockMechanicFactory.isEnabled()) return null;
        if (!(data instanceof NoteBlock noteBlock)) return null;
        return NoteBlockMechanicFactory
                .getBlockMechanic((noteBlock.getInstrument().getType()) * 25
                        + noteBlock.getNote().getId() + (noteBlock.isPowered() ? 400 : 0) - 26);
    }

    public static NoteBlockMechanic getNoteBlockMechanic(Block block) {
        if (!NoteBlockMechanicFactory.isEnabled()) return null;
        if (block.getType() != Material.NOTE_BLOCK) return null;
        final NoteBlock noteblock = (NoteBlock) block.getBlockData();
        return NoteBlockMechanicFactory
                .getBlockMechanic((noteblock.getInstrument().getType()) * 25
                        + noteblock.getNote().getId() + (noteblock.isPowered() ? 400 : 0) - 26);
    }

    @org.jetbrains.annotations.Nullable
    public static NoteBlockMechanic getNoteBlockMechanic(String itemID) {
        if (!NoteBlockMechanicFactory.isEnabled()) return null;
        Mechanic mechanic = NoteBlockMechanicFactory.getInstance().getMechanic(itemID);
        return mechanic instanceof NoteBlockMechanic noteBlockMechanic ? noteBlockMechanic : null;
    }

    @org.jetbrains.annotations.Nullable
    public static StringBlockMechanic getStringMechanic(BlockData blockData) {
        if (!StringBlockMechanicFactory.isEnabled()) return null;
        if (!(blockData instanceof Tripwire tripwire)) return null;
        return StringBlockMechanicFactory.getBlockMechanic(StringBlockMechanicFactory.getCode(tripwire));
    }

    @org.jetbrains.annotations.Nullable
    public static StringBlockMechanic getStringMechanic(Block block) {
        if (!StringBlockMechanicFactory.isEnabled()) return null;
        if (block.getType() == Material.TRIPWIRE) {
            final Tripwire tripwire = (Tripwire) block.getBlockData();
            return StringBlockMechanicFactory.getBlockMechanic(StringBlockMechanicFactory.getCode(tripwire));
        } else return null;
    }

    @org.jetbrains.annotations.Nullable
    public static StringBlockMechanic getStringMechanic(String itemID) {
        if (!StringBlockMechanicFactory.isEnabled()) return null;
        return StringBlockMechanicFactory.getInstance().getMechanic(itemID);
    }

    @org.jetbrains.annotations.Nullable
    public static ChorusBlockMechanic getChorusMechanic(BlockData blockData) {
        if (!ChorusBlockMechanicFactory.isEnabled()) return null;
        if (!(blockData instanceof MultipleFacing multipleFacing)) return null;
        if (blockData.getMaterial() != Material.CHORUS_PLANT) return null;
        return ChorusBlockMechanicFactory.getBlockMechanic(ChorusBlockMechanicFactory.getCode(multipleFacing));
    }

    @org.jetbrains.annotations.Nullable
    public static ChorusBlockMechanic getChorusMechanic(Block block) {
        if (!ChorusBlockMechanicFactory.isEnabled()) return null;
        if (block.getType() == Material.CHORUS_PLANT) {
            final MultipleFacing multipleFacing = (MultipleFacing) block.getBlockData();
            return ChorusBlockMechanicFactory.getBlockMechanic(ChorusBlockMechanicFactory.getCode(multipleFacing));
        } else return null;
    }

    @org.jetbrains.annotations.Nullable
    public static ChorusBlockMechanic getChorusMechanic(String itemID) {
        if (!ChorusBlockMechanicFactory.isEnabled()) return null;
        return ChorusBlockMechanicFactory.getInstance().getMechanic(itemID);
    }

    @org.jetbrains.annotations.Nullable
    public static ShapedBlockMechanic getShapedMechanic(Block block) {
        if (!ShapedBlockMechanicFactory.isEnabled() || ShapedBlockType.fromMaterial(block.getType()) == null) return null;
        BlockMechanicFactory blockFactory = BlockMechanicFactory.getInstance();
        if (blockFactory == null) return null;
        ShapedBlockMechanicFactory shapedFactory = blockFactory.getShapedBlockFactory();
        return shapedFactory != null ? shapedFactory.getMechanicFromBlock(block) : null;
    }

    @org.jetbrains.annotations.Nullable
    public static ShapedBlockMechanic getShapedMechanic(String itemID) {
        if (!ShapedBlockMechanicFactory.isEnabled()) return null;
        BlockMechanicFactory blockFactory = BlockMechanicFactory.getInstance();
        if (blockFactory == null) return null;
        ShapedBlockMechanicFactory shapedFactory = blockFactory.getShapedBlockFactory();
        if (shapedFactory == null) return null;
        Mechanic mechanic = shapedFactory.getMechanic(itemID);
        return mechanic instanceof ShapedBlockMechanic shapedBlockMechanic ? shapedBlockMechanic : null;
    }

    private static void createInitialLight(Block block, String itemID) {
        ToggleLightMechanic toggleLight = getToggleLightMechanic(itemID);
        if (toggleLight != null && (toggleLight.hasToggleLight() || toggleLight.getBaseLightLevel() > 0)) {
            toggleLight.updateLight(block, toggleLight.getBaseLightLevel());
            return;
        }
        LightMechanic light = getLightMechanic(itemID);
        if (light != null) light.createBlockLight(block);
    }

    private static void removeLight(Block block, String itemID) {
        ToggleLightMechanic toggleLight = getToggleLightMechanic(itemID);
        if (toggleLight != null && (toggleLight.hasToggleLight() || toggleLight.getBaseLightLevel() > 0)) {
            toggleLight.updateLight(block, 0);
            return;
        }
        LightMechanic light = getLightMechanic(itemID);
        if (light != null) light.removeBlockLight(block);
    }

    @Nullable
    private static LightMechanic getLightMechanic(String itemID) {
        NoteBlockMechanic nbm = getNoteBlockMechanic(itemID);
        if (nbm != null && nbm.hasLight()) return nbm.getLight();
        StringBlockMechanic sbm = getStringMechanic(itemID);
        if (sbm != null && sbm.hasLight()) return sbm.getLight();
        ChorusBlockMechanic cbm = getChorusMechanic(itemID);
        if (cbm != null && cbm.hasLight()) return cbm.getLight();
        ShapedBlockMechanic shaped = getShapedMechanic(itemID);
        if (shaped != null && shaped.hasLight()) return shaped.getLight();
        return null;
    }

    @Nullable
    private static ToggleLightMechanic getToggleLightMechanic(String itemID) {
        ToggleLightMechanicFactory factory = ToggleLightMechanicFactory.getInstance();
        if (factory == null) {
            MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("toggle_light");
            if (mechanicFactory instanceof ToggleLightMechanicFactory) {
                factory = (ToggleLightMechanicFactory) mechanicFactory;
            } else {
                return null;
            }
        }
        return factory != null ? factory.getMechanic(itemID) : null;
    }
}
