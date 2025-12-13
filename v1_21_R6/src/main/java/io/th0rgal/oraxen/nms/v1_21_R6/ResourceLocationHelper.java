package io.th0rgal.oraxen.nms.v1_21_R6;

import java.lang.reflect.Method;

/**
 * Reflection-based helper to handle the ResourceLocation → Identifier rename
 * between Minecraft 1.21.10 and 1.21.11.
 * 
 * On 1.21.10 and earlier: net.minecraft.resources.ResourceLocation
 * On 1.21.11+: net.minecraft.resources.Identifier
 * 
 * Both classes have the same API (parse(), etc.), just different names.
 */
public final class ResourceLocationHelper {

    private static final Class<?> RESOURCE_LOCATION_CLASS;
    private static final Method PARSE_METHOD;

    static {
        Class<?> clazz = null;
        Method parseMethod = null;

        // Try 1.21.11+ Identifier first
        try {
            clazz = Class.forName("net.minecraft.resources.Identifier");
            parseMethod = clazz.getMethod("parse", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Fall back to ResourceLocation (1.21.10 and earlier)
            try {
                clazz = Class.forName("net.minecraft.resources.ResourceLocation");
                parseMethod = clazz.getMethod("parse", String.class);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                throw new RuntimeException("Could not find ResourceLocation or Identifier class", e);
            }
        }

        RESOURCE_LOCATION_CLASS = clazz;
        PARSE_METHOD = parseMethod;
    }

    private ResourceLocationHelper() {
        // Utility class
    }

    /**
     * Parse a resource location string (e.g., "minecraft:stone") into the
     * appropriate ResourceLocation/Identifier object for the current server version.
     * 
     * @param value the resource location string
     * @return the parsed object (ResourceLocation on 1.21.10, Identifier on 1.21.11)
     */
    @SuppressWarnings("unchecked")
    public static <T> T parse(String value) {
        try {
            return (T) PARSE_METHOD.invoke(null, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse resource location: " + value, e);
        }
    }

    /**
     * Get the ResourceLocation/Identifier class for the current server version.
     * Useful for creating Maps with the correct key type.
     */
    public static Class<?> getResourceLocationClass() {
        return RESOURCE_LOCATION_CLASS;
    }

    /**
     * Check if running on 1.21.11+ (Identifier) or earlier (ResourceLocation).
     */
    public static boolean usesIdentifier() {
        return RESOURCE_LOCATION_CLASS.getSimpleName().equals("Identifier");
    }
}

