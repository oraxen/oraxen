package io.th0rgal.oraxen;

import java.util.function.Function;

public class EnumUtils {

    public static <E extends Enum<E>> E getEnum(final Class<E> enumClass, final String enumName, final E defaultEnum) {
        if (enumName == null) return defaultEnum;
        try {
            return Enum.valueOf(enumClass, enumName);
        } catch (final IllegalArgumentException ex) {
            return defaultEnum;
        }
    }

    public static <E extends Enum<E>> E getEnumOrElse(final Class<E> enumClass, final String enumName, Function<String, ? extends E> supplier) {
        if (enumName == null) return supplier.apply(null);
        try {
            return Enum.valueOf(enumClass, enumName);
        } catch (final IllegalArgumentException ex) {
            return supplier.apply(enumName);
        }
    }
}
