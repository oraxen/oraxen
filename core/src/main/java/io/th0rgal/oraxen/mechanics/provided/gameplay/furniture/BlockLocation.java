package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.morepersistentdatatypes.datatypes.serializable.ConfigurationSerializableDataType;
import org.bukkit.Location;
import org.bukkit.Utility;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;

import java.util.*;

public class BlockLocation implements ConfigurationSerializable {
    public static PersistentDataType<byte[],BlockLocation> dataType = new ConfigurationSerializableDataType<>(BlockLocation.class);

    private int x;
    private int y;
    private int z;

    public static BlockLocation ZERO = new BlockLocation(0,0,0);

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

    public BlockLocation(Map<String, Integer> coordinatesMap) {
        this.x = coordinatesMap.getOrDefault("x", 0);
        this.y = coordinatesMap.getOrDefault("y", 0);
        this.z = coordinatesMap.getOrDefault("z", 0);
    }

    public BlockLocation(String location) {
        if (location.equals("origin")) {
            this.x = 0;
            this.y = 0;
            this.z = 0;
        } else {
            List<Integer> split = new ArrayList<>(Arrays.stream(location.split(",")).map(s -> {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }).toList());
            while (split.size() < 3) split.add(0);

            this.x = split.get(0);
            this.y = split.get(1);
            this.z = split.get(2);
        }
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
        if (angle < 0) angle += 360;  // Ensure angle is positive
        double radians = Math.toRadians(angle);

        // Standard 2D rotation matrix for counterclockwise rotation
        int newX = (int) Math.round(x * Math.cos(radians) - (-z) * Math.sin(radians));
        int newZ = (int) Math.round(x * Math.sin(radians) + (-z) * Math.cos(radians));

        return new BlockLocation(newX, y, newZ);

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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BlockLocation blockLocation && blockLocation.x == x && blockLocation.y == y && blockLocation.z == z;
    }

    @Override
    @Utility
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("x", this.x);
        data.put("y", this.y);
        data.put("z", this.z);

        return data;
}

    /**
     * Required method for deserialization
     *
     * @param args map to deserialize
     * @return deserialized location
     * @throws IllegalArgumentException if the world don't exists
     * @see ConfigurationSerializable
     */
    @NotNull
    public static BlockLocation deserialize(@NotNull Map<String, Object> args) {
        return new BlockLocation((int) args.get("x"), (int) args.get("y"), (int) args.get("z"));
    }
}
