package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class MechanicFactory {

    private final Map<String, Mechanic> mechanicByItem = new HashMap<>();
    private final String mechanicId;
    private final ConfigurationSection section;

    // Cached schema from annotations
    private List<MechanicConfigProperty> cachedSchema;

    protected MechanicFactory(ConfigurationSection section) {
        this.section = section;
        this.mechanicId = section.getName();
    }

    protected MechanicFactory(String mechanicId) {
        this.mechanicId = mechanicId;
        this.section = null;
    }

    protected ConfigurationSection getSection() {
        return this.section;
    }

    public abstract Mechanic parse(ConfigurationSection itemMechanicConfiguration);

    /**
     * Returns the configuration schema for this mechanic.
     * By default, scans for @ConfigProperty annotations on this class.
     * Override to provide custom schema or additional properties.
     *
     * @return List of config properties this mechanic accepts
     */
    @NotNull
    public List<MechanicConfigProperty> getConfigSchema() {
        if (cachedSchema == null) {
            cachedSchema = scanConfigProperties();
        }
        return cachedSchema;
    }

    /**
     * Returns the category of this mechanic (e.g., "combat", "farming",
     * "cosmetic").
     * By default, reads from @MechanicInfo annotation.
     *
     * @return The mechanic category, or null if not categorized
     */
    @Nullable
    public String getMechanicCategory() {
        MechanicInfo info = getClass().getAnnotation(MechanicInfo.class);
        return info != null ? info.category() : null;
    }

    /**
     * Returns a description of what this mechanic does.
     * By default, reads from @MechanicInfo annotation.
     *
     * @return Description of the mechanic, or null if none
     */
    @Nullable
    public String getMechanicDescription() {
        MechanicInfo info = getClass().getAnnotation(MechanicInfo.class);
        return info != null ? info.description() : null;
    }

    /**
     * Scans this class for @ConfigProperty annotations and builds the schema.
     */
    private List<MechanicConfigProperty> scanConfigProperties() {
        List<MechanicConfigProperty> properties = new ArrayList<>();
        Class<?> clazz = getClass();

        // Scan class-level @ConfigProperty annotations
        ConfigProperty[] classProps = clazz.getAnnotationsByType(ConfigProperty.class);
        for (ConfigProperty prop : classProps) {
            if (!prop.name().isEmpty()) {
                properties.add(convertAnnotationToProperty(prop.name(), prop));
            }
        }

        // Scan static fields with @ConfigProperty annotations
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()))
                continue;
            if (field.getType() != String.class)
                continue;

            ConfigProperty prop = field.getAnnotation(ConfigProperty.class);
            if (prop == null)
                continue;

            try {
                field.setAccessible(true);
                String propertyName = (String) field.get(null);
                if (propertyName != null && !propertyName.isEmpty()) {
                    properties.add(convertAnnotationToProperty(propertyName, prop));
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        return properties.isEmpty() ? Collections.emptyList() : properties;
    }

    /**
     * Converts a @ConfigProperty annotation to a MechanicConfigProperty.
     */
    private MechanicConfigProperty convertAnnotationToProperty(String name, ConfigProperty prop) {
        MechanicConfigProperty.Type type = convertType(prop.type());

        MechanicConfigProperty.Builder builder = MechanicConfigProperty.builder(name, type)
                .description(prop.description().isEmpty() ? null : prop.description());

        // Parse default value
        if (!prop.defaultValue().isEmpty()) {
            builder.defaultValue(parseDefaultValue(prop.defaultValue(), prop.type()));
        }

        // Numeric constraints
        if (prop.min() != Double.NEGATIVE_INFINITY) {
            builder.min(prop.min());
        }
        if (prop.max() != Double.POSITIVE_INFINITY) {
            builder.max(prop.max());
        }

        // Enum handling
        if (!prop.enumRef().isEmpty()) {
            builder.enumRef(prop.enumRef());
        }
        if (prop.enumValues().length > 0) {
            builder.enumValues(Arrays.asList(prop.enumValues()));
        }

        if (prop.required()) {
            builder.required();
        }

        // Nested properties for OBJECT type
        if (prop.nested().length > 0) {
            Map<String, MechanicConfigProperty> nested = new LinkedHashMap<>();
            for (NestedProperty np : prop.nested()) {
                nested.put(np.name(), convertNestedProperty(np));
            }
            builder.nested(nested);
        }

        return builder.build();
    }

    /**
     * Converts a @NestedProperty annotation to a MechanicConfigProperty.
     */
    private MechanicConfigProperty convertNestedProperty(NestedProperty np) {
        MechanicConfigProperty.Type type = convertType(np.type());

        MechanicConfigProperty.Builder builder = MechanicConfigProperty.builder(np.name(), type)
                .description(np.description().isEmpty() ? null : np.description());

        if (!np.defaultValue().isEmpty()) {
            builder.defaultValue(parseDefaultValue(np.defaultValue(), np.type()));
        }
        if (np.min() != Double.NEGATIVE_INFINITY) {
            builder.min(np.min());
        }
        if (np.max() != Double.POSITIVE_INFINITY) {
            builder.max(np.max());
        }
        if (!np.enumRef().isEmpty()) {
            builder.enumRef(np.enumRef());
        }
        if (np.enumValues().length > 0) {
            builder.enumValues(Arrays.asList(np.enumValues()));
        }

        return builder.build();
    }

    private MechanicConfigProperty.Type convertType(PropertyType type) {
        return switch (type) {
            case STRING -> MechanicConfigProperty.Type.STRING;
            case INTEGER -> MechanicConfigProperty.Type.INTEGER;
            case DOUBLE -> MechanicConfigProperty.Type.DOUBLE;
            case BOOLEAN -> MechanicConfigProperty.Type.BOOLEAN;
            case LIST -> MechanicConfigProperty.Type.LIST;
            case OBJECT -> MechanicConfigProperty.Type.OBJECT;
            case ENUM -> MechanicConfigProperty.Type.ENUM;
        };
    }

    private Object parseDefaultValue(String value, PropertyType type) {
        try {
            return switch (type) {
                case INTEGER -> Integer.parseInt(value);
                case DOUBLE -> Double.parseDouble(value);
                case BOOLEAN -> Boolean.parseBoolean(value);
                default -> value;
            };
        } catch (NumberFormatException e) {
            // Malformed numeric default in annotation - return string as fallback
            return value;
        }
    }

    protected void addToImplemented(Mechanic mechanic) {
        mechanicByItem.put(mechanic.getItemID(), mechanic);
    }

    public Set<String> getItems() {
        return mechanicByItem.keySet();
    }

    public boolean isNotImplementedIn(String itemID) {
        return !mechanicByItem.containsKey(itemID);
    }

    public boolean isNotImplementedIn(ItemStack itemStack) {
        return !mechanicByItem.containsKey(OraxenItems.getIdByItem(itemStack));
    }

    public Mechanic getMechanic(String itemID) {
        return mechanicByItem.get(itemID);
    }

    public Mechanic getMechanic(ItemStack itemStack) {
        return mechanicByItem.get(OraxenItems.getIdByItem(itemStack));
    }

    public String getMechanicID() {
        return mechanicId;
    }

}
