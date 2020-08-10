package io.th0rgal.oraxen.command.argument;

public enum RecipeType {

    SHAPED, SHAPELESS, FURNACE;

    public static RecipeType fromString(String string) {
        try {
            return valueOf(string);
        } catch (IllegalArgumentException ignore) {
            RecipeType[] values = values();
            for (int index = 0; index < values.length; index++)
                if (values[index].name().equalsIgnoreCase(string))
                    return values[index];
        }
        return null;
    }

}
