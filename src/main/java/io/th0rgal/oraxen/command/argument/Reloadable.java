package io.th0rgal.oraxen.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.oraxen.chimerate.commons.command.types.WordType;

public enum Reloadable {

    ITEMS, PACK, RECIPES, ALL;

    public static ReloadableType TYPE = new ReloadableType();

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

    public static class ReloadableType implements WordType<Reloadable> {

        private static final DynamicCommandExceptionType EXCEPTION = new DynamicCommandExceptionType(
                type -> new LiteralMessage("Unknown reloadable type: " + type));

        private ReloadableType() {
        }

        @Override
        public Reloadable parse(StringReader reader) throws CommandSyntaxException {
            String string = reader.readQuotedString();
            Reloadable reloadable = fromString(string);
            if (reloadable == null)
                throw EXCEPTION.createWithContext(reader, string);
            return reloadable;
        }

    }

}
