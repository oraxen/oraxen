package io.th0rgal.oraxen.command.types;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.oraxen.chimerate.commons.command.types.WordType;

import io.th0rgal.oraxen.command.argument.Reloadable;

public class ReloadableType implements WordType<Reloadable> {

    public static ReloadableType TYPE = new ReloadableType();

    private static final DynamicCommandExceptionType EXCEPTION = new DynamicCommandExceptionType(
            type -> new LiteralMessage("Unknown reloadable type: " + type));

    private ReloadableType() {
    }

    @Override
    public Reloadable parse(StringReader reader) throws CommandSyntaxException {
        String string = reader.readQuotedString();
        Reloadable reloadable = Reloadable.fromString(string);
        if (reloadable == null)
            throw EXCEPTION.createWithContext(reader, string);
        return reloadable;
    }

}