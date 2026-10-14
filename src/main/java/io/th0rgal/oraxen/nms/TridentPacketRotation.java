package io.th0rgal.oraxen.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/** Adapts rotations without depending on the movement format changed in 26.3. */
public final class TridentPacketRotation {
    private static final Map<Class<?>, Field> entityIds = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MoveAccess> moves = new ConcurrentHashMap<>();
    private static final Map<Class<?>, RecordAccess> records = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> removalAccessors = new ConcurrentHashMap<>();

    private TridentPacketRotation() {}

    public static Iterable<Integer> removedEntities(Object packet) throws ReflectiveOperationException {
        Method accessor = removalAccessors.get(packet.getClass());
        if (accessor == null) {
            try {
                accessor = packet.getClass().getMethod("entityIds");
            } catch (NoSuchMethodException exception) {
                accessor = packet.getClass().getMethod("getEntityIds");
            }
            removalAccessors.put(packet.getClass(), accessor);
        }
        return (Iterable<Integer>) accessor.invoke(packet);
    }

    public static int entityId(Object packet) throws ReflectiveOperationException {
        Field field = entityIds.get(packet.getClass());
        if (field == null) {
            Class<?> type = packet.getClass();
            while (type != null) {
                try {
                    field = type.getDeclaredField("entityId");
                    field.setAccessible(true);
                    entityIds.put(packet.getClass(), field);
                    break;
                } catch (NoSuchFieldException exception) {
                    type = type.getSuperclass();
                }
            }
            if (field == null) throw new NoSuchFieldException("entityId");
        }
        return field.getInt(packet);
    }

    public static Object move(Object packet, int id, float yaw, float pitch, boolean onGround)
            throws ReflectiveOperationException {
        MoveAccess access = moves.get(packet.getClass());
        if (access == null) {
            Constructor<?> constructor = Arrays.stream(packet.getClass().getConstructors())
                    .filter(value -> value.getParameterCount() >= 4 && value.getParameterTypes()[0] == int.class)
                    .findFirst().orElseThrow(() -> new NoSuchMethodException("Movement constructor"));
            Method[] getters = switch (constructor.getParameterCount()) {
                case 4 -> new Method[0];
                case 5 -> new Method[]{packet.getClass().getMethod("getPositionDelta")};
                case 7 -> new Method[]{packet.getClass().getMethod("getXa"), packet.getClass().getMethod("getYa"),
                        packet.getClass().getMethod("getZa")};
                default -> throw new NoSuchMethodException("Movement constructor");
            };
            access = new MoveAccess(constructor, getters);
            moves.put(packet.getClass(), access);
        }
        Constructor<?> constructor = access.constructor;
        Class<?>[] types = constructor.getParameterTypes();
        Object yRot = angle(-yaw, types[types.length - 3]);
        Object xRot = angle(Math.clamp(-pitch, -90.0F, 90.0F), types[types.length - 2]);
        if (constructor.getParameterCount() == 4) return constructor.newInstance(id, yRot, xRot, onGround);
        if (constructor.getParameterCount() == 5) {
            Object delta = access.getters[0].invoke(packet);
            return constructor.newInstance(id, delta, yRot, xRot, onGround);
        }
        return constructor.newInstance(id,
                access.getters[0].invoke(packet), access.getters[1].invoke(packet),
                access.getters[2].invoke(packet), yRot, xRot, onGround);
    }

    private static Object angle(float degrees, Class<?> type) throws NoSuchMethodException {
        if (type == float.class) return degrees;
        if (type == byte.class) return (byte) Math.floor(degrees * 256.0F / 360.0F);
        throw new NoSuchMethodException("Unsupported movement angle type: " + type.getName());
    }

    public static Object positionSync(Object packet) throws ReflectiveOperationException {
        RecordAccess access = records.get(packet.getClass());
        if (access == null) {
            RecordComponent[] components = packet.getClass().getRecordComponents();
            Class<?>[] types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
            access = new RecordAccess(packet.getClass().getConstructor(types), components);
            records.put(packet.getClass(), access);
        }
        RecordComponent[] components = access.components;
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            values[i] = components[i].getAccessor().invoke(packet);
            if (components[i].getName().equals("values")) values[i] = positionSync(values[i]);
            if (components[i].getName().equals("yRot")) values[i] = -(float) values[i];
            if (components[i].getName().equals("xRot")) values[i] = Math.clamp(-(float) values[i], -90.0F, 90.0F);
        }
        return access.constructor.newInstance(values);
    }

    private record MoveAccess(Constructor<?> constructor, Method[] getters) {}
    private record RecordAccess(Constructor<?> constructor, RecordComponent[] components) {}
}
