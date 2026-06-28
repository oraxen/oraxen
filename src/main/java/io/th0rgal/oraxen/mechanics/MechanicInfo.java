package io.th0rgal.oraxen.mechanics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define mechanic metadata for schema generation.
 * Place on MechanicFactory subclasses to specify category and description.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MechanicInfo {
    /**
     * Category of the mechanic (e.g., "combat", "farming", "gameplay", "cosmetic", "misc")
     */
    String category();

    /**
     * Human-readable description of what the mechanic does
     */
    String description();
}

