package io.th0rgal.oraxen.command.argument;

import java.util.Optional;

import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.BaseArgument;

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

    public static Optional<RecipeType> fromArgument(BaseArgument argument) {
        return Optional.ofNullable(argument.getType() != ArgumentType.STRING ? null : fromString(argument.asString().getValue()));
    }
    
}
