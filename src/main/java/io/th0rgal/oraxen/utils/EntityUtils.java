package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.RegionAccessor;
import org.bukkit.UnsafeValues;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class EntityUtils {

    // 1.20.2+ exposes the java.util.function.Consumer spawn overload; older servers only have
    // the org.bukkit.util.Consumer overload, which is gone from the compile-target API.
    private static final boolean MODERN_SPAWN_CONSUMER = VersionUtil.atOrAbove("1.20.2");
    private static final Method NEXT_ENTITY_ID_METHOD = resolveNextEntityIdMethod();

    public static int nextEntityId(@NotNull World world) {
        try {
            if (NEXT_ENTITY_ID_METHOD.getParameterCount() == 0)
                return (int) NEXT_ENTITY_ID_METHOD.invoke(Bukkit.getUnsafe());
            return (int) NEXT_ENTITY_ID_METHOD.invoke(Bukkit.getUnsafe(), world);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to allocate a virtual entity id", e);
        }
    }

    private static Method resolveNextEntityIdMethod() {
        try {
            return UnsafeValues.class.getMethod("nextEntityId", World.class);
        } catch (NoSuchMethodException ignored) {
            try {
                return UnsafeValues.class.getMethod("nextEntityId");
            } catch (NoSuchMethodException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    public static boolean isUnderWater(Entity entity) {
        return entity.isUnderWater();
    }

    public static boolean isNone(ItemDisplay itemDisplay) {
        return itemDisplay.getItemDisplayTransform() == ItemDisplay.ItemDisplayTransform.NONE;
    }

    /**
     * Spawns an entity at the given location and applies the consumer to it.
     * @param location The location to spawn the entity at
     * @param clazz The class of the entity to spawn
     * @param consumer The consumer to apply to the entity
     * @return The entity that was spawned
     */
    public static <T extends Entity> T spawnEntity(@NotNull Location location, @NotNull Class<T> clazz, EntityConsumer<T> consumer) {
        try {
            RegionAccessor regionAccessor = location.getWorld();
            if (MODERN_SPAWN_CONSUMER)
                return regionAccessor.spawn(location, clazz, consumer::accept);
            return LegacySpawner.spawn(regionAccessor, location, clazz, consumer);
        } catch (RuntimeException e) {
            Logs.logWarning(e.getMessage());
        }
        return null;
    }

    public interface EntityConsumer<T> {
        void accept(T entity);
    }

    /**
     * Isolates the pre-1.20.2 {@code org.bukkit.util.Consumer} spawn overload in a class that is
     * only loaded on legacy servers, so newer runtimes never link the deprecated types.
     */
    @SuppressWarnings("deprecation")
    private static final class LegacySpawner {
        private static final Method SPAWN_METHOD = resolveSpawnMethod();

        private LegacySpawner() {
        }

        @Nullable
        private static Method resolveSpawnMethod() {
            try {
                return RegionAccessor.class.getMethod("spawn", Location.class, Class.class, org.bukkit.util.Consumer.class);
            } catch (NoSuchMethodException e) {
                Logs.logWarning(e.getMessage());
                return null;
            }
        }

        @Nullable
        static <T extends Entity> T spawn(RegionAccessor regionAccessor, Location location, Class<T> clazz, EntityConsumer<T> consumer) {
            if (SPAWN_METHOD == null) return null;
            try {
                org.bukkit.util.Consumer<T> legacyConsumer = consumer::accept;
                return clazz.cast(SPAWN_METHOD.invoke(regionAccessor, location, clazz, legacyConsumer));
            } catch (ReflectiveOperationException e) {
                Logs.logWarning(e.getMessage());
                return null;
            }
        }
    }
}
