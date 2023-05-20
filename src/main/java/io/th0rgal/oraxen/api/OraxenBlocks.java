package io.th0rgal.oraxen.api;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.compatibilities.provided.lightapi.WrappedLightAPI;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.storage.StorageMechanic;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic.SAPLING_KEY;
import static io.th0rgal.oraxen.utils.storage.StorageMechanic.STORAGE_KEY;

public class OraxenBlocks {

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
            default -> false;
        };
    }

    /**
     * Check if an itemID is an instance of an OraxenBlock
     *
     * @param itemId The ID to check
     * @return true if the itemID is an instance of an OraxenBlock, otherwise false
     */
    public static boolean isOraxenBlock(String itemId) {
        return OraxenItems.hasMechanic(itemId, "noteblock")
                || OraxenItems.hasMechanic(itemId, "stringblock");
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
        return !NoteBlockMechanicFactory.getInstance().isNotImplementedIn(itemID);
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
        return !StringBlockMechanicFactory.getInstance().isNotImplementedIn(itemID);
    }

    public static void place(String itemID, Location location) {

        if (isOraxenNoteBlock(itemID)) {
            placeNoteBlock(location, itemID);
        } else if (isOraxenStringBlock(itemID)) {
            placeStringBlock(location, itemID);
        }
    }

    /**
     * Get the BlockData assosiated with
     *
     * @param itemID The ItemID of the OraxenBlock
     * @return The BlockData assosiated with the ItemID, can be null
     */
    public static BlockData getOraxenBlockData(String itemID) {
        if (isOraxenNoteBlock(itemID)) {
            return NoteBlockMechanicFactory.getInstance().createNoteBlockData(itemID);
        } else if (isOraxenStringBlock(itemID)) {
            return StringBlockMechanicFactory.getInstance().createTripwireData(itemID);
        } else return null;
    }

    private static void placeNoteBlock(Location location, String itemID) {
        NoteBlockMechanicFactory.setBlockModel(location.getBlock(), itemID);
        Block block = location.getBlock();
        PersistentDataContainer pdc = BlockHelpers.getPDC(block);
        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (mechanic == null) return;

        if (mechanic.hasLight())
            WrappedLightAPI.createBlockLight(block.getLocation(), mechanic.getLight());

        if (mechanic.hasDryout() && mechanic.getDryout().isFarmBlock()) {
            pdc.set(FARMBLOCK_KEY, PersistentDataType.STRING, mechanic.getItemID());
        }

        if (mechanic.isStorage() && mechanic.getStorage().getStorageType() == StorageMechanic.StorageType.STORAGE) {
            pdc.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[]{});
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

        if (mechanic.getLight() != -1)
            WrappedLightAPI.createBlockLight(block.getLocation(), mechanic.getLight());
        if (mechanic.isSapling()) {
            SaplingMechanic sapling = mechanic.getSaplingMechanic();
            if (sapling != null && sapling.canGrowNaturally())
                BlockHelpers.getPDC(block).set(SAPLING_KEY, PersistentDataType.INTEGER, sapling.getNaturalGrowthTime());
        }
    }

    /**
     * Breaks an OraxenBlock at the given location
     *
     * @param location The location of the OraxenBlock
     * @return True if the block was broken, false if the block was not an OraxenBlock
     */
    public static boolean remove(Location location, @Nullable Player player) {
        Block block = location.getBlock();
        if (isOraxenNoteBlock(block)) {
            removeNoteBlock(block, player);
        } else if (isOraxenStringBlock(block)) {
            removeStringBlock(block, player);
        } else return false;
        return true;
    }

    private static void removeNoteBlock(Block block, @Nullable Player player) {
        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (mechanic == null) return;
        if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock())
            mechanic = mechanic.getDirectional().getParentMechanic();

        if (player != null) {
            OraxenNoteBlockBreakEvent noteBlockBreakEvent = new OraxenNoteBlockBreakEvent(mechanic, block, player);
            OraxenPlugin.get().getServer().getPluginManager().callEvent(noteBlockBreakEvent);
            if (noteBlockBreakEvent.isCancelled())
                return;
        }

        if (mechanic.hasLight())
            WrappedLightAPI.removeBlockLight(block.getLocation());
        if (player != null && player.getGameMode() != GameMode.CREATIVE)
            mechanic.getDrop().spawns(block.getLocation(), player.getInventory().getItemInMainHand());
        if (mechanic.isStorage() && mechanic.getStorage().getStorageType() == StorageMechanic.StorageType.STORAGE) {
            mechanic.getStorage().dropStorageContent(block);
        }
        block.setType(Material.AIR);
        checkNoteBlockAbove(block.getLocation());
    }

    private static void removeStringBlock(Block block, @Nullable Player player) {
        StringBlockMechanic mechanic = getStringMechanic(block);
        ItemStack item = player != null ? player.getInventory().getItemInMainHand() : new ItemStack(Material.AIR);
        if (mechanic == null) return;

        if (player != null) {
            OraxenStringBlockBreakEvent wireBlockBreakEvent = new OraxenStringBlockBreakEvent(mechanic, block, player);
            OraxenPlugin.get().getServer().getPluginManager().callEvent(wireBlockBreakEvent);
            if (wireBlockBreakEvent.isCancelled()) {
                return;
            }
        }

        if (mechanic.hasLight())
            WrappedLightAPI.removeBlockLight(block.getLocation());
        if (mechanic.isTall())
            block.getRelative(BlockFace.UP).setType(Material.AIR, false);
        if (player != null && player.getGameMode() != GameMode.CREATIVE)
            mechanic.getDrop().spawns(block.getLocation(), item);
        block.setType(Material.AIR, false);
        final Block blockAbove = block.getRelative(BlockFace.UP);
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
            StringBlockMechanicListener.fixClientsideUpdate(block.getLocation());
            if (blockAbove.getType() == Material.TRIPWIRE)
                removeStringBlock(blockAbove, player);
        }, 1L);
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
                    case MUSHROOM_STEM -> getBlockMechanic(location.getBlock());
                    default -> null;
                };
    }

    public static Mechanic getOraxenBlock(BlockData blockData) {
        return switch (blockData.getMaterial()) {
            case NOTE_BLOCK -> getNoteBlockMechanic(blockData);
            case TRIPWIRE -> getStringMechanic(blockData);
            default -> null;
        };
    }

    public static NoteBlockMechanic getNoteBlockMechanic(BlockData data) {
        if (!(data instanceof NoteBlock noteBlock)) return null;
        return NoteBlockMechanicFactory
                .getBlockMechanic((noteBlock.getInstrument().getType()) * 25
                        + noteBlock.getNote().getId() + (noteBlock.isPowered() ? 400 : 0) - 26);
    }

    public static NoteBlockMechanic getNoteBlockMechanic(Block block) {
        if (block.getType() != Material.NOTE_BLOCK) return null;
        final NoteBlock noteblock = (NoteBlock) block.getBlockData();
        return NoteBlockMechanicFactory
                .getBlockMechanic((noteblock.getInstrument().getType()) * 25
                        + noteblock.getNote().getId() + (noteblock.isPowered() ? 400 : 0) - 26);
    }

    public static NoteBlockMechanic getNoteBlockMechanic(String itemID) {
        Mechanic mechanic = NoteBlockMechanicFactory.getInstance().getMechanic(itemID);
        return mechanic instanceof NoteBlockMechanic noteBlockMechanic ? noteBlockMechanic : null;
    }

    public static StringBlockMechanic getStringMechanic(BlockData blockData) {
        if (!(blockData instanceof Tripwire tripwire)) return null;
        return StringBlockMechanicFactory.getBlockMechanic(StringBlockMechanicFactory.getCode(tripwire));
    }

    public static StringBlockMechanic getStringMechanic(Block block) {
        if (block.getType() == Material.TRIPWIRE) {
            final Tripwire tripwire = (Tripwire) block.getBlockData();
            return StringBlockMechanicFactory.getBlockMechanic(StringBlockMechanicFactory.getCode(tripwire));
        } else return null;
    }

    public static StringBlockMechanic getStringMechanic(String itemID) {
        Mechanic mechanic = StringBlockMechanicFactory.getInstance().getMechanic(itemID);
        return mechanic instanceof StringBlockMechanic stringMechanic ? stringMechanic : null;
    }

    public static BlockMechanic getBlockMechanic(Block block) {
        if (block.getType() == Material.MUSHROOM_STEM) {
            return BlockMechanicFactory.getBlockMechanic(BlockMechanic.getCode(block));
        } else return null;
    }
}
