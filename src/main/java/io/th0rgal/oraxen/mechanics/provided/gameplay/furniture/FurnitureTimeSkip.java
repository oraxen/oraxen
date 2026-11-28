package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.world.TimeSkipEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

final class FurnitureTimeSkip {

    private static final Method skipReason;
    private static final Object nightSkip;
    private static final Method defaultClock = findDefaultClock();
    private static final Method eventClock = findEventClock();
    private static final Constructor<TimeSkipEvent> constructor;

    static {
        try {
            // 26.2 moved SkipReason to ClockTimeSkipEvent. 26.3 also requires the world clock.
            skipReason = TimeSkipEvent.class.getMethod("getSkipReason");
            Class<?> reasonType = skipReason.getReturnType();
            nightSkip = reasonType.getField("NIGHT_SKIP").get(null);
            constructor = defaultClock == null
                    ? TimeSkipEvent.class.getConstructor(World.class, reasonType, long.class)
                    : TimeSkipEvent.class.getConstructor(World.class, defaultClock.getReturnType(), reasonType, long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private FurnitureTimeSkip() {
    }

    private static Method findDefaultClock() {
        try {
            return World.class.getMethod("getDefaultClock");
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findEventClock() {
        try {
            return Class.forName("org.bukkit.event.world.ClockTimeSkipEvent").getMethod("getClock");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    static TimeSkipEvent nightSkip(World world, long amount) {
        try {
            return defaultClock == null
                    ? constructor.newInstance(world, nightSkip, amount)
                    : constructor.newInstance(world, defaultClock.invoke(world), nightSkip, amount);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create furniture night skip event", e);
        }
    }

    static boolean isNightSkip(Event event) {
        try {
            return skipReason.invoke(event) == nightSkip;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read furniture time skip reason", e);
        }
    }

    static boolean affectsWorld(Event event, World world) {
        if (eventClock == null || defaultClock == null) return true;
        try {
            Object clock = eventClock.invoke(event);
            return clock != null && clock.equals(defaultClock.invoke(world));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
