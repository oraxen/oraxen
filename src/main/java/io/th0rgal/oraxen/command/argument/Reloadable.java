package io.th0rgal.oraxen.command.argument;

public enum Reloadable {

    ITEMS, PACK, RECIPES, ALL;

    public static Reloadable fromString(String string) {
        try {
            return valueOf(string);
        } catch (IllegalArgumentException ignore) {
            Reloadable[] values = values();
            for (int index = 0; index < values.length; index++)
                if (values[index].name().equalsIgnoreCase(string))
                    return values[index];
        }
        return null;
    }

}
