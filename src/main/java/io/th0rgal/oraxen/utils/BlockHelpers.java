package io.th0rgal.oraxen.utils;

import org.apache.commons.lang.math.IntRange;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BlockHelpers {

    public static Location toBlockLocation(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Location toCenterLocation(Location location) {
        return toBlockLocation(location).clone().add(0.5, 0.5, 0.5);
    }

    public static final List<Material> REPLACEABLE_BLOCKS = Arrays
            .asList(Material.SNOW, Material.VINE, Material.GRASS, Material.TALL_GRASS, Material.SEAGRASS, Material.FERN,
                    Material.LARGE_FERN, Material.AIR);

    public static boolean correctAllBlockStates(Block block, Player player, BlockFace face, ItemStack item) {
        final BlockData data = block.getBlockData();
        final BlockState state = block.getState();
        final Material type = block.getType();
        if (data instanceof Tripwire) return true;
        if (data instanceof Ladder && (face == BlockFace.UP || face == BlockFace.DOWN)) return false;
        if (type == Material.HANGING_ROOTS && face != BlockFace.DOWN) return false;
        if (type.toString().endsWith("TORCH") && face == BlockFace.DOWN) return false;
        if ((state instanceof Sign || state instanceof Banner) && face == BlockFace.DOWN) return false;
        if (data instanceof Ageable) return handleAgeableBlocks(block, face);
        if (!(data instanceof Door) && (data instanceof Bisected || data instanceof Slab))
            handleHalfBlocks(block, player);
        if (data instanceof Rotatable) handleRotatableBlocks(block, player);
        if (type.toString().contains("CORAL") && !type.toString().endsWith("CORAL_BLOCK") && face == BlockFace.DOWN)
            return false;
        if (type.toString().endsWith("CORAL") && block.getRelative(BlockFace.DOWN).getType() == Material.AIR)
            return false;
        if (type.toString().endsWith("_CORAL_FAN") && face != BlockFace.UP)
            block.setType(Material.valueOf(type.toString().replace("_CORAL_FAN", "_CORAL_WALL_FAN")));
        if (data instanceof Waterlogged) handleWaterlogged(block, face);
        if ((data instanceof Bed || data instanceof Chest || data instanceof Bisected) &&
                !(data instanceof Stairs) && !(data instanceof TrapDoor))
            if (!handleDoubleBlocks(block, player)) return false;
        if ((state instanceof Skull || state instanceof Sign || state instanceof Banner || type.toString().contains("TORCH")) && face != BlockFace.DOWN && face != BlockFace.UP)
            handleWallAttachable(block, face);

        if (!(data instanceof Stairs) && (data instanceof Directional || data instanceof FaceAttachable || data instanceof MultipleFacing || data instanceof Attachable)) {
            if (data instanceof MultipleFacing && face == BlockFace.UP) return false;
            if (data instanceof CoralWallFan && face == BlockFace.DOWN) return false;
            handleDirectionalBlocks(block, face);
        }

        if (data instanceof Orientable) {
            if (face == BlockFace.UP || face == BlockFace.DOWN) ((Orientable) data).setAxis(Axis.Y);
            else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) ((Orientable) data).setAxis(Axis.Z);
            else if (face == BlockFace.WEST || face == BlockFace.EAST) ((Orientable) data).setAxis(Axis.X);
            else ((Orientable) data).setAxis(Axis.Y);
            block.setBlockData(data, false);
        }

        if (data instanceof Lantern) {
            if (face != BlockFace.DOWN) return false;
            ((Lantern) data).setHanging(true);
            block.setBlockData(data, false);
        }

        if (state instanceof BlockInventoryHolder) {
            Inventory inv = ((Container) ((BlockStateMeta) Objects.requireNonNull(item.getItemMeta())).getBlockState()).getInventory();
            for (ItemStack i : inv)
                if (i != null) ((BlockInventoryHolder) block.getState()).getInventory().addItem(i);
        }

        if (type.toString().endsWith("ANVIL")) {
            ((Directional) data).setFacing(getAnvilFacing(face));
            block.setBlockData(data, false);
        }

        if (block.getState() instanceof Sign)
            player.openSign((Sign) block.getState());

        return true;
    }

    private static void handleWaterlogged(Block block, BlockFace face) {
        final BlockData data = block.getBlockData();
        if (data instanceof Waterlogged) {
            if (data instanceof Directional && ((Directional) data).getFaces().contains(face) && !(data instanceof Stairs))
                ((Directional) data).setFacing(face);
            ((Waterlogged) data).setWaterlogged(false);
        }
        block.setBlockData(data, false);
    }

    private static void handleWallAttachable(Block block, BlockFace face) {
        final Material type = block.getType();
        if (type.toString().endsWith("_BANNER"))
            block.setType(Material.valueOf(type.toString().replace("_BANNER", "_WALL_BANNER")));
        else if (type.toString().endsWith("TORCH"))
            block.setType(Material.valueOf(type.toString().replace("TORCH", "WALL_TORCH")));
        else if (type.toString().endsWith("SIGN"))
            block.setType(Material.valueOf(type.toString().replace("_SIGN", "_WALL_SIGN")));
        else if (type.toString().endsWith("SKULL"))
            block.setType(Material.valueOf(type.toString().replace("_SKULL", "_WALL_SKULL")));
        else block.setType(Material.valueOf(type.toString().replace("_HEAD", "_WALL_HEAD")));

        final Directional data = (Directional) Bukkit.createBlockData(block.getType());
        data.setFacing(face);
        block.setBlockData(data, false);
    }

    private static boolean handleDoubleBlocks(Block block, Player player) {
        final BlockData data = block.getBlockData();
        final Block up = block.getRelative(BlockFace.UP);
        if (data instanceof Door) {
            if (up.getType().isSolid() || !BlockHelpers.REPLACEABLE_BLOCKS.contains(up.getType())) return false;
            if (getLeftBlock(block, player).getBlockData() instanceof Door)
                ((Door) data).setHinge(Door.Hinge.RIGHT);
            else ((Door) data).setHinge(Door.Hinge.LEFT);

            ((Door) data).setFacing(player.getFacing());
            ((Door) data).setHalf(Bisected.Half.TOP);
            block.getRelative(BlockFace.UP).setBlockData(data, false);
            ((Door) data).setHalf(Bisected.Half.BOTTOM);
            block.setBlockData(data, false);
        } else if (data instanceof Bed) {
            final Block nextBlock = block.getRelative(player.getFacing());
            if (nextBlock.getType().isSolid() || !BlockHelpers.REPLACEABLE_BLOCKS.contains(nextBlock.getType()))
                return false;
            nextBlock.setType(block.getType(), false);
            final Bed nextData = (Bed) nextBlock.getBlockData();
            block.getRelative(player.getFacing()).setBlockData(data, false);

            ((Bed) data).setPart(Bed.Part.FOOT);
            nextData.setPart(Bed.Part.HEAD);
            ((Bed) data).setFacing(player.getFacing());
            nextData.setFacing(player.getFacing());
            nextBlock.setBlockData(nextData, false);
            block.setBlockData(data, false);
        } else if (data instanceof Chest) {
            if (getLeftBlock(block, player).getBlockData() instanceof Chest)
                ((Chest) data).setType(Chest.Type.LEFT);
            else if (getRightBlock(block, player).getBlockData() instanceof Chest)
                ((Chest) data).setType(Chest.Type.RIGHT);
            else ((Chest) data).setType(Chest.Type.SINGLE);

            ((Chest) data).setFacing(player.getFacing().getOppositeFace());
            block.setBlockData(data, true);
        } else if (data instanceof Bisected) {
            if (up.getType().isSolid() || !BlockHelpers.REPLACEABLE_BLOCKS.contains(up.getType())) return false;

            ((Bisected) data).setHalf(Bisected.Half.TOP);
            block.getRelative(BlockFace.UP).setBlockData(data, false);
            ((Bisected) data).setHalf(Bisected.Half.BOTTOM);
            block.setBlockData(data, false);
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

        if (data instanceof TrapDoor) {
            ((TrapDoor) data).setFacing(player.getFacing().getOppositeFace());
            if (eye.getHitBlockFace() == BlockFace.UP) ((TrapDoor) data).setHalf(Bisected.Half.BOTTOM);
            else if (hitFace == BlockFace.DOWN) ((TrapDoor) data).setHalf(Bisected.Half.TOP);
            else if (hitLoc.getY() <= toBlockLocation(hitBlock.getLocation()).getY())
                ((TrapDoor) data).setHalf(Bisected.Half.BOTTOM);
            else ((TrapDoor) data).setHalf(Bisected.Half.TOP);
        } else if (data instanceof Stairs) {
            ((Stairs) data).setFacing(player.getFacing());
            if (hitLoc.getY() <= toCenterLocation(hitBlock.getLocation()).getY())
                ((Stairs) data).setHalf(Bisected.Half.BOTTOM);
            else ((Stairs) data).setHalf(Bisected.Half.TOP);
        } else if (data instanceof Slab) {
            if (hitLoc.getY() <= toCenterLocation(hitBlock.getLocation()).getY())
                ((Slab) data).setType(Slab.Type.BOTTOM);
            else ((Slab) data).setType(Slab.Type.TOP);
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
        if (data instanceof Directional) {
            if (data instanceof FaceAttachable) {
                if (face == BlockFace.UP) ((FaceAttachable) data).setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
                else if (face == BlockFace.DOWN)
                    ((FaceAttachable) data).setAttachedFace(FaceAttachable.AttachedFace.CEILING);
                else ((Directional) data).setFacing(face);
            } else if (((Directional) data).getFaces().contains(face)) ((Directional) data).setFacing(face);
        } else if (data instanceof MultipleFacing) {
            for (BlockFace blockFace : ((MultipleFacing) data).getAllowedFaces())
                ((MultipleFacing) data).setFace(blockFace, block.getRelative(blockFace).getType().isSolid());
        } else if (data instanceof Attachable) {
            ((Attachable) data).setAttached(true);
        }
        block.setBlockData(data, false);
    }

    private static boolean handleAgeableBlocks(Block block, BlockFace face) {
        final Material type = block.getType();
        if (type.toString().contains("WEEPING_VINES")) return face == BlockFace.DOWN;
        else if (type.toString().contains("TWISTING_VINES")) return face == BlockFace.UP;
        else return false;
    }

    private static Block getLeftBlock(Block block, Player player) {
        Block leftBlock;
        if (player.getFacing() == BlockFace.NORTH) leftBlock = block.getRelative(BlockFace.WEST);
        else if (player.getFacing() == BlockFace.SOUTH) leftBlock = block.getRelative(BlockFace.EAST);
        else if (player.getFacing() == BlockFace.WEST) leftBlock = block.getRelative(BlockFace.SOUTH);
        else if (player.getFacing() == BlockFace.EAST) leftBlock = block.getRelative(BlockFace.NORTH);
        else leftBlock = block;

        if (leftBlock.getBlockData() instanceof Chest &&
                (((Chest) leftBlock.getBlockData()).getFacing() != player.getFacing().getOppositeFace())) return block;
        else return leftBlock;
    }

    private static Block getRightBlock(Block block, Player player) {
        Block rightBlock;
        if (player.getFacing() == BlockFace.NORTH) rightBlock = block.getRelative(BlockFace.EAST);
        else if (player.getFacing() == BlockFace.SOUTH) rightBlock = block.getRelative(BlockFace.WEST);
        else if (player.getFacing() == BlockFace.WEST) rightBlock = block.getRelative(BlockFace.NORTH);
        else if (player.getFacing() == BlockFace.EAST) rightBlock = block.getRelative(BlockFace.SOUTH);
        else rightBlock = block;

        if (rightBlock.getBlockData() instanceof Chest &&
                (((Chest) rightBlock.getBlockData()).getFacing() != player.getFacing().getOppositeFace())) return block;
        else return rightBlock;
    }

    private static BlockFace getRelativeFacing(Player player) {
        double yaw = player.getLocation().getYaw();
        BlockFace face = BlockFace.SELF;
        if (new IntRange(0.0, 22.5).containsDouble(yaw) || yaw >= 337.5 || yaw >= -22.5 && yaw <= 0.0 || yaw <= -337.5)
            face = BlockFace.SOUTH;
        else if (new IntRange(22.5, 67.5).containsDouble(yaw) || new IntRange(-337.5, -292.5).containsDouble(yaw))
            face = BlockFace.WEST;
        else if (new IntRange(112.5, 157.5).containsDouble(yaw) || new IntRange(-292.5, -247.5).containsDouble(yaw))
            face = BlockFace.NORTH_WEST;
        else if (new IntRange(157.5, 202.5).containsDouble(yaw) || new IntRange(-202.5, -157.5).containsDouble(yaw))
            face = BlockFace.NORTH;
        else if (new IntRange(202.5, 247.5).containsDouble(yaw) || new IntRange(-157.5, -112.5).containsDouble(yaw))
            face = BlockFace.NORTH_EAST;
        else if (new IntRange(247.5, 292.5).containsDouble(yaw) || new IntRange(-112.5, -67.5).containsDouble(yaw))
            face = BlockFace.EAST;
        else if (new IntRange(292.5, 337.5).containsDouble(yaw) || new IntRange(-67.5, -22.5).containsDouble(yaw))
            face = BlockFace.SOUTH_EAST;
        return face;
    }

    public static BlockFace getAnvilFacing(BlockFace face) {
        BlockFace f = BlockFace.SELF;
        switch (face) {
            case NORTH -> f = BlockFace.EAST;
            case EAST -> f = BlockFace.NORTH;
            case SOUTH -> f = BlockFace.WEST;
            case WEST -> f = BlockFace.SOUTH;
        }
        return f;
    }
}
