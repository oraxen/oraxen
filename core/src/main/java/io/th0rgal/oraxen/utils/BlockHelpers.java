package io.th0rgal.oraxen.utils;

import com.google.common.collect.Sets;
import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

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
        playCustomBlockSound(toCenterLocation(location), sound, SoundCategory.BLOCKS, volume, pitch);
    }

    public static void playCustomBlockSound(Location location, String sound, SoundCategory category, float volume, float pitch) {
        if (sound == null || location == null || location.getWorld() == null || category == null) return;
        location.getWorld().playSound(location, validateReplacedSounds(sound), category, volume, pitch);
    }

    public static String validateReplacedSounds(String sound) {
        ConfigurationSection mechanics = OraxenPlugin.get().configsManager().getMechanics().getConfigurationSection("custom_block_sounds");
        if (sound == null || mechanics == null) return sound;

        sound = sound.replace("minecraft:", "");
        if (sound.startsWith("block.wood") && mechanics.getBoolean("noteblock")) {
            return sound.replace("block.wood", "oraxen:required.wood");
        } else if (sound.startsWith("block.stone") && mechanics.getBoolean("stringblock_and_furniture")) {
                return sound.replace("block.stone", "oraxen:required.stone");
        } else return sound;
    }

    public static Location toBlockLocation(Location location) {
        Location blockLoc = location.clone();
        blockLoc.setX(location.getBlockX());
        blockLoc.setY(location.getBlockY());
        blockLoc.setZ(location.getBlockZ());
        return blockLoc;
    }

    public static Location toCenterLocation(Location location) {
        Location centerLoc = location.clone();
        centerLoc.setX(location.getBlockX() + 0.5);
        centerLoc.setY(location.getBlockY() + 0.5);
        centerLoc.setZ(location.getBlockZ() + 0.5);
        return centerLoc;
    }

    public static Location toCenterBlockLocation(Location location) {
        return toCenterLocation(location).subtract(0,0.5,0);
    }

    public static boolean isStandingInside(final Player player, final Block block) {
        if (player == null || block == null) return false;
        // Since the block might be AIR, Block#getBoundingBox returns an empty one
        // Get the block-center and expand it 0.5 to cover the block
        BoundingBox blockBox = BoundingBox.of(BlockHelpers.toCenterLocation(block.getLocation()), 0.5, 0.5, 0.5);

        return !block.getWorld().getNearbyEntities(blockBox).stream()
                .filter(e -> e instanceof LivingEntity && (!(e instanceof Player p) || p.getGameMode() != GameMode.SPECTATOR))
                .toList().isEmpty();
    }

    /** Returns the PersistentDataContainer from CustomBlockData
     * @param block The block to get the PersistentDataContainer for
     * */
    public static PersistentDataContainer getPDC(Block block) {
        return getPDC(block, OraxenPlugin.get());
    }

    /** Returns the PersistentDataContainer from CustomBlockData
     * @param block The block to get the PersistentDataContainer for
     * @param plugin The plugin to get the CustomBlockData from
     * */
    public static PersistentDataContainer getPDC(Block block, JavaPlugin plugin) {
        return new CustomBlockData(block, plugin);
    }

    public static final Set<Material> UNBREAKABLE_BLOCKS = Sets.newHashSet(Material.BEDROCK, Material.BARRIER, Material.NETHER_PORTAL, Material.END_PORTAL_FRAME, Material.END_PORTAL, Material.END_GATEWAY);

    static {
        UNBREAKABLE_BLOCKS.add(Material.REINFORCED_DEEPSLATE);
        REPLACEABLE_BLOCKS = Tag.REPLACEABLE.getValues().stream().toList();
    }

    public static final List<Material> REPLACEABLE_BLOCKS;

    public static boolean isReplaceable(Block block) {
        if (block.getBlockData() instanceof Snow snow) return snow.getLayers() == 1;
        return REPLACEABLE_BLOCKS.contains(block.getType());
    }

    public static boolean isReplaceable(BlockData blockData) {
        if (blockData instanceof Snow snow) return snow.getLayers() == 1;
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
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(placedAgainst.getLocation());
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

    /*
     * Calling loc.getChunk() will crash a Paper 1.19 build 62-66 (possibly more) Server if the Chunk does not exist.
     * Instead, get Chunk location and check with World.isChunkLoaded() if the Chunk is loaded.
     */
    public static boolean isLoaded(World world, Location loc) {
        return world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    public static boolean isLoaded(Location loc) {
        return loc.getWorld() != null && isLoaded(loc.getWorld(), loc);
    }
}
