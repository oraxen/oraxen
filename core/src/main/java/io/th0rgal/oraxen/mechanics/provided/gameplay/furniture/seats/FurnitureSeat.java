package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.seats;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.joml.Math;

import java.util.*;

public class FurnitureSeat {
    public static final NamespacedKey SEAT_KEY = new NamespacedKey(OraxenPlugin.get(), "seat");

    private final Vector offset;

    @SuppressWarnings({"unsafe", "unchecked"})
    public static FurnitureSeat getSeat(Object offset) {
        return switch (offset) {
            case Map<?, ?> seatMap -> new FurnitureSeat((Map<String, Object>) seatMap);
            case Vector seatVector -> new FurnitureSeat(seatVector);
            case String seatString -> new FurnitureSeat(seatString);
            case Double seatString -> new FurnitureSeat(seatString.toString());
            case Integer seatString -> new FurnitureSeat(seatString.toString());
            case null, default -> null;
        };
    }

    public FurnitureSeat(Vector offset) {
        this.offset = offset;
    }

    public FurnitureSeat(Map<String, Object> offset) {
        this.offset = Vector.deserialize(offset);
    }

    public FurnitureSeat(String offset) {
        this.offset = VectorUtils.getVectorFromString(offset, 0);
    }

    public Vector offset() {
        return offset;
    }

    /**
     * Offset rotated around the baseEntity's yaw
     *
     * @param yaw Yaw of baseEntity
     * @return Rotated offset vector
     */
    public Vector offset(float yaw) {
        return rotateOffset(yaw);
    }

    public static boolean isSeat(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(SEAT_KEY, DataType.UUID);
    }

    public static void sitOnSeat(ItemDisplay baseEntity, Player player, Location interactionPoint) {
        updateLegacySeats(baseEntity);
        Location centeredLoc = BlockHelpers.toCenterLocation(interactionPoint);
        baseEntity.getPersistentDataContainer()
                .getOrDefault(SEAT_KEY, DataType.asList(DataType.UUID), List.of())
                .stream().map(Bukkit::getEntity).filter(e -> e instanceof Interaction && e.getPassengers().isEmpty())
                .min(Comparator.comparingDouble(e -> centeredLoc.distanceSquared(e.getLocation())))
                .ifPresent(seat -> seat.addPassenger(player));
    }

    private Vector rotateOffset(float angle) {
        if (angle < 0) angle += 360;  // Ensure yaw is positive
        double radians = Math.toRadians(angle);

        // Get the coordinates relative to the local y-axis
        double x = offset.getX() * Math.cos(radians) - (-offset.getZ()) * Math.sin(radians);
        double z = offset.getX() * Math.sin(radians) + (-offset.getZ()) * Math.cos(radians);
        double y = offset.getY();

        return new Vector(x, y, z);
    }

    private static void updateLegacySeats(ItemDisplay baseEntity) {
        PersistentDataContainer pdc = baseEntity.getPersistentDataContainer();
        UUID seat = pdc.has(SEAT_KEY, DataType.UUID) ? pdc.get(SEAT_KEY, DataType.UUID) : null;
        if (seat == null) return;
        else if (baseEntity.getPersistentDataContainer().has(SEAT_KEY, DataType.asList(DataType.UUID))) {
            baseEntity.getPersistentDataContainer().remove(SEAT_KEY);
        }
        baseEntity.getPersistentDataContainer().set(SEAT_KEY, DataType.asList(DataType.UUID), List.of(seat));
    }

    public static void spawnSeats(ItemDisplay baseEntity, FurnitureMechanic mechanic) {
        Location location = baseEntity.getLocation();
        float yaw = baseEntity.getLocation().getYaw();
        UUID uuid = baseEntity.getUniqueId();
        List<UUID> seatUUIDs = new ArrayList<>();
        for (FurnitureSeat seat : mechanic.seats()) {
            location.getWorld().spawn(location.clone().add(seat.offset(yaw)), Interaction.class, (Interaction i) -> {
                i.setInteractionHeight(0.1f);
                i.setInteractionWidth(0.1f);
                i.setPersistent(true);
                i.getPersistentDataContainer().set(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING, mechanic.getItemID());
                i.getPersistentDataContainer().set(FurnitureSeat.SEAT_KEY, DataType.UUID, uuid);
                seatUUIDs.add(i.getUniqueId());
            });
        }
        baseEntity.getPersistentDataContainer().set(FurnitureSeat.SEAT_KEY, DataType.asList(DataType.UUID), seatUUIDs);
    }
}
