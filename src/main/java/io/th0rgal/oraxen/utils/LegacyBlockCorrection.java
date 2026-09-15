package io.th0rgal.oraxen.utils;

import org.apache.commons.lang3.Range;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Lectern;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.bukkit.block.data.FaceAttachable.AttachedFace.CEILING;
import static org.bukkit.block.data.FaceAttachable.AttachedFace.FLOOR;

/**
 * Bukkit-API-only correction of vanilla block states when a vanilla block is
 * placed against a custom (noteblock/stringblock) block.
 * <p>
 * Used as the fallback on servers where the NMS handler is unavailable
 * (below 1.21.2, or when the handler failed to load); on 1.21.2+ the NMS
 * handler performs this work instead. Restored from the pre-1.219 LEGACY
 * block-correction path. This whole class can be deleted once support for
 * versions below 1.21.2 is dropped.
 */
final class LegacyBlockCorrection {

    private LegacyBlockCorrection() {
    }

    static void correctAllBlockStates(Block target, Player player, EquipmentSlot hand, BlockFace face, ItemStack item, @Nullable BlockData newData) {
        if (newData == null) return;
        // The BlockData needs to be set beforehand, unlike the NMS-method
        BlockData oldData = target.getBlockData();
        target.setBlockData(newData);
        BlockData correctedData = correctBlockData(target, player, face, item);
        if (correctedData == null) {
            target.setBlockData(oldData);
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
        Utils.swingHand(player, hand);
    }

    @Nullable
    private static BlockData correctBlockData(Block block, Player player, BlockFace face, ItemStack item) {
        BlockData data = block.getBlockData();
        BlockState state = block.getState();
        Material type = block.getType();

        if (type == Material.SEAGRASS) return null;
        if (data instanceof Tripwire) return data;
        if (data instanceof Sapling && face != BlockFace.UP) return null;
        if (data instanceof Ladder && (face == BlockFace.UP || face == BlockFace.DOWN)) return null;
        if (type == Material.HANGING_ROOTS && face != BlockFace.DOWN) return null;
        if (type.toString().endsWith("TORCH") && face == BlockFace.DOWN) return null;
        if (type.toString().endsWith("HANGING_SIGN") && face == BlockFace.UP) return null;
        if (((state instanceof Sign && !type.toString().endsWith("HANGING_SIGN")) || state instanceof Banner) && face == BlockFace.DOWN) return null;
        if (data instanceof Ageable) return !handleAgeableBlocks(block, face) ? data : null;
        if (!(data instanceof Door) && (data instanceof Bisected || data instanceof Slab)) handleHalfBlocks(block, player);
        if (data instanceof Rotatable) handleRotatableBlocks(block, player);
        if (type.toString().contains("CORAL") && !type.toString().endsWith("CORAL_BLOCK") && face == BlockFace.DOWN) return null;
        if (type.toString().endsWith("CORAL") && block.getRelative(BlockFace.DOWN).getType() == Material.AIR) return null;
        if (type.toString().endsWith("_CORAL_FAN") && face != BlockFace.UP) block.setType(Material.valueOf(type.toString().replace("_CORAL_FAN", "_CORAL_WALL_FAN")));
        if (data instanceof Waterlogged) handleWaterlogged(block, face);
        if ((data instanceof Bed || data instanceof Chest || data instanceof Bisected) && !(data instanceof Stairs) && !(data instanceof TrapDoor)) if (!handleDoubleBlocks(block, player)) return null;
        if ((state instanceof Skull || state instanceof Sign || state instanceof Banner || type.toString().contains("TORCH")) && face != BlockFace.DOWN && face != BlockFace.UP) handleWallAttachable(block, face);

        if (!(data instanceof Stairs) && !type.toString().endsWith("HANGING_SIGN") && (data instanceof Directional || data instanceof FaceAttachable || data instanceof MultipleFacing || data instanceof Attachable)) {
            if (!(data instanceof SculkVein) && data instanceof MultipleFacing && face == BlockFace.UP) return null;
            if (data instanceof CoralWallFan && face == BlockFace.DOWN) return null;
            handleDirectionalBlocks(block, face);
        }

        // The handlers above write their changes directly to the block, so refresh
        // the local snapshot before mutating it further. The mutations below are
        // committed to the block at the end of this method; without that write-back
        // the corrected axis, hanging state and facing would be discarded.
        data = block.getBlockData();

        if (data instanceof Orientable orientable) {
            if (face == BlockFace.UP || face == BlockFace.DOWN) orientable.setAxis(Axis.Y);
            else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) orientable.setAxis(Axis.Z);
            else if (face == BlockFace.WEST || face == BlockFace.EAST) orientable.setAxis(Axis.X);
            else orientable.setAxis(Axis.Y);
        }

        if (data instanceof Lantern lantern) {
            lantern.setHanging(face == BlockFace.DOWN);
        }

        if (data instanceof Lectern lectern) {
            lectern.setFacing(player.getFacing().getOppositeFace());
        }

        if (type.toString().endsWith("ANVIL")) {
            if (face == BlockFace.UP || face == BlockFace.DOWN)
                ((Directional) data).setFacing(getAnvilFacing(player.getFacing().getOppositeFace()));
            else ((Directional) data).setFacing(getAnvilFacing(face));
        }

        if (state instanceof BlockInventoryHolder invHolder && item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
            Inventory inv = ((BlockInventoryHolder) blockStateMeta.getBlockState()).getInventory();
            invHolder.getInventory().setContents(inv.getContents());
        }

        if (data instanceof Repeater repeater) {
            repeater.setFacing(player.getFacing().getOppositeFace());
        }

        if (block.getState() instanceof Banner banner && item.hasItemMeta() && item.getItemMeta() instanceof BannerMeta bannerItem) {
            banner.setPatterns(bannerItem.getPatterns());
            if (!banner.update(false, false)) return null;
        }

        if (data instanceof TripwireHook hook) {
            if (block.getRelative(player.getFacing()).getType().isSolid())
                hook.setFacing(player.getFacing().getOppositeFace());
            else {
                List<BlockFace> solidFaces = hook.getFaces().stream().filter(f -> block.getRelative(f).getType().isSolid()).toList();
                if (solidFaces.isEmpty()) return null;
                else hook.setFacing(solidFaces.get(0).getOppositeFace());
            }
        }

        block.setBlockData(data, false);
        return data;
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
        if (data.getMaterial().toString().endsWith("WALL_HANGING_SIGN")) {
            assert data instanceof WallHangingSign;
            ((WallHangingSign) data).setFacing(getWallHangingSignFacing(face.getOppositeFace()));
        }
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
        else if (Range.between(67.5, 112.5).contains(yaw) || Range.between(-292.5, -247.5).contains(yaw))
            face = BlockFace.SOUTH_WEST;
        else if (Range.between(112.5, 157.5).contains(yaw) || Range.between(-247.5, -202.5).contains(yaw))
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

    private static BlockFace getAnvilFacing(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            default -> BlockFace.NORTH;
        };
    }

    private static BlockFace getWallHangingSignFacing(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case WEST -> BlockFace.SOUTH;
            default -> BlockFace.NORTH;
        };
    }

    private static Location toBlockLocation(Location location) {
        final Location blockLoc = location.clone();
        blockLoc.setX(location.getBlockX());
        blockLoc.setY(location.getBlockY());
        blockLoc.setZ(location.getBlockZ());
        return blockLoc;
    }

    private static Location toCenterLocation(Location location) {
        final Location centerLoc = location.clone();
        centerLoc.setX(location.getBlockX() + 0.5);
        centerLoc.setY(location.getBlockY() + 0.5);
        centerLoc.setZ(location.getBlockZ() + 0.5);
        return centerLoc;
    }
}
