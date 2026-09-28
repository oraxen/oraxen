package io.th0rgal.oraxen.mechanics;

import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Reads a scalar mechanic setting from the {@link ConfigProperty} that already
 * documents it, so the constructor default and the published schema stay the same value.
 */
public final class ConfigPropertyValues {

    private ConfigPropertyValues() {
    }

    public static boolean bool(Class<?> factoryClass, ConfigurationSection section, String name) {
        String defaultValue = property(factoryClass, name).defaultValue();
        boolean fallback = defaultValue.isEmpty() ? false : Boolean.parseBoolean(defaultValue);
        return section.getBoolean(name, fallback);
    }

    public static int integer(Class<?> factoryClass, ConfigurationSection section, String name) {
        return section.getInt(name, Integer.parseInt(requiredDefault(factoryClass, name)));
    }

    public static double decimal(Class<?> factoryClass, ConfigurationSection section, String name) {
        return section.getDouble(name, Double.parseDouble(requiredDefault(factoryClass, name)));
    }

    public static String text(Class<?> factoryClass, ConfigurationSection section, String name) {
        String defaultValue = property(factoryClass, name).defaultValue();
        return defaultValue.isEmpty() ? section.getString(name) : section.getString(name, defaultValue);
    }

    public static ConfigProperty property(Class<?> factoryClass, String name) {
        for (Class<?> type = factoryClass; type != null && type != Object.class; type = type.getSuperclass()) {
            for (ConfigProperty property : type.getAnnotationsByType(ConfigProperty.class)) {
                if (name.equals(property.name())) return property;
            }
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) continue;
                ConfigProperty property = field.getAnnotation(ConfigProperty.class);
                if (property == null) continue;
                if (!property.name().isEmpty()) {
                    if (name.equals(property.name())) return property;
                    continue;
                }
                try {
                    field.setAccessible(true);
                    if (name.equals(field.get(null))) return property;
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        throw new IllegalArgumentException("No @ConfigProperty named " + name + " on " + factoryClass.getName());
    }

    private static String requiredDefault(Class<?> factoryClass, String name) {
        String defaultValue = property(factoryClass, name).defaultValue();
        if (defaultValue.isEmpty())
            throw new IllegalArgumentException("@ConfigProperty " + name + " on " + factoryClass.getSimpleName() + " has no defaultValue");
        return defaultValue;
    }
}
