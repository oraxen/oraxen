package io.th0rgal.oraxen.protocol.utils;

import io.th0rgal.oraxen.utils.CacheInvoker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class NMSComponentSerializer implements ProtocolComponentSerializer {

    private static final CacheInvoker INVOKER = CacheInvoker.get();

    private static NMSComponentSerializer INSTANCE = null;

    private final MethodHandle serializer;
    private final MethodHandle deserializer;

    private NMSComponentSerializer(Class<?> componentClass) {
        MethodHandle unreflectedSerializer = null;
        MethodHandle unreflectedDeserializer = null;

        try {
            for (Method method : componentClass.getClasses()[0].getMethods()) {
                if (method.getReturnType() != String.class) {
                    continue;
                }

                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1 || parameterTypes[0] != componentClass) {
                    continue;
                }

                unreflectedSerializer = MethodHandles.lookup().unreflect(method);
                break;
            }

            for (Method method : componentClass.getClasses()[0].getMethods()) {
                if (!componentClass.isAssignableFrom(method.getReturnType())) {
                    continue;
                }

                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1 || parameterTypes[0] != String.class) {
                    continue;
                }

                unreflectedDeserializer = MethodHandles.lookup().unreflect(method);
                break;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        serializer = unreflectedSerializer;
        deserializer = unreflectedDeserializer;
    }

    @Override
    public Object deserialize(String input) {
        try {
            return INVOKER.cache(deserializer).invoke(input);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String serialize(Object component) {
        try {
            return (String) INVOKER.cache(serializer).invoke(component);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public static NMSComponentSerializer get(Class<?> componentClass) {
        if (INSTANCE == null) {
            INSTANCE = new NMSComponentSerializer(componentClass);
        }

        return INSTANCE;
    }
}
