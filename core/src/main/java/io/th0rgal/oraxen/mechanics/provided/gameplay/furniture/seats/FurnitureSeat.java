package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.seats;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;

import java.util.*;

public class FurnitureSeat {
    public static final NamespacedKey SEAT_KEY = new NamespacedKey(OraxenPlugin.get(), "seat");

    private final Vector offset;
    public static FurnitureSeat getSeat(Object offset) {
        if (offset instanceof Map<?,?> seatMap)
            return new FurnitureSeat((Map<String, Object>) seatMap);
        else if (offset instanceof Vector seatVector)
            return new FurnitureSeat(seatVector);
        else if (offset instanceof String seatString)
            return new FurnitureSeat(seatString);
        else if (offset instanceof Double seatString)
            return new FurnitureSeat(seatString.toString());
        else if (offset instanceof Integer seatString)
            return new FurnitureSeat(seatString.toString());
        else return null;
    }

    public FurnitureSeat(Vector offset) {
        this.offset = offset;
    }
    public FurnitureSeat(Map<String, Object> offset) {
        this.offset = Vector.deserialize(offset);
    }
    public FurnitureSeat(String offset) {
        List<Double> split = new ArrayList<>(Arrays.stream(offset.split(" ", 3)).map(s -> {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }).toList());
        while (split.size() < 3) split.add(0.0);

        this.offset = Vector.deserialize(Map.of(
                "x", split.get(0),
                "y", split.get(1),
                "z", split.get(2)
        ));
    }

    public Vector offset() {
        return offset;
    }

    /**
     * Offset rotated around the baseEntity's yaw
     * @param yaw Yaw of baseEntity
     * @return Rotated offset vector
     */
    public Vector offset(float yaw) {
        return rotateOffset(yaw);
    }

    public static void sitOnSeat(Entity baseEntity, Player player, Location interactionPoint) {
        updateLegacySeats(baseEntity);
        Location centeredLoc = BlockHelpers.toCenterLocation(interactionPoint);
        List<Entity> seats = new ArrayList<>(baseEntity.getPersistentDataContainer()
                .getOrDefault(SEAT_KEY, DataType.asList(DataType.UUID), List.of())
                .stream().map(Bukkit::getEntity).filter(e -> e instanceof ArmorStand).toList());
        seats.sort(Comparator.comparingDouble(e ->centeredLoc.distanceSquared(e.getLocation())));
        seats.stream().findFirst().ifPresent(seat -> seat.addPassenger(player));
    }

    private Vector rotateOffset(float angle) {
        double angleRad = Math.toRadians(angle);

        // Get the coordinates relative to the local y-axis
        double x = Math.cos(angleRad) * offset.getX() + Math.sin(angleRad) * offset.getZ();
        double y = offset.getY();
        double z = Math.sin(angleRad) * offset.getX() + Math.cos(angleRad) * offset.getZ();

        return new Vector(x, y, z);
    }

    private static void updateLegacySeats(Entity baseEntity) {
        PersistentDataContainer pdc = baseEntity.getPersistentDataContainer();
        UUID seat = pdc.has(SEAT_KEY, DataType.UUID) ? pdc.get(SEAT_KEY, DataType.UUID) : null;
        if (seat == null) return;
        else if (baseEntity.getPersistentDataContainer().has(SEAT_KEY, DataType.asList(DataType.UUID))) {
            baseEntity.getPersistentDataContainer().remove(SEAT_KEY);
        }
        baseEntity.getPersistentDataContainer().set(SEAT_KEY, DataType.asList(DataType.UUID), List.of(seat));
    }
}
