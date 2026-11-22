package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Set;

record FurnitureShulkerHitbox(double x, double y, double z, double scale, int peek, BlockFace direction) {

    private static final Set<BlockFace> DIRECTIONS = Set.of(BlockFace.UP, BlockFace.DOWN,
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    static FurnitureShulkerHitbox parse(Object value) {
        if (!(value instanceof String entry)) return null;
        String[] parts = entry.trim().split("\\s+");
        if (parts.length < 2 || parts.length > 4) return null;
        String[] coordinates = parts[0].split(",", -1);
        if (coordinates.length != 3) return null;

        try {
            double x = Double.parseDouble(coordinates[0]);
            double y = Double.parseDouble(coordinates[1]);
            double z = Double.parseDouble(coordinates[2]);
            double scale = Double.parseDouble(parts[1]);
            int peek = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            BlockFace direction = parts.length > 3 ? BlockFace.valueOf(parts[3].toUpperCase(Locale.ROOT)) : BlockFace.UP;
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Double.isFinite(scale)
                    || scale < 0.0625 || scale > 3 || peek < 0 || peek > 100 || !DIRECTIONS.contains(direction))
                return null;
            return new FurnitureShulkerHitbox(x, y, z, scale, peek, direction);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    Vector offset() {
        return new Vector(x, y, z);
    }

    // The shulker opens away from the face it attaches to.
    static BlockFace attachedFace(BlockFace direction, float yaw) {
        if (direction == BlockFace.UP) return BlockFace.DOWN;
        if (direction == BlockFace.DOWN) return BlockFace.UP;
        Vector rotated = FurnitureMechanic.rotateGroundOffset(new Vector(direction.getModX(), 0, direction.getModZ()), yaw);
        BlockFace facing = Math.abs(rotated.getX()) >= Math.abs(rotated.getZ())
                ? rotated.getX() >= 0 ? BlockFace.EAST : BlockFace.WEST
                : rotated.getZ() >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        return facing.getOppositeFace();
    }
}
