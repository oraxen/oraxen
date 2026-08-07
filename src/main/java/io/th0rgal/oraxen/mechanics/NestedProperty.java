package io.th0rgal.oraxen.mechanics;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines a nested property within an OBJECT type ConfigProperty.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface NestedProperty {
    /**
     * Property name
     */
    String name();

    /**
     * Type of the property
     */
    PropertyType type();

    /**
     * Human-readable description
     */
    String description() default "";

    /**
     * Default value as string
     */
    String defaultValue() default "";

    /**
     * Minimum value for numeric types
     */
    double min() default Double.NEGATIVE_INFINITY;

    /**
     * Maximum value for numeric types
     */
    double max() default Double.POSITIVE_INFINITY;

    /**
     * Reference to an enum type
     */
    String enumRef() default "";

    /**
     * Inline enum values
     */
    String[] enumValues() default {};
}

