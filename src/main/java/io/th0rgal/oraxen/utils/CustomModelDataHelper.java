package io.th0rgal.oraxen.utils;

import org.bukkit.Color;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Cross-version access to an item's custom model data.
 *
 * <p>Paper 1.21.4 replaced the integer custom model data component with lists of
 * floats, strings, flags, and colors. The plugin still exposes the legacy integer
 * internally, so the integer is represented by the first float on modern Paper.</p>
 */
public final class CustomModelDataHelper {
    private static final ModernApi MODERN_API = VersionUtil.atOrAbove("1.21.4")
            ? ModernApi.create()
            : null;

    private CustomModelDataHelper() {
    }

    public static boolean hasCustomModelData(@NotNull ItemMeta itemMeta) {
        if (MODERN_API == null)
            return itemMeta.hasCustomModelData();

        ComponentData componentData = getComponentData(itemMeta);
        return componentData != null && !componentData.floats().isEmpty();
    }

    public static @Nullable Integer getCustomModelData(@NotNull ItemMeta itemMeta) {
        if (MODERN_API == null)
            return itemMeta.hasCustomModelData() ? itemMeta.getCustomModelData() : null;

        ComponentData componentData = getComponentData(itemMeta);
        if (componentData == null || componentData.floats().isEmpty())
            return null;
        return componentData.floats().get(0).intValue();
    }

    /**
     * Sets or removes the legacy integer value without replacing unrelated modern
     * custom model data fields.
     */
    public static void setCustomModelData(@NotNull ItemMeta itemMeta, @Nullable Integer customModelData) {
        if (MODERN_API == null) {
            itemMeta.setCustomModelData(customModelData);
            return;
        }

        Object component = MODERN_API.getComponent(itemMeta);
        if (component == null)
            return;

        MODERN_API.setFloats(component, customModelData == null
                ? List.of()
                : List.of(customModelData.floatValue()));
        applyComponent(itemMeta, component);
    }

    /**
     * Reads all custom model data fields. The returned data is a snapshot.
     * Returns {@code null} on versions without the modern component API.
     */
    public static @Nullable ComponentData getComponentData(@NotNull ItemMeta itemMeta) {
        if (MODERN_API == null)
            return null;

        Object component = MODERN_API.getComponent(itemMeta);
        return component == null ? null : MODERN_API.read(component);
    }

    /**
     * Updates the supplied modern fields while retaining fields represented by
     * {@code null}, as well as the component's flags and colors.
     */
    public static void setComponentData(@NotNull ItemMeta itemMeta,
                                         @Nullable List<String> strings,
                                         @Nullable List<Float> floats) {
        if (MODERN_API == null || (strings == null && floats == null))
            return;

        Object component = MODERN_API.getComponent(itemMeta);
        if (component == null)
            return;

        if (strings != null)
            MODERN_API.setStrings(component, strings);
        if (floats != null)
            MODERN_API.setFloats(component, floats);
        applyComponent(itemMeta, component);
    }

    private static void applyComponent(ItemMeta itemMeta, Object component) {
        ComponentData data = MODERN_API.read(component);
        MODERN_API.setComponent(itemMeta, data.isEmpty() ? null : component);
    }

    public record ComponentData(List<String> strings, List<Float> floats,
                                List<Boolean> flags, List<Color> colors) {
        public ComponentData {
            strings = List.copyOf(strings);
            floats = List.copyOf(floats);
            flags = List.copyOf(flags);
            colors = List.copyOf(colors);
        }

        private boolean isEmpty() {
            return strings.isEmpty() && floats.isEmpty() && flags.isEmpty() && colors.isEmpty();
        }
    }

    private static final class ModernApi {
        private final Method getComponent;
        private final Method setComponent;
        private final Method setComponentFloats;
        private final Method getComponentFloats;
        private final Method setComponentStrings;
        private final Method getComponentStrings;
        private final Method setComponentFlags;
        private final Method getComponentFlags;
        private final Method setComponentColors;
        private final Method getComponentColors;

        private ModernApi(Method getComponent, Method setComponent,
                          Method setComponentFloats, Method getComponentFloats,
                          Method setComponentStrings, Method getComponentStrings,
                          Method setComponentFlags, Method getComponentFlags,
                          Method setComponentColors, Method getComponentColors) {
            this.getComponent = getComponent;
            this.setComponent = setComponent;
            this.setComponentFloats = setComponentFloats;
            this.getComponentFloats = getComponentFloats;
            this.setComponentStrings = setComponentStrings;
            this.getComponentStrings = getComponentStrings;
            this.setComponentFlags = setComponentFlags;
            this.getComponentFlags = getComponentFlags;
            this.setComponentColors = setComponentColors;
            this.getComponentColors = getComponentColors;
        }

        private static @Nullable ModernApi create() {
            try {
                Class<?> componentType = Class.forName(
                        "org.bukkit.inventory.meta.components.CustomModelDataComponent");
                return new ModernApi(
                        ItemMeta.class.getMethod("getCustomModelDataComponent"),
                        ItemMeta.class.getMethod("setCustomModelDataComponent", componentType),
                        componentType.getMethod("setFloats", List.class),
                        componentType.getMethod("getFloats"),
                        componentType.getMethod("setStrings", List.class),
                        componentType.getMethod("getStrings"),
                        componentType.getMethod("setFlags", List.class),
                        componentType.getMethod("getFlags"),
                        componentType.getMethod("setColors", List.class),
                        componentType.getMethod("getColors"));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        private ComponentData read(Object component) {
            return new ComponentData(
                    new ArrayList<>((List<String>) invoke(getComponentStrings, component)),
                    new ArrayList<>((List<Float>) invoke(getComponentFloats, component)),
                    new ArrayList<>((List<Boolean>) invoke(getComponentFlags, component)),
                    new ArrayList<>((List<Color>) invoke(getComponentColors, component)));
        }

        private Object getComponent(ItemMeta itemMeta) {
            return invoke(getComponent, itemMeta);
        }

        private void setComponent(ItemMeta itemMeta, @Nullable Object component) {
            invoke(setComponent, itemMeta, component);
        }

        private void setFloats(Object component, List<Float> floats) {
            invoke(setComponentFloats, component, new ArrayList<>(floats));
        }

        private void setStrings(Object component, List<String> strings) {
            invoke(setComponentStrings, component, new ArrayList<>(strings));
        }

        private Object invoke(Method method, Object target, Object... arguments) {
            try {
                return method.invoke(target, arguments);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Unable to access the custom model data component API", exception);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtimeException)
                    throw runtimeException;
                if (cause instanceof Error error)
                    throw error;
                throw new IllegalStateException("The custom model data component API failed", cause);
            }
        }
    }
}
