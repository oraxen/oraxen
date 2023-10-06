package io.th0rgal.oraxen.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class EntityUtils {
    private static Method spawnMethod;

    static {
        try {
            // Get the method based on the server version
            Class<?> entitySpawnerClass = Class.forName("org.bukkit.RegionAccessor"); // Replace with actual path
            if (VersionUtil.isSupportedVersionOrNewer("1.20.2")) {
                spawnMethod = entitySpawnerClass.getDeclaredMethod("spawn", Location.class, Class.class, java.util.function.Consumer.class);
            } else {
                spawnMethod = entitySpawnerClass.getDeclaredMethod("spawn", Location.class, Class.class, org.bukkit.util.Consumer.class);
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace(); // Handle the exception according to your needs
        }
    }

    /**
     * Spawns an entity at the given location and applies the consumer to it based on server version
     * @param location The location to spawn the entity at
     * @param clazz The class of the entity to spawn
     * @param consumer The consumer to apply to the entity
     * @return The entity that was spawned
     */
    public static <T> T spawnEntity(@NotNull Location location, @NotNull Class<T> clazz, EntityConsumer<T> consumer) {
       try {
            T entity;
            World world = location.getWorld();
            Object wrappedConsumer;

            // Determine the consumer type and choose the appropriate spawn method
            // 1.20.2> uses java.util.function.Consumer while 1.20.2< uses org.bukkit.util.Consumer
            if (VersionUtil.isSupportedVersionOrNewer("1.20.2")) wrappedConsumer = new JavaConsumerWrapper<>(consumer);
            else wrappedConsumer = new BukkitConsumerWrapper<>(consumer);

            entity = (T) spawnMethod.invoke(world, location, clazz, wrappedConsumer);

            return entity;
        } catch (Exception e) {
            e.printStackTrace(); // Handle the exception according to your needs
        }
        return null;
    }



    public interface EntityConsumer<T> {
        void accept(T entity);
    }

    public static class JavaConsumerWrapper<T> implements java.util.function.Consumer<T> {
        private final EntityConsumer<T> entityConsumer;

        public JavaConsumerWrapper(EntityConsumer<T> entityConsumer) {
            this.entityConsumer = entityConsumer;
        }

        @Override
        public void accept(T entity) {
            entityConsumer.accept(entity);
        }
    }

    public static class BukkitConsumerWrapper<T> implements org.bukkit.util.Consumer<T> {
        private final EntityConsumer<T> entityConsumer;

        public BukkitConsumerWrapper(EntityConsumer<T> entityConsumer) {
            this.entityConsumer = entityConsumer;
        }

        @Override
        public void accept(T entity) {
            entityConsumer.accept(entity);
        }
    }

}

