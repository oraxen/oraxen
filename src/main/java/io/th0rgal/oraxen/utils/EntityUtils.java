package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

@SuppressWarnings({"unchecked", "unused", "deprecation"})
public class EntityUtils {
    private static Method spawnMethod;

    public static boolean isUnderWater(Entity entity) {
        if (VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.19")) {
            return entity.isUnderWater();
        } else return entity.isInWater();
    }

    public static boolean isFixed(ItemDisplay itemDisplay) {
        return itemDisplay.getItemDisplayTransform() == ItemDisplay.ItemDisplayTransform.FIXED;
    }

    public static boolean isNone(ItemDisplay itemDisplay) {
        return itemDisplay.getItemDisplayTransform() == ItemDisplay.ItemDisplayTransform.NONE;
    }

    public void teleport(@NotNull Location location, @NotNull Entity entity, PlayerTeleportEvent.TeleportCause cause) {
        if (VersionUtil.isPaperServer() || VersionUtil.isFoliaServer() && VersionUtil.atOrAbove("1.19.4")) {
            entity.teleportAsync(location, cause);
        } else entity.teleport(location);
    }

    /**
     * Teleports an entity to the given location
     * Uses teleportAsync on 1.19.4+ Paper/Folia servers and teleport on all other servers
     * @param location The location to teleport the entity to
     * @param entity The entity to teleport
     */
    public static void teleport(@NotNull Location location, @NotNull Entity entity) {
        if (VersionUtil.atOrAbove("1.19.4") && (VersionUtil.isPaperServer() || VersionUtil.isFoliaServer())) {
            entity.teleportAsync(location);
        } else entity.teleport(location);
    }

    static {
        try {
            // Get the method based on the server version
            Class<?> entitySpawnerClass = Class.forName("org.bukkit.RegionAccessor"); // Replace with actual path
            if (VersionUtil.atOrAbove("1.20.2")) {
                spawnMethod = entitySpawnerClass.getDeclaredMethod("spawn", Location.class, Class.class, java.util.function.Consumer.class);
            } else {
                spawnMethod = entitySpawnerClass.getDeclaredMethod("spawn", Location.class, Class.class, org.bukkit.util.Consumer.class);
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Logs.logWarning(e.getMessage()); // Handle the exception according to your needs
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
            if (VersionUtil.atOrAbove("1.20.2")) wrappedConsumer = new JavaConsumerWrapper<>(consumer);
            else wrappedConsumer = new BukkitConsumerWrapper<>(consumer);

            entity = (T) spawnMethod.invoke(world, location, clazz, wrappedConsumer);

            return entity;
        } catch (Exception e) {
           Logs.logWarning(e.getMessage()); // Handle the exception according to your needs
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

