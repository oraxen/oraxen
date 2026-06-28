package io.th0rgal.oraxen.mechanics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a configuration property for schema generation.
 * Can be placed on static String fields in MechanicFactory classes,
 * where the field value is the property name.
 * 
 * Example:
 * <pre>
 * {@literal @}ConfigProperty(type = PropertyType.INTEGER, description = "Health restored per hit", min = 1)
 * public static final String PROP_AMOUNT = "amount";
 * </pre>
 * 
 * Or can be placed directly on the factory class for simple properties:
 * <pre>
 * {@literal @}ConfigProperty(name = "amount", type = PropertyType.INTEGER, description = "Health restored", min = 1)
 * public class LifeLeechMechanicFactory extends MechanicFactory { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
@Repeatable(ConfigProperties.class)
public @interface ConfigProperty {
    /**
     * Property name. If empty, the field value will be used (for field annotations).
     */
    String name() default "";

    /**
     * Type of the property
     */
    PropertyType type();

    /**
     * Human-readable description of the property
     */
    String description() default "";

    /**
     * Default value as string (will be parsed according to type)
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
     * Reference to an enum type for ENUM properties (e.g., "Particle", "Material")
     */
    String enumRef() default "";

    /**
     * Inline enum values for ENUM properties with fixed choices
     */
    String[] enumValues() default {};

    /**
     * Whether this property is required
     */
    boolean required() default false;

    /**
     * Nested properties for OBJECT type (defined as key=value pairs)
     * Format: "propertyName:TYPE:description" or use @NestedProperty annotations
     */
    NestedProperty[] nested() default {};
}

