package io.th0rgal.oraxen.command.argument;

import java.util.Optional;

import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.BaseArgument;

public enum Reloadable {

    ITEMS,
    PACK,
    RECIPES,
    MESSAGES,
    ALL;

    public static Reloadable fromString(String string) {
        try {
            return valueOf(string);
        } catch (IllegalArgumentException ignore) {
            Reloadable[] values = values();
            for (Reloadable value : values)
                if (value.name().equalsIgnoreCase(string))
                    return value;
        }
        return null;
    }

    public static Optional<Reloadable> fromArgument(BaseArgument argument) {
        return Optional.ofNullable(argument.getType() != ArgumentType.STRING ? null : fromString(argument.asString().getValue()));
    }

}
