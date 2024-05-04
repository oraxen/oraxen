package io.th0rgal.oraxen.items.helpers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ItemPropertiesWrapper {

    private static final Map<String, Method> methodCache = new HashMap<>();

    public static void setProperty(@NotNull Object  object, String propertyName, @NotNull Object propertyValue) {
        setProperty(object, propertyName, propertyValue.getClass(), propertyValue);
    }

    public static void setProperty(@NotNull Object  object, String propertyName, String propertyClass, @Nullable Object propertyValue) throws ClassNotFoundException {
        setProperty(object, propertyName, Class.forName(propertyClass), propertyValue);
    }

    public static void setProperty(@NotNull Object object, String propertyName, Class<?> propertyClass, @Nullable Object propertyValue) {
        if (propertyName == null) return;

        try {
            Method method = methodCache.computeIfAbsent(propertyName, key -> {
                try {
                    return object.getClass().getDeclaredMethod(key, propertyClass);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });

            if (method != null) {
                method.setAccessible(true);
                method.invoke(object, propertyValue);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static Object getProperty(@NotNull Object object, String propertyName) {
        if (propertyName == null) return null;

        try {
            Method method = methodCache.computeIfAbsent(propertyName, key -> {
                try {
                    return object.getClass().getDeclaredMethod(key);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });

            if (method != null) {
                method.setAccessible(true);
                return method.invoke(object);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }
}
