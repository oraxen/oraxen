package io.th0rgal.oraxen.utils;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.apache.commons.lang3.Range;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Attachable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.CoralWallFan;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.block.data.type.Lectern;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.block.data.type.SculkVein;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.block.data.type.TripwireHook;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.bukkit.block.data.FaceAttachable.AttachedFace.CEILING;
import static org.bukkit.block.data.FaceAttachable.AttachedFace.FLOOR;

public class BlockHelpers {

    public static void playCustomBlockSound(Location location, String sound, float volume, float pitch) {
        playCustomBlockSound(toCenterLocation(location), sound, SoundCategory.BLOCKS, volume, pitch);
    }

    public static void playCustomBlockSound(Location location, String sound, SoundCategory category, float volume, float pitch) {
        if (sound == null || location == null || location.getWorld() == null || category == null) return;
        location.getWorld().playSound(location, validateReplacedSounds(sound), category, volume, pitch);
    }

    public static String validateReplacedSounds(String sound) {
        ConfigurationSection mechanics = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds");
        if (mechanics == null) return sound;

        sound = sound.replace("minecraft:", "");
        if (sound.startsWith("block.wood") && mechanics.getBoolean("noteblock_and_block")) {
            return sound.replace("block.wood", "required.wood");
        } else if (sound.startsWith("block.stone") && mechanics.getBoolean("stringblock_and_furniture")) {
                return sound.replace("block.stone", "required.stone");
        }else return sound;
    }

