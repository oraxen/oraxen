package io.th0rgal.oraxen.nms.handler.shared;

import io.th0rgal.oraxen.utils.logs.Logs;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;

import java.lang.reflect.Field;

final class GhastCollision {

    private static final Field STILL_TIMEOUT = field("serverStillTimeout");
    private static final EntityDataAccessor<Boolean> STAYS_STILL = stillFlag();

    private GhastCollision() {}

    static boolean keepStationary(Entity entity) {
        if (STILL_TIMEOUT == null || STAYS_STILL == null
                || !(((CraftEntity) entity).getHandle() instanceof HappyGhast ghast)) return false;
        try {
            // Vanilla refreshes the timer when a player stands on top, then counts it back down.
            // Keep it active and synchronize the flag that the client uses for collision.
            STILL_TIMEOUT.setInt(ghast, 20);
            ghast.getEntityData().set(STAYS_STILL, true);
            return true;
        } catch (IllegalAccessException exception) {
            Logs.logError("Could not keep a furniture ghast stationary");
            Logs.debug(exception);
            return false;
        }
    }

    private static Field field(String name) {
        try {
            Field field = HappyGhast.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            Logs.logError("Could not access Happy Ghast collision state");
            Logs.debug(exception);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Boolean> stillFlag() {
        Field field = field("STAYS_STILL");
        if (field == null) return null;
        try {
            return (EntityDataAccessor<Boolean>) field.get(null);
        } catch (IllegalAccessException exception) {
            Logs.logError("Could not access the Happy Ghast stationary flag");
            Logs.debug(exception);
            return null;
        }
    }
}
