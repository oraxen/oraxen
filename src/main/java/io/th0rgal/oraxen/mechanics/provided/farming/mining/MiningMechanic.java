package io.th0rgal.oraxen.mechanics.provided.farming.mining;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class MiningMechanic extends Mechanic {

    private final List<Offset> offsets;
    private final boolean faceRelative;
    private final int radius;
    private final int depth;

    public MiningMechanic(MechanicFactory factory, String itemID, List<?> entries) {
        super(factory, itemID);
        offsets = parseOffsets(itemID, entries);
        faceRelative = false;
        radius = 0;
        depth = 0;
    }

    public MiningMechanic(MechanicFactory factory, ConfigurationSection section) {
        super(factory, section);
        List<?> configuredOffsets = section.getList("offsets");
        if (configuredOffsets != null) {
            offsets = parseOffsets(getItemID(), configuredOffsets);
            faceRelative = false;
            radius = 0;
            depth = 0;
            return;
        }
        if (!section.contains("radius") && !section.contains("depth"))
            throw new IllegalArgumentException("mechanics.mining must be a list of x,y,z offsets for " + getItemID());
        offsets = List.of();
        faceRelative = true;
        radius = Math.max(0, section.getInt("radius"));
        depth = Math.max(0, section.getInt("depth"));
    }

    public List<Offset> getOffsets() {
        return offsets;
    }

    public boolean isFaceRelative() {
        return faceRelative;
    }

    public List<Location> faceTargets(Location origin, BlockFace into) {
        List<Location> targets = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int step = 0; step < depth; step++) {
                    if (x == 0 && y == 0 && step == 0) continue;
                    targets.add(faceRelative(origin, into, x, y, step));
                }
            }
        }
        return targets;
    }

    public static BlockFace lookingDirection(Vector direction) {
        double ax = Math.abs(direction.getX());
        double ay = Math.abs(direction.getY());
        double az = Math.abs(direction.getZ());
        if (ay >= ax && ay >= az) return direction.getY() >= 0 ? BlockFace.UP : BlockFace.DOWN;
        if (ax >= az) return direction.getX() >= 0 ? BlockFace.EAST : BlockFace.WEST;
        return direction.getZ() >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    public static Location faceRelative(Location origin, BlockFace into, int x, int y, int depth) {
        int signed = switch (into) {
            case WEST, DOWN, NORTH -> -depth;
            default -> depth;
        };
        return switch (into) {
            case EAST, WEST -> origin.clone().add(signed, x, y);
            case UP, DOWN -> origin.clone().add(x, signed, y);
            default -> origin.clone().add(x, y, signed);
        };
    }

    private static List<Offset> parseOffsets(String itemID, List<?> entries) {
        List<Offset> parsed = new ArrayList<>();
        for (Object entry : entries) {
            if (!(entry instanceof String coordinates))
                throw new IllegalArgumentException("mining offsets must be strings in x,y,z format for " + itemID);
            String[] parts = coordinates.split(",", -1);
            if (parts.length != 3)
                throw new IllegalArgumentException("Invalid mining offset '" + coordinates + "' for " + itemID);
            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());
                Offset offset = new Offset(x, y, z);
                if (!parsed.contains(offset)) parsed.add(offset);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid mining offset '" + coordinates + "' for " + itemID, exception);
            }
        }
        return List.copyOf(parsed);
    }

    public record Offset(int x, int y, int z) {
    }
}
