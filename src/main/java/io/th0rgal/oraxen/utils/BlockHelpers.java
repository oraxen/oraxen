package io.th0rgal.oraxen.utils;

import com.google.common.collect.Sets;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockHelpers {

    /**
     * Returns the block the entity is standing on.<br>
     * Mainly to handle cases where player is on the edge of a block, with AIR below them
     */
    @Nullable
    public static Block getBlockStandingOn(Entity entity) {
        Block block = entity.getLocation().getBlock();
        Block blockBelow = block.getRelative(BlockFace.DOWN);
        if (!block.getType().isAir() && block.getType() != Material.LIGHT) return block;
        if (!blockBelow.getType().isAir()) return blockBelow;

        // Expand players hitbox by 0.3, which is the maximum size a player can be off a block
        // Whilst not falling off
        BoundingBox entityBox = entity.getBoundingBox().expand(0.3);
        for (BlockFace face : BlockFace.values()) {
            if (!face.isCartesian() || face.getModY() != 0) continue;
            Block relative = blockBelow.getRelative(face);
            if (relative.getType() == Material.AIR) continue;
            if (relative.getBoundingBox().overlaps(entityBox)) return relative;
        }

        return null;
    }

    public static void playCustomBlockSound(Location location, String sound, float volume, float pitch) {
        playCustomBlockSound(location.toCenterLocation(), sound, SoundCategory.BLOCKS, volume, pitch);
    }

    public static void playCustomBlockSound(Location location, String sound, SoundCategory category, float volume, float pitch) {
        if (sound == null || location == null || location.getWorld() == null || category == null) return;
        AdventureUtils.playSound(location, validateReplacedSounds(sound), AdventureUtils.toSource(category), volume, pitch);
    }

    public static String validateReplacedSounds(String sound) {
        ConfigurationSection mechanics = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds");
        if (sound == null || mechanics == null) return sound;

        sound = sound.replace("minecraft:", "");
        if (sound.startsWith("block.wood") && BlockSounds.isBlockSoundEnabled(mechanics)) {
            return sound.replace("block.wood", "required.wood");
        } else if (sound.startsWith("block.stone") && (BlockSounds.isBlockSoundEnabled(mechanics) ||
                BlockSounds.isFurnitureSoundEnabled(mechanics))) {
            return sound.replace("block.stone", "required.stone");
        } else return sound;
    }

    /** Centered on the block horizontally, but at the bottom of the block vertically. */
    public static Location toCenterBlockLocation(Location location) {
        return location.toCenterLocation().subtract(0, 0.5, 0);
    }

    public static boolean isStandingInside(final Player player, final Block block) {
        if (player == null || block == null) return false;
        // Since the block might be AIR, Block#getBoundingBox returns an empty one
        // Get the block-center and expand it 0.5 to cover the block
        BoundingBox blockBox = BoundingBox.of(block.getLocation().toCenterLocation(), 0.5, 0.5, 0.5);

        return !block.getWorld().getNearbyEntities(blockBox).stream()
                .filter(e -> e instanceof LivingEntity && (!(e instanceof Player p) || p.getGameMode() != GameMode.SPECTATOR))
                .toList().isEmpty();
    }

    /**
     * Regex matching the per-block keys used to store block data inside the chunk's PersistentDataContainer.
     * The encoding (relative x/z within the chunk, absolute y) matches the scheme previously used by the
     * shaded CustomBlockData library, so existing worlds keep working without migration.
     */
    private static final Pattern BLOCK_KEY_REGEX = Pattern.compile("^x(\\d+)y(-?\\d+)z(\\d+)$");

    /** Returns the block-data PersistentDataContainer stored in the block's chunk
     * @param block The block to get the PersistentDataContainer for
     * */
    public static PersistentDataContainer getPDC(Block block) {
        return getPDC(block, OraxenPlugin.get());
    }

    /** Returns the block-data PersistentDataContainer stored in the block's chunk
     * @param block The block to get the PersistentDataContainer for
     * @param plugin The plugin the block data belongs to
     * */
    public static PersistentDataContainer getPDC(Block block, JavaPlugin plugin) {
        return new BlockPersistentDataContainer(block, plugin);
    }

    /** Removes all block data stored for the given block in its chunk's PersistentDataContainer
     * @param block The block to remove the PersistentDataContainer for
     * */
    public static void removePDC(Block block) {
        removePDC(block, OraxenPlugin.get());
    }

    /** Removes all block data stored for the given block in its chunk's PersistentDataContainer
     * @param block The block to remove the PersistentDataContainer for
     * @param plugin The plugin the block data belongs to
     * */
    public static void removePDC(Block block, JavaPlugin plugin) {
        block.getChunk().getPersistentDataContainer().remove(blockKey(plugin, block));
    }

    /** Returns all blocks in the given chunk that have block data stored by the given plugin
     * @param plugin The plugin the block data belongs to
     * @param chunk The chunk to scan for blocks with block data
     * */
    public static Set<Block> getBlocksWithCustomData(Plugin plugin, Chunk chunk) {
        String namespace = new NamespacedKey(plugin, "dummy").getNamespace();
        Set<Block> blocks = new HashSet<>();
        for (NamespacedKey key : chunk.getPersistentDataContainer().getKeys()) {
            if (!key.getNamespace().equals(namespace)) continue;
            Block block = blockFromKey(key, chunk);
            if (block != null) blocks.add(block);
        }
        return blocks;
    }

    private static NamespacedKey blockKey(Plugin plugin, Block block) {
        return new NamespacedKey(plugin, "x" + (block.getX() & 0xF) + "y" + block.getY() + "z" + (block.getZ() & 0xF));
    }

    @Nullable
    private static Block blockFromKey(NamespacedKey key, Chunk chunk) {
        Matcher matcher = BLOCK_KEY_REGEX.matcher(key.getKey());
        if (!matcher.matches()) return null;
        int x, y, z;
        try {
            x = Integer.parseInt(matcher.group(1));
            y = Integer.parseInt(matcher.group(2));
            z = Integer.parseInt(matcher.group(3));
        } catch (NumberFormatException e) {
            return null;
        }
        World world = chunk.getWorld();
        if (x < 0 || x > 15 || z < 0 || z > 15 || y < world.getMinHeight() || y > world.getMaxHeight() - 1) return null;
        return chunk.getBlock(x, y, z);
    }

    /**
     * A PersistentDataContainer for a single block, backed by the chunk's PersistentDataContainer.
     * Every mutation is saved back to the chunk immediately; the per-block entry is removed once empty.
     * Must only be used from the thread owning the block's region, like any other block access.
     */
    private static final class BlockPersistentDataContainer implements PersistentDataContainer {

        private final Chunk chunk;
        private final NamespacedKey key;
        private final PersistentDataContainer pdc;

        private BlockPersistentDataContainer(Block block, Plugin plugin) {
            this.chunk = block.getChunk();
            this.key = blockKey(plugin, block);
            PersistentDataContainer chunkPDC = chunk.getPersistentDataContainer();
            PersistentDataContainer blockPDC = chunkPDC.get(key, PersistentDataType.TAG_CONTAINER);
            this.pdc = blockPDC != null ? blockPDC : chunkPDC.getAdapterContext().newPersistentDataContainer();
        }

        private void save() {
            if (pdc.isEmpty()) chunk.getPersistentDataContainer().remove(key);
            else chunk.getPersistentDataContainer().set(key, PersistentDataType.TAG_CONTAINER, pdc);
        }

        @Override
        public <P, C> void set(@NotNull NamespacedKey namespacedKey, @NotNull PersistentDataType<P, C> type, @NotNull C value) {
            pdc.set(namespacedKey, type, value);
            save();
        }

        @Override
        public <P, C> boolean has(@NotNull NamespacedKey namespacedKey, @NotNull PersistentDataType<P, C> type) {
            return pdc.has(namespacedKey, type);
        }

        @Override
        public boolean has(@NotNull NamespacedKey namespacedKey) {
            return pdc.has(namespacedKey);
        }

        @Nullable
        @Override
        public <P, C> C get(@NotNull NamespacedKey namespacedKey, @NotNull PersistentDataType<P, C> type) {
            return pdc.get(namespacedKey, type);
        }

        @NotNull
        @Override
        public <P, C> C getOrDefault(@NotNull NamespacedKey namespacedKey, @NotNull PersistentDataType<P, C> type, @NotNull C defaultValue) {
            return pdc.getOrDefault(namespacedKey, type, defaultValue);
        }

        @NotNull
        @Override
        public Set<NamespacedKey> getKeys() {
            return pdc.getKeys();
        }

        @Override
        public void remove(@NotNull NamespacedKey namespacedKey) {
            pdc.remove(namespacedKey);
            save();
        }

        @Override
        public boolean isEmpty() {
            return pdc.isEmpty();
        }

        @Override
        public int getSize() {
            return pdc.getSize();
        }

        @Override
        public void copyTo(@NotNull PersistentDataContainer other, boolean replace) {
            pdc.copyTo(other, replace);
        }

        @NotNull
        @Override
        public PersistentDataAdapterContext getAdapterContext() {
            return pdc.getAdapterContext();
        }

        @Override
        public byte[] serializeToBytes() throws IOException {
            return pdc.serializeToBytes();
        }

        @Override
        public void readFromBytes(byte[] bytes, boolean clear) throws IOException {
            pdc.readFromBytes(bytes, clear);
            save();
        }
    }

    public static final Set<Material> UNBREAKABLE_BLOCKS = Sets.newHashSet(Material.BEDROCK, Material.BARRIER, Material.NETHER_PORTAL, Material.END_PORTAL_FRAME, Material.END_PORTAL, Material.END_GATEWAY);

    static {
        UNBREAKABLE_BLOCKS.add(Material.REINFORCED_DEEPSLATE);
        REPLACEABLE_BLOCKS = Tag.REPLACEABLE.getValues().stream().toList();
    }

    public static final List<Material> REPLACEABLE_BLOCKS;

    public static boolean isReplaceable(Block block) {
        return REPLACEABLE_BLOCKS.contains(block.getType());
    }

    public static boolean isReplaceable(BlockData blockData) {
        return REPLACEABLE_BLOCKS.contains(blockData.getMaterial());
    }

    public static boolean isReplaceable(Material material) {
        return REPLACEABLE_BLOCKS.contains(material);
    }

    /**
     * Improved version of {@link Material#isInteractable()} intended for replicating vanilla behavior.
     * Checks if the block one places against is interactable in the sense a chest is
     * Also checks if the block is an Oraxen block or not as NoteBlocks are Interacable
     */
    public static boolean isInteractable(Block placedAgainst) {
        if (placedAgainst == null) return false;

        NoteBlockMechanic noteBlockMechanic = OraxenBlocks.getNoteBlockMechanic(placedAgainst);
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(placedAgainst);
        Material type = placedAgainst.getType();

        if (noteBlockMechanic != null) return false;
        if (furnitureMechanic != null) return furnitureMechanic.isInteractable();
        if (Tag.STAIRS.isTagged(type)) return false;
        if (Tag.FENCES.isTagged(type)) return false;
        if (!type.isInteractable()) return false;
        return switch (type) {
            case PUMPKIN, MOVING_PISTON, REDSTONE_ORE, REDSTONE_WIRE -> false;
            default -> true;
        };
    }

    public static BlockState getState(Block block) {
        if(VersionUtil.isPaperServer()) {
            return block.getState(false);
        } else {
            return block.getState();
        }
    }

    public static void correctAllBlockStates(Block placedAgainst, Player player, EquipmentSlot hand, BlockFace face, ItemStack item) {
        if (NMSHandlers.getHandler() == null) return;
        // TODO Fix boats, currently Item#use in BoatItem calls PlayerInteractEvent
        // thus causing a StackOverflow, find a workaround
        if (Tag.ITEMS_BOATS.isTagged(item.getType())) return;

        NMSHandlers.getHandler().correctBlockStates(player, hand, item);

        Block target = placedAgainst.getRelative(face);
        if (target.getState() instanceof Sign sign) player.openSign(sign, Side.FRONT);
    }

}
