package io.th0rgal.oraxen.settings;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ METHOD })
public @interface Update {
    
    /**
     * Relative path to the configuration file without file extension.
     * 
     * Example:
     * <code>
     * new String[] {"recipes", "shaped"}
     * </code>
     * This will load the <b>shaped.yml</b> of the folder <b>recipes</b>
     * 
     * @return the path
     */
    String[] path();
    
    /**
     * [0]: Only {@code YamlConfiguration} as parameter
     * [1]: {@code YamlConfiguration} and {@code File} as parameter
     * [2]: Only {@code File} as parameter
     * 
     * @return the type
     */
    int type() default 0;
    
    /**
     * Priority in that the updates should be executed.
     * 
     * 0 is lowest and {@code Integer}.MAV_VALUE is the highest.
     * 
     * @return the priority
     */
    int priority() default 0;
    
    /**
     * Return the version of this update
     * 
     * The easiest way to provide a safe version format based on time would be
     * <b>YYYYMMddkkmmss</b>
     * 
     * Please make sure that the format above matches 
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html">DateTimeFormatter</a>
     * for the time
     * 
     * <b>This should be an fixed number and not defined by any code!</b>
     * 
     * @return the version
     */
    long version();
    
    /**
     * Minimum version required to use this update
     * 
     * @return the required version
     */
    long required() default 0;
    
}
