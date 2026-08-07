package io.th0rgal.oraxen.mechanics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents a configuration property for a mechanic.
 * Used for schema generation to provide autocomplete and validation in external
 * tools.
 */
public record MechanicConfigProperty(
        @NotNull String name,
        @NotNull Type type,
        @Nullable String description,
        @Nullable Object defaultValue,
        @Nullable Number min,
        @Nullable Number max,
        @Nullable List<String> enumValues,
        @Nullable String enumRef,
        boolean required,
        @Nullable Map<String, MechanicConfigProperty> nestedProperties) {
    public enum Type {
        STRING, INTEGER, DOUBLE, BOOLEAN, LIST, OBJECT, ENUM
    }

    // Builder for convenient construction
    public static Builder builder(String name, Type type) {
        return new Builder(name, type);
    }

    public static class Builder {
        private final String name;
        private final Type type;
        private String description;
        private Object defaultValue;
        private Number min;
        private Number max;
        private List<String> enumValues;
        private String enumRef;
        private boolean required = false;
        private Map<String, MechanicConfigProperty> nestedProperties;

        private Builder(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder min(Number min) {
            this.min = min;
            return this;
        }

        public Builder max(Number max) {
            this.max = max;
            return this;
        }

        public Builder range(Number min, Number max) {
            this.min = min;
            this.max = max;
            return this;
        }

        public Builder enumValues(List<String> enumValues) {
            this.enumValues = enumValues;
            return this;
        }

        public Builder enumValues(String... enumValues) {
            this.enumValues = List.of(enumValues);
            return this;
        }

        public Builder enumRef(String enumRef) {
            this.enumRef = enumRef;
            return this;
        }

        public Builder required() {
            this.required = true;
            return this;
        }

        public Builder nested(Map<String, MechanicConfigProperty> nestedProperties) {
            this.nestedProperties = nestedProperties;
            return this;
        }

        public MechanicConfigProperty build() {
            return new MechanicConfigProperty(name, type, description, defaultValue, min, max, enumValues, enumRef,
                    required, nestedProperties);
        }
    }

    // Convenience factory methods
    public static MechanicConfigProperty integer(String name, String description) {
        return builder(name, Type.INTEGER).description(description).build();
    }

    public static MechanicConfigProperty integer(String name, String description, int defaultValue) {
        return builder(name, Type.INTEGER).description(description).defaultValue(defaultValue).build();
    }

    public static MechanicConfigProperty integer(String name, String description, int defaultValue, int min) {
        return builder(name, Type.INTEGER).description(description).defaultValue(defaultValue).min(min).build();
    }

    public static MechanicConfigProperty integer(String name, String description, int defaultValue, int min, int max) {
        return builder(name, Type.INTEGER).description(description).defaultValue(defaultValue).range(min, max).build();
    }

    public static MechanicConfigProperty decimal(String name, String description) {
        return builder(name, Type.DOUBLE).description(description).build();
    }

    public static MechanicConfigProperty decimal(String name, String description, double defaultValue) {
        return builder(name, Type.DOUBLE).description(description).defaultValue(defaultValue).build();
    }

    public static MechanicConfigProperty decimal(String name, String description, double defaultValue, double min) {
        return builder(name, Type.DOUBLE).description(description).defaultValue(defaultValue).min(min).build();
    }

    public static MechanicConfigProperty decimal(String name, String description, double defaultValue, double min,
            double max) {
        return builder(name, Type.DOUBLE).description(description).defaultValue(defaultValue).range(min, max).build();
    }

    public static MechanicConfigProperty bool(String name, String description) {
        return builder(name, Type.BOOLEAN).description(description).build();
    }

    public static MechanicConfigProperty bool(String name, String description, boolean defaultValue) {
        return builder(name, Type.BOOLEAN).description(description).defaultValue(defaultValue).build();
    }

    public static MechanicConfigProperty string(String name, String description) {
        return builder(name, Type.STRING).description(description).build();
    }

    public static MechanicConfigProperty string(String name, String description, String defaultValue) {
        return builder(name, Type.STRING).description(description).defaultValue(defaultValue).build();
    }

    public static MechanicConfigProperty enumType(String name, String description, String enumRef) {
        return builder(name, Type.ENUM).description(description).enumRef(enumRef).build();
    }

    public static MechanicConfigProperty enumType(String name, String description, List<String> values) {
        return builder(name, Type.ENUM).description(description).enumValues(values).build();
    }

    public static MechanicConfigProperty list(String name, String description) {
        return builder(name, Type.LIST).description(description).build();
    }

    public static MechanicConfigProperty object(String name, String description,
            Map<String, MechanicConfigProperty> nestedProperties) {
        return builder(name, Type.OBJECT).description(description).nested(nestedProperties).build();
    }
}
