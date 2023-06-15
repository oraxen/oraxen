package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;

public class BlockLocation {

    private int x;
    private int y;
    private int z;

    public BlockLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockLocation(Location location) {
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
    }

    public BlockLocation(String serializedBlockLocation) {
        String[] values = serializedBlockLocation.split(",");
        this.x = Integer.parseInt(values[0]);
        this.y = Integer.parseInt(values[1]);
        this.z = Integer.parseInt(values[2]);
    }

    public BlockLocation(Map<String, Object> coordinatesMap) {
        this.x = (Integer) coordinatesMap.get("x");
        this.y = (Integer) coordinatesMap.get("y");
        this.z = (Integer) coordinatesMap.get("z");
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }

    public BlockLocation add(BlockLocation blockLocation) {
        BlockLocation output = new BlockLocation(x, y, z);
        output.x += blockLocation.x;
        output.y += blockLocation.y;
        output.z += blockLocation.z;
        return output;
    }

    public Location add(Location location) {
        return location.clone().add(x, y, z);
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    public BlockLocation groundRotate(float angle) {
        BlockLocation output = new BlockLocation(x, y, z);
        float fixedAngle = (360 - angle);
        double radians = Math.toRadians(fixedAngle);
        output.x = ((int) Math.round(Math.cos(radians) * x - Math.sin(radians) * z));
        output.z = ((int) Math.round(Math.sin(radians) * x - Math.cos(radians) * z));
        if (fixedAngle % 180 > 1)
            output.z = -output.z;
        return output;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
