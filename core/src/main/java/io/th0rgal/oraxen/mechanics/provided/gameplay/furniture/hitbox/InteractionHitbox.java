package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox;

import io.th0rgal.oraxen.utils.ParseUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        List<Double> offsets = new ArrayList<>(Arrays.stream(split.getFirst().split(",", 3)).map(s -> ParseUtils.parseDouble(s, 0.0)).toList());
        while (offsets.size() < 3) offsets.add(0.0);

        this.offset = new Vector(offsets.get(0), offsets.get(1), offsets.get(2));
        this.width = ParseUtils.parseFloat(StringUtils.substringBefore(split.getLast(), ","), 1.0f);
        this.height = ParseUtils.parseFloat(StringUtils.substringAfter(split.getLast(), ","), 1.0f);
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
        double angleRad = Math.toRadians(yaw);

        // Get the coordinates relative to the local y-axis
        double x = Math.cos(angleRad) * offset.getX() + Math.sin(angleRad) * offset.getZ();
        double y = offset.getY();
        double z = Math.sin(angleRad) * offset.getX() + Math.cos(angleRad) * offset.getZ();

        return new Vector(x, y, z);
    }
}
