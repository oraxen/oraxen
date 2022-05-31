package io.th0rgal.oraxen.utils;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class BlockHelpers {

    public static boolean correctAllBlockStates(Block block, Player player, BlockFace face) {
        final BlockData data = block.getBlockData();
        final BlockState state = block.getState();
        final Material type = block.getType();
        if (data instanceof Tripwire) return true;
        if (data instanceof Ladder && (face == BlockFace.UP || face == BlockFace.DOWN)) return false;
        if (type == Material.HANGING_ROOTS && face != BlockFace.DOWN) return false;
        if (type.toString().endsWith("TORCH") && face == BlockFace.DOWN) return false;
        if (state instanceof Sign && face == BlockFace.DOWN) return false;
        if (data instanceof Ageable) return handleAgeableBlocks(block, face);
        if (!(data instanceof Door) && (data instanceof Bisected || data instanceof Slab)) handleHalfBlocks(block, player);
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
        if ((state instanceof Skull || state instanceof Sign || type.toString().contains("TORCH")) && face != BlockFace.DOWN && face != BlockFace.UP)
            handleWallAttachable(block, player, face);

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

    private static void handleWallAttachable(Block block, Player player, BlockFace face) {
        final Material type = block.getType();
        if (type.toString().endsWith("TORCH"))
            block.setType(Material.valueOf(type.toString().replace("TORCH", "WALL_TORCH")));
        else if (type.toString().endsWith("SIGN"))
            block.setType(Material.valueOf(type.toString().replace("_SIGN", "_WALL_SIGN")));
        else if (type.toString().endsWith("SKULL"))
            block.setType(Material.valueOf(type.toString().replace("_SKULL", "_WALL_SKULL")));
        else block.setType(Material.valueOf(type.toString().replace("_HEAD", "_WALL_HEAD")));

        final Directional data = (Directional) Bukkit.createBlockData(block.getType());
        data.setFacing(face);
        block.setBlockData(data, false);
        if (block.getState() instanceof Sign) player.openSign((Sign) block.getState());
    }

    private static boolean handleDoubleBlocks(Block block, Player player) {
        final BlockData data = block.getBlockData();
        final Block up = block.getRelative(BlockFace.UP);
        if (data instanceof Door) {
            if (up.getType().isSolid() || !Utils.REPLACEABLE_BLOCKS.contains(up.getType())) return false;
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
            if (nextBlock.getType().isSolid() || !Utils.REPLACEABLE_BLOCKS.contains(nextBlock.getType())) return false;
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
            if (up.getType().isSolid() || !Utils.REPLACEABLE_BLOCKS.contains(up.getType())) return false;

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

        if (data instanceof TrapDoor) {
            ((TrapDoor) data).setFacing(player.getFacing().getOppositeFace());
            if (eye.getHitBlockFace() == BlockFace.UP) ((TrapDoor) data).setHalf(Bisected.Half.BOTTOM);
            else if (eye.getHitBlockFace() == BlockFace.DOWN) ((TrapDoor) data).setHalf(Bisected.Half.TOP);
            else if (eye.getHitPosition().getY() <= eye.getHitBlock().getLocation().clone().add(0.5, 0.0, 0.5).getY())
                ((TrapDoor) data).setHalf(Bisected.Half.BOTTOM);
            else ((TrapDoor) data).setHalf(Bisected.Half.TOP);
        } else if (data instanceof Stairs) {
            ((Stairs) data).setFacing(player.getFacing());
            if (eye.getHitPosition().getY() <= eye.getHitBlock().getLocation().clone().add(0.0, 0.75, 0.0).getY())
                ((Stairs) data).setHalf(Bisected.Half.BOTTOM);
            else ((Stairs) data).setHalf(Bisected.Half.TOP);
        } else if (data instanceof Slab) {
            if (eye.getHitPosition().getY() <= eye.getHitBlock().getLocation().clone().add(0.5, 0.0, 0.5).getY())
                ((Slab) data).setType(Slab.Type.BOTTOM);
            else ((Slab) data).setType(Slab.Type.TOP);
        }
        block.setBlockData(data, false);
    }

    private static void handleRotatableBlocks(Block block, Player player) {
        final BlockData data = block.getBlockData();
        if (data instanceof Rotatable) {
            ((Rotatable) data).setRotation(player.getFacing());
        }
        block.setBlockData(data, false);
    }

    private static void handleDirectionalBlocks(Block block, BlockFace face) {
        final BlockData data = block.getBlockData();
        Bukkit.broadcastMessage(data.getMaterial().toString());
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
}
