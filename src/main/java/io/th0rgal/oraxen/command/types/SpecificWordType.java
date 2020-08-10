package io.th0rgal.oraxen.command.types;

import java.util.Arrays;
import java.util.List;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.oraxen.chimerate.commons.command.types.WordType;

public class SpecificWordType implements WordType<String> {

    private static final DynamicCommandExceptionType EXCEPTION = new DynamicCommandExceptionType(
            word -> new LiteralMessage("Illegal word: " + word));

    public static SpecificWordType of(String... words) {
        return new SpecificWordType(words);
    }

    private final List<String> words;

    public SpecificWordType(String... words) {
        this.words = Arrays.asList(words);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String string = reader.readUnquotedString().toLowerCase();
        if (words.contains(string))
            throw EXCEPTION.createWithContext(reader, string);
        return string;
    }

}
