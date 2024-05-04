package io.th0rgal.oraxen.items.helpers;

import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ItemPropertiesWrapper {

    private static final Map<String, Method> methodCache = new HashMap<>();

    public static void setProperty(ItemMeta itemMeta, String propertyName, @NotNull Object propertyValue) {
        setProperty(itemMeta, propertyName, propertyValue.getClass(), propertyValue);
    }

    public static void setProperty(ItemMeta itemMeta, String propertyName, String propertyClass, @Nullable Object propertyValue) throws ClassNotFoundException {
        setProperty(itemMeta, propertyName, Class.forName(propertyClass), propertyValue);
    }

    public static void setProperty(ItemMeta itemMeta, String propertyName, Class<?> propertyClass, @Nullable Object propertyValue) {
        if (itemMeta == null || propertyName == null) return;

        try {
            Method method = methodCache.computeIfAbsent(propertyName, key -> {
                try {
                    return itemMeta.getClass().getDeclaredMethod(key, propertyClass);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });

            if (method != null) {
                method.setAccessible(true);
                method.invoke(itemMeta, propertyValue);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static Object getProperty(ItemMeta itemMeta, String propertyName) {
        if (itemMeta == null || propertyName == null) return null;

        try {
            Method method = methodCache.computeIfAbsent(propertyName, key -> {
                try {
                    return itemMeta.getClass().getDeclaredMethod(key);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });

            if (method != null) {
                method.setAccessible(true);
                return method.invoke(itemMeta);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }
}