    public static Location toBlockLocation(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Location toCenterLocation(Location location) {
        return toBlockLocation(location).clone().add(0.5, 0.5, 0.5);
    }

    public static Location toCenterBlockLocation(Location location) {
        return toCenterLocation(location).subtract(0,0.5,0);
    }

    public static Location toSimpleLocation(Location location) {
        return new Location(location.getWorld(), location.getX(), location.getY(), location.getZ(), 0f, 0f);
    }

    public static boolean isStandingInside(final Player player, final Block block) {
        if (player == null) return false;
        final Location playerLoc = player.getLocation();
        final Location blockLoc = BlockHelpers.toCenterLocation(block.getLocation());
        return Range.between(0.5, 1.5).contains(blockLoc.getY() - playerLoc.getY()) &&
                Range.between(-0.80, 0.80).contains(blockLoc.getX() - playerLoc.getX())
                && Range.between(-0.80, 0.80).contains(blockLoc.getZ() - playerLoc.getZ());
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

    public static final List<Material> REPLACEABLE_BLOCKS = Arrays
            .asList(Material.SNOW, Material.VINE, Material.GRASS, Material.TALL_GRASS, Material.SEAGRASS, Material.FERN,
                    Material.LARGE_FERN, Material.AIR);

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

    public static boolean correctAllBlockStates(Block block, Player player, BlockFace face, ItemStack item, boolean waterloggedBefore) {
        BlockData data = block.getBlockData();
        BlockState state = block.getState();
        Material type = block.getType();
        if (type == Material.SEAGRASS && !waterloggedBefore) return true;
        if (data instanceof Tripwire) return false;
        if (data instanceof Sapling && face != BlockFace.UP) return true;
        if (data instanceof Ladder && (face == BlockFace.UP || face == BlockFace.DOWN)) return true;
        if (type == Material.HANGING_ROOTS && face != BlockFace.DOWN) return true;
        if (type.toString().endsWith("TORCH") && face == BlockFace.DOWN) return true;
        if (data instanceof HangingSign && face == BlockFace.UP) return true;
        if (((state instanceof Sign && !(data instanceof HangingSign)) || state instanceof Banner) && face == BlockFace.DOWN) return true;
        if (data instanceof Ageable) return !handleAgeableBlocks(block, face);
        if (!(data instanceof Door) && (data instanceof Bisected || data instanceof Slab)) handleHalfBlocks(block, player);
        if (data instanceof Rotatable) handleRotatableBlocks(block, player);
        if (type.toString().contains("CORAL") && !type.toString().endsWith("CORAL_BLOCK") && face == BlockFace.DOWN) return true;
        if (type.toString().endsWith("CORAL") && block.getRelative(BlockFace.DOWN).getType() == Material.AIR) return true;
        if (type.toString().endsWith("_CORAL_FAN") && face != BlockFace.UP) block.setType(Material.valueOf(type.toString().replace("_CORAL_FAN", "_CORAL_WALL_FAN")));
        if (data instanceof Waterlogged) handleWaterlogged(block, face);
        if ((data instanceof Bed || data instanceof Chest || data instanceof Bisected) && !(data instanceof Stairs) && !(data instanceof TrapDoor)) if (!handleDoubleBlocks(block, player)) return true;
        if ((state instanceof Skull || state instanceof Sign || state instanceof Banner || type.toString().contains("TORCH")) && face != BlockFace.DOWN && face != BlockFace.UP) handleWallAttachable(block, face);

        if (!(data instanceof Stairs) && !(data instanceof HangingSign) && (data instanceof Directional || data instanceof FaceAttachable || data instanceof MultipleFacing || data instanceof Attachable)) {
            if (!(data instanceof SculkVein) && data instanceof MultipleFacing && face == BlockFace.UP) return true;
            if (data instanceof CoralWallFan && face == BlockFace.DOWN) return true;
            handleDirectionalBlocks(block, face);
        }

        if (data instanceof Orientable orientable) {
            if (face == BlockFace.UP || face == BlockFace.DOWN) orientable.setAxis(Axis.Y);
            else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) orientable.setAxis(Axis.Z);
            else if (face == BlockFace.WEST || face == BlockFace.EAST) orientable.setAxis(Axis.X);
            else orientable.setAxis(Axis.Y);
            block.setBlockData(orientable, false);
        }

        if (data instanceof Lantern lantern) {
            if (face != BlockFace.DOWN) return true;
            lantern.setHanging(true);
            block.setBlockData(lantern, false);
        }

        if (data instanceof Lectern lectern) {
            ((Lectern) data).setFacing(player.getFacing().getOppositeFace());
            block.setBlockData(lectern, false);
        }

        if (type.toString().endsWith("ANVIL")) {
            if (face == BlockFace.UP || face == BlockFace.DOWN)
                ((Directional) data).setFacing(getAnvilFacing(player.getFacing().getOppositeFace()));
            else ((Directional) data).setFacing(getAnvilFacing(face));
            block.setBlockData(data, false);
        }

        if (state instanceof BlockInventoryHolder invHolder) {
            Inventory inv = ((BlockInventoryHolder) ((BlockStateMeta) Objects.requireNonNull(item.getItemMeta())).getBlockState()).getInventory();
            for (ItemStack i : inv)
                if (i != null) invHolder.getInventory().addItem(i);
        }

        if (data instanceof Repeater repeater) {
            repeater.setFacing(player.getFacing().getOppositeFace());
            block.setBlockData(repeater, false);
        }

        if (data instanceof TripwireHook hook) {
            if (block.getRelative(player.getFacing()).getType().isSolid())
                hook.setFacing(player.getFacing().getOppositeFace());
            else {
                List<BlockFace> solidFaces = hook.getFaces().stream().filter(f -> block.getRelative(f).getType().isSolid()).toList();
                if (solidFaces.isEmpty()) return true;
                else hook.setFacing(solidFaces.get(0).getOppositeFace());
            }
            block.setBlockData(hook, false);
        }

        if (block.getState() instanceof Sign sign) player.openSign(sign);

        return false;
    }

    private static void handleWaterlogged(Block block, BlockFace face) {
        final BlockData data = block.getBlockData();
        if (data instanceof Waterlogged waterlogged) {
            if (data instanceof Directional directional && directional.getFaces().contains(face) && !(data instanceof Stairs))
                directional.setFacing(face);
            waterlogged.setWaterlogged(false);
        }
        block.setBlockData(data, false);
    }

    private static void handleWallAttachable(Block block, BlockFace face) {
        final Material type = block.getType();
        if (type.toString().endsWith("_BANNER"))
            block.setType(Material.valueOf(type.toString().replace("_BANNER", "_WALL_BANNER")));
        else if (type.toString().endsWith("TORCH"))
            block.setType(Material.valueOf(type.toString().replace("TORCH", "WALL_TORCH")));
        else if (type.toString().endsWith("HANGING_SIGN"))
            block.setType(Material.valueOf(type.toString().replace("_HANGING_SIGN", "_WALL_HANGING_SIGN")));
        else if (type.toString().endsWith("SIGN"))
            block.setType(Material.valueOf(type.toString().replace("_SIGN", "_WALL_SIGN")));
        else if (type.toString().endsWith("SKULL"))
            block.setType(Material.valueOf(type.toString().replace("_SKULL", "_WALL_SKULL")));
        else block.setType(Material.valueOf(type.toString().replace("_HEAD", "_WALL_HEAD")));

        final BlockData data = block.getBlockData();
        if (data instanceof Directional directional) directional.setFacing(face);
        if (data instanceof WallHangingSign hanging) hanging.setFacing(getWallHangingSignFacing(face.getOppositeFace()));
        block.setBlockData(data, false);
    }

    private static boolean handleDoubleBlocks(Block block, Player player) {
        final BlockData data = block.getBlockData();
        final Block up = block.getRelative(BlockFace.UP);
        if (data instanceof Door door) {
            if (up.getType().isSolid() || !BlockHelpers.REPLACEABLE_BLOCKS.contains(up.getType())) return false;
            if (getLeftBlock(block, player).getBlockData() instanceof Door)
                door.setHinge(Door.Hinge.RIGHT);
            else door.setHinge(Door.Hinge.LEFT);

            door.setFacing(player.getFacing());
            door.setHalf(Bisected.Half.TOP);
            block.getRelative(BlockFace.UP).setBlockData(door, false);
            door.setHalf(Bisected.Half.BOTTOM);
            block.setBlockData(door, false);
        } else if (data instanceof Bed bed) {
            final Block nextBlock = block.getRelative(player.getFacing());
            if (nextBlock.getType().isSolid() || !BlockHelpers.REPLACEABLE_BLOCKS.contains(nextBlock.getType()))
                return false;
            nextBlock.setType(block.getType(), false);
            final Bed nextData = (Bed) nextBlock.getBlockData();
            block.getRelative(player.getFacing()).setBlockData(bed, false);

            bed.setPart(Bed.Part.FOOT);
            nextData.setPart(Bed.Part.HEAD);
            bed.setFacing(player.getFacing());
            nextData.setFacing(player.getFacing());
            nextBlock.setBlockData(nextData, false);
            block.setBlockData(bed, false);
        } else if (data instanceof Chest chest) {
            if (getLeftBlock(block, player).getBlockData() instanceof Chest)
                chest.setType(Chest.Type.LEFT);
            else if (getRightBlock(block, player).getBlockData() instanceof Chest)
                chest.setType(Chest.Type.RIGHT);
            else chest.setType(Chest.Type.SINGLE);

            chest.setFacing(player.getFacing().getOppositeFace());
            block.setBlockData(chest, true);
        } else if (data instanceof Bisected bisected) {
            if (up.getType().isSolid() || !BlockHelpers.REPLACEABLE_BLOCKS.contains(up.getType())) return false;

            bisected.setHalf(Bisected.Half.TOP);
            block.getRelative(BlockFace.UP).setBlockData(bisected, false);
            bisected.setHalf(Bisected.Half.BOTTOM);
            block.setBlockData(bisected, false);
        } else {
            block.setBlockData(Bukkit.createBlockData(Material.AIR), false);
            return false;
        }
        return true;
    }

    private static void handleHalfBlocks(Block block, Player player) {
        final RayTraceResult eye = player.rayTraceBlocks(5.0, FluidCollisionMode.NEVER);
        final BlockData data = block.getBlockData();
        if (eye == null) return;
        final Block hitBlock = eye.getHitBlock();
        final BlockFace hitFace = eye.getHitBlockFace();
        final Location hitLoc = eye.getHitPosition().toLocation(block.getWorld());
        if (hitBlock == null || hitFace == null) return;

        if (data instanceof TrapDoor trapDoor) {
            trapDoor.setFacing(player.getFacing().getOppositeFace());
            if (eye.getHitBlockFace() == BlockFace.UP) trapDoor.setHalf(Bisected.Half.BOTTOM);
            else if (hitFace == BlockFace.DOWN) trapDoor.setHalf(Bisected.Half.TOP);
            else if (hitLoc.getY() <= toBlockLocation(hitBlock.getLocation()).getY())
                trapDoor.setHalf(Bisected.Half.BOTTOM);
            else trapDoor.setHalf(Bisected.Half.TOP);
        } else if (data instanceof Stairs stairs) {
            stairs.setFacing(player.getFacing());
            if (hitFace == BlockFace.UP) stairs.setHalf(Bisected.Half.BOTTOM);
            else if (hitFace == BlockFace.DOWN) stairs.setHalf(Bisected.Half.TOP);
            else if (hitLoc.getY() <= toCenterLocation(hitBlock.getLocation()).getY())
                stairs.setHalf(Bisected.Half.BOTTOM);
            else stairs.setHalf(Bisected.Half.TOP);
        } else if (data instanceof Slab slab) {
            if (hitFace == BlockFace.UP) slab.setType(Slab.Type.BOTTOM);
            else if (hitFace == BlockFace.DOWN) slab.setType(Slab.Type.TOP);
            else if (hitLoc.getY() <= toCenterLocation(hitBlock.getLocation()).getY())
                slab.setType(Slab.Type.BOTTOM);
            else slab.setType(Slab.Type.TOP);
        }
        block.setBlockData(data, false);
    }

    private static void handleRotatableBlocks(Block block, Player player) {
        final Rotatable data = (Rotatable) block.getBlockData();
        if (block.getType().toString().contains("SKULL") || block.getType().toString().contains("HEAD"))
            data.setRotation(getRelativeFacing(player));
        else data.setRotation(getRelativeFacing(player).getOppositeFace());

        block.setBlockData(data, false);
    }

    private static void handleDirectionalBlocks(Block block, BlockFace face) {
        final BlockData data = block.getBlockData();
        if (data instanceof Directional directional) {
            if (data instanceof FaceAttachable faceAttachable) {
                if (face == BlockFace.UP) faceAttachable.setAttachedFace(FLOOR);
                else if (face == BlockFace.DOWN) faceAttachable.setAttachedFace(CEILING);
                else directional.setFacing(face);
            } else if (directional.getFaces().contains(face)) directional.setFacing(face);
        } else if (data instanceof SculkVein sculkVein)
            sculkVein.setFace(face.getOppositeFace(), block.getRelative(face.getOppositeFace()).getType().isSolid());
        else if (data instanceof MultipleFacing multipleFacing) {
            for (BlockFace blockFace : multipleFacing.getAllowedFaces())
                multipleFacing.setFace(blockFace, block.getRelative(blockFace).getType().isSolid());
        } else if (data instanceof Attachable attachable)
            attachable.setAttached(true);
        block.setBlockData(data, false);
    }

    private static boolean handleAgeableBlocks(Block block, BlockFace face) {
        final Material type = block.getType();
        if (type.toString().contains("WEEPING_VINES")) return face == BlockFace.DOWN;
        else if (type.toString().contains("TWISTING_VINES")) return face == BlockFace.UP;
        else return false;
    }

    private static Block getLeftBlock(Block block, Player player) {
        BlockFace playerFacing = player.getFacing();
        Block leftBlock = switch (playerFacing) {
            case NORTH -> block.getRelative(BlockFace.WEST);
            case SOUTH -> block.getRelative(BlockFace.EAST);
            case WEST -> block.getRelative(BlockFace.SOUTH);
            case EAST -> block.getRelative(BlockFace.NORTH);
            default -> block;
        };

        boolean isChest = leftBlock.getBlockData() instanceof Chest chest &&
                (chest.getFacing() != player.getFacing().getOppositeFace());
        return isChest ? block : leftBlock;
    }

    private static Block getRightBlock(Block block, Player player) {
        BlockFace playerFacing = player.getFacing();
        Block rightBlock = switch (playerFacing) {
            case NORTH -> block.getRelative(BlockFace.EAST);
            case SOUTH -> block.getRelative(BlockFace.WEST);
            case WEST -> block.getRelative(BlockFace.NORTH);
            case EAST -> block.getRelative(BlockFace.SOUTH);
            default -> block;
        };
        boolean isChest =
                rightBlock.getBlockData() instanceof Chest chest &&
                        (chest.getFacing() != playerFacing.getOppositeFace());
        return isChest ? block : rightBlock;
    }

    private static BlockFace getRelativeFacing(Player player) {
        double yaw = player.getLocation().getYaw();
        BlockFace face = BlockFace.SELF;
        if (Range.between(0.0, 22.5).contains(yaw) || yaw >= 337.5 || yaw >= -22.5 && yaw <= 0.0 || yaw <= -337.5)
            face = BlockFace.SOUTH;
        else if (Range.between(22.5, 67.5).contains(yaw) || Range.between(-337.5, -292.5).contains(yaw))
            face = BlockFace.WEST;
        else if (Range.between(112.5, 157.5).contains(yaw) || Range.between(-292.5, -247.5).contains(yaw))
            face = BlockFace.NORTH_WEST;
        else if (Range.between(157.5, 202.5).contains(yaw) || Range.between(-202.5, -157.5).contains(yaw))
            face = BlockFace.NORTH;
        else if (Range.between(202.5, 247.5).contains(yaw) || Range.between(-157.5, -112.5).contains(yaw))
            face = BlockFace.NORTH_EAST;
        else if (Range.between(247.5, 292.5).contains(yaw) || Range.between(-112.5, -67.5).contains(yaw))
            face = BlockFace.EAST;
        else if (Range.between(292.5, 337.5).contains(yaw) || Range.between(-67.5, -22.5).contains(yaw))
            face = BlockFace.SOUTH_EAST;
        return face;
    }

    public static BlockFace getAnvilFacing(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            default -> BlockFace.NORTH;
        };
    }

    public static BlockFace getWallHangingSignFacing(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case WEST -> BlockFace.SOUTH;
            default -> BlockFace.NORTH;
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
