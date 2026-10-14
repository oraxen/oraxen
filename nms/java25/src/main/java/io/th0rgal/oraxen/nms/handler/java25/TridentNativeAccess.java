package io.th0rgal.oraxen.nms.handler.java25;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/** Resolves native members whose declaring class or signature varies between server versions. */
final class TridentNativeAccess {
    static final EntityType<?> itemDisplay = resolveItemDisplay();
    private static final Method soundLocation;
    private static final Method soundNamespace;
    private static final Method soundPath;
    private static final Method createSound;
    private static final Method movement;
    private static final Method[] legacyMovement;
    private static final Method yaw;
    private static final Method pitch;

    static {
        try {
            soundLocation = SoundEvent.class.getMethod("location");
            soundNamespace = ResourceLocationHelper.getResourceLocationClass().getMethod("getNamespace");
            soundPath = ResourceLocationHelper.getResourceLocationClass().getMethod("getPath");
            createSound = SoundEvent.class.getMethod("createVariableRangeEvent", ResourceLocationHelper.getResourceLocationClass());
            yaw = rotationAccessor("getYRot", "getyRot");
            pitch = rotationAccessor("getXRot", "getxRot");
            Method accessor = null;
            Method[] legacy = null;
            try {
                accessor = ClientboundAddEntityPacket.class.getMethod("getMovement");
            } catch (NoSuchMethodException exception) {
                legacy = new Method[]{ClientboundAddEntityPacket.class.getMethod("getXa"),
                        ClientboundAddEntityPacket.class.getMethod("getYa"), ClientboundAddEntityPacket.class.getMethod("getZa")};
            }
            movement = accessor;
            legacyMovement = legacy;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private TridentNativeAccess() {}

    private static Method rotationAccessor(String name, String legacyName) throws NoSuchMethodException {
        try {
            return ClientboundMoveEntityPacket.class.getMethod(name);
        } catch (NoSuchMethodException exception) {
            return ClientboundMoveEntityPacket.class.getMethod(legacyName);
        }
    }

    private static EntityType<?> resolveItemDisplay() {
        try {
            Class<?> owner;
            try {
                owner = Class.forName("net.minecraft.world.entity.EntityTypes");
            } catch (ClassNotFoundException exception) {
                owner = EntityType.class;
            }
            return (EntityType<?>) owner.getField("ITEM_DISPLAY").get(null);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    static SoundEvent sound(String key) throws ReflectiveOperationException {
        Object location = ResourceLocationHelper.parse(key);
        return (SoundEvent) createSound.invoke(null, location);
    }

    static String soundPath(SoundEvent sound) throws ReflectiveOperationException {
        Object location = soundLocation.invoke(sound);
        if (!"minecraft".equals(soundNamespace.invoke(location))) return "";
        return (String) soundPath.invoke(location);
    }

    static Vec3 movement(ClientboundAddEntityPacket packet) throws ReflectiveOperationException {
        if (movement != null) return (Vec3) movement.invoke(packet);
        return new Vec3((double) legacyMovement[0].invoke(packet), (double) legacyMovement[1].invoke(packet),
                (double) legacyMovement[2].invoke(packet));
    }

    static float yaw(ClientboundMoveEntityPacket packet) throws ReflectiveOperationException { return rotation(yaw, packet); }
    static float pitch(ClientboundMoveEntityPacket packet) throws ReflectiveOperationException { return rotation(pitch, packet); }

    private static float rotation(Method accessor, Object packet) throws ReflectiveOperationException {
        float value = ((Number) accessor.invoke(packet)).floatValue();
        return accessor.getReturnType() == byte.class ? value * 360.0F / 256.0F : value;
    }
}
