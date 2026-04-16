package io.th0rgal.oraxen.nms.v1_26_R1;

import java.lang.reflect.Method;

/**
 * Reflection-based helper to handle the ResourceLocation → Identifier rename
 * that occurred in Paper 26.x's Mojang-mapped API.
 *
 * This class is only used by the v1_26_R1 NMS module (Paper 26.x+).
 * The ResourceLocation fallback is retained for defensive resilience only.
 */
public final class ResourceLocationHelper {

    private static final Class<?> RESOURCE_LOCATION_CLASS;
    private static final Method PARSE_METHOD;

    static {
        Class<?> clazz = null;
        Method parseMethod = null;

        // Try Paper 26.x+ Identifier first
        try {
            clazz = Class.forName("net.minecraft.resources.Identifier");
            parseMethod = clazz.getMethod("parse", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Fall back to ResourceLocation for defensive compatibility
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
     * Parses a resource location string (for example {@code minecraft:stone})
     * into the server's runtime identifier type.
     *
     * @param value the resource location string
     * @return the parsed Identifier, or ResourceLocation if the fallback branch is used
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
     * Returns the runtime identifier class used by this server.
     */
    public static Class<?> getResourceLocationClass() {
        return RESOURCE_LOCATION_CLASS;
    }

    /**
     * Returns whether the runtime identifier type is {@code Identifier}.
     */
    public static boolean usesIdentifier() {
        return RESOURCE_LOCATION_CLASS.getSimpleName().equals("Identifier");
    }
}

