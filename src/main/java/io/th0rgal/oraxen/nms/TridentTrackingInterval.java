package io.th0rgal.oraxen.nms;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Changes only an individual projectile's movement update cadence. */
public final class TridentTrackingInterval {
    private static final Map<Class<?>, Access> accessors = new ConcurrentHashMap<>();

    private TridentTrackingInterval() {}

    public static void everyTick(Object serverEntity) throws ReflectiveOperationException {
        Class<?> type = serverEntity.getClass();
        Access access = accessors.get(type);
        if (access == null) {
            Field interval = type.getDeclaredField("updateInterval");
            interval.setAccessible(true);
            // 26.3 replaces the integer interval with UpdateInterval.periodic(int).
            Object everyTick = interval.getType() == int.class ? 1
                    : interval.getType().getMethod("periodic", int.class).invoke(null, 1);
            access = new Access(interval, everyTick);
            accessors.put(type, access);
        }
        access.interval.set(serverEntity, access.everyTick);
    }

    private record Access(Field interval, Object everyTick) {}
}
