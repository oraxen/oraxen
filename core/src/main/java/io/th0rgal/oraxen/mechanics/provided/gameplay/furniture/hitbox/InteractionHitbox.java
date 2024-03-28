package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox;

import io.th0rgal.oraxen.utils.ParseUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.*;

public class InteractionHitbox {
    private final Vector offset;
    private final float width;
    private final float height;

    public static InteractionHitbox DEFAULT = new InteractionHitbox(new Vector(), 1.0, 1.0);

    public InteractionHitbox(Object hitboxObject) {
        InteractionHitbox hitbox;
        if (hitboxObject instanceof String string) hitbox = new InteractionHitbox(string);
        else hitbox = DEFAULT;

        this.offset = hitbox.offset;
        this.width = hitbox.width;
        this.height = hitbox.height;
    }

    public InteractionHitbox(String hitboxString) {
        List<String> split = new ArrayList<>(List.of(hitboxString.split(" ", 2)));
        // Add vector offset
        if (split.size() == 1) split.add("0,0,0");

        List<Double> offsets = new ArrayList<>(Arrays.stream(split.get(1).split(",", 3)).map(s -> ParseUtils.parseDouble(s, 0.0)).toList());
        while (offsets.size() < 3) offsets.add(0.0);

        this.offset = new Vector(offsets.get(0), offsets.get(1), offsets.get(2));
        this.width = ParseUtils.parseFloat(StringUtils.substringBefore(split.get(0), ","), 1.0f);
        this.height = ParseUtils.parseFloat(StringUtils.substringAfter(split.get(0), ","), 1.0f);
    }

    public InteractionHitbox(Vector offset, double width, double height) {
        this.offset = offset;
        this.width = (float) width;
        this.height = (float) height;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
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

    private Vector rotateOffset(float angle) {
        double angleRad = Math.toRadians(angle);

        // Get the coordinates relative to the local y-axis
        double x = Math.cos(angleRad) * offset.getX() + Math.sin(angleRad) * offset.getZ();
        double y = offset.getY();
        double z = Math.sin(angleRad) * offset.getX() + Math.cos(angleRad) * offset.getZ();

        return new Vector(x, y, z);
    }

    public static class Id {
        private final UUID baseUuid;
        private final IntList entityIds;

        public Id(UUID baseUuid, IntList entityIds) {
            this.baseUuid = baseUuid;
            this.entityIds = entityIds;
        }

        public Id(UUID baseUuid, Collection<Integer> entityIds) {
            this.baseUuid = baseUuid;
            IntList intList = IntList.of();
            intList.addAll(entityIds);
            this.entityIds = intList;
        }

        public UUID baseUUID() {
            return baseUuid;
        }

        public Entity baseEntity() {
            return Bukkit.getEntity(baseUuid);
        }

        public IntList entityIds() {
            return entityIds;
        }

    }
}
