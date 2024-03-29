package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.seats;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FurnitureSeat {
    public static final NamespacedKey SEAT_KEY = new NamespacedKey(OraxenPlugin.get(), "seat");

    private final Vector offset;
    @SuppressWarnings("unsafe")
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

    public static boolean isSeat(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(SEAT_KEY, DataType.UUID);
    }

    public static void sitOnSeat(Entity baseEntity, Player player, Location interactionPoint) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;
        IFurniturePacketManager packetManager = FurnitureFactory.instance.furniturePacketManager();
        packetManager.mountSeatPacket(baseEntity, interactionPoint, mechanic, player);
//        updateLegacySeats(baseEntity);
//        Location centeredLoc = BlockHelpers.toCenterLocation(interactionPoint);
//        List<Entity> seats = new ArrayList<>(baseEntity.getPersistentDataContainer()
//                .getOrDefault(SEAT_KEY, DataType.asList(DataType.UUID), List.of())
//                .stream().map(Bukkit::getEntity).filter(e -> e instanceof ArmorStand).toList());
//        seats.sort(Comparator.comparingDouble(e ->centeredLoc.distanceSquared(e.getLocation())));
//        seats.stream().findFirst().ifPresent(seat -> seat.addPassenger(player));
    }

    private Vector rotateOffset(float angle) {
        double angleRad = Math.toRadians(angle);

        // Get the coordinates relative to the local y-axis
        double x = Math.cos(angleRad) * offset.getX() + Math.sin(angleRad) * offset.getZ();
        double y = offset.getY();
        double z = Math.sin(angleRad) * offset.getX() + Math.cos(angleRad) * offset.getZ();

        return new Vector(x, y, z);
    }

    public static void spawnSeats(Entity baseEntity, FurnitureMechanic mechanic) {
//        Location location = baseEntity.getLocation();
//        float yaw = FurnitureHelpers.furnitureYaw(baseEntity);
//        UUID uuid = baseEntity.getUniqueId();
//        List<UUID> seatUUIDs = new ArrayList<>();
//        for (FurnitureSeat seat : mechanic.seats()) {
//            ArmorStand armorStand = EntityUtils.spawnEntity(location.clone().add(seat.offset(yaw)),  ArmorStand.class, (ArmorStand stand) -> {
//                stand.setVisible(false);
//                stand.setRotation(yaw, 0);
//                stand.setInvulnerable(true);
//                stand.setPersistent(true);
//                stand.setAI(false);
//                stand.setCollidable(false);
//                stand.setGravity(false);
//                stand.setSilent(true);
//                stand.setCustomNameVisible(false);
//                stand.setCanPickupItems(false);
//                //TODO Maybe marker works here? Was removed for rotation issues but should be fixed
//                stand.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.ADDING_OR_CHANGING);
//                stand.addEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.ADDING_OR_CHANGING);
//                stand.addEquipmentLock(EquipmentSlot.OFF_HAND, ArmorStand.LockType.ADDING_OR_CHANGING);
//                stand.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.ADDING_OR_CHANGING);
//                stand.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.ADDING_OR_CHANGING);
//                stand.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.ADDING_OR_CHANGING);
//                stand.getPersistentDataContainer().set(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING, mechanic.getItemID());
//                stand.getPersistentDataContainer().set(FurnitureSeat.SEAT_KEY, DataType.UUID, uuid);
//            });
//            if (armorStand != null) seatUUIDs.add(armorStand.getUniqueId());
//        }
//        baseEntity.getPersistentDataContainer().set(FurnitureSeat.SEAT_KEY, DataType.asList(DataType.UUID), seatUUIDs);
    }
}
