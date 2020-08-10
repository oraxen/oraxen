package io.th0rgal.oraxen.command.types;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.oraxen.chimerate.commons.command.types.WordType;

import io.th0rgal.oraxen.command.argument.RecipeType;

public class RecipeTypeType implements WordType<RecipeType> {

    public static RecipeTypeType TYPE = new RecipeTypeType();

    private static final DynamicCommandExceptionType EXCEPTION = new DynamicCommandExceptionType(
            type -> new LiteralMessage("Unknown recipe type: " + type));

    private RecipeTypeType() {
    }

    @Override
    public RecipeType parse(StringReader reader) throws CommandSyntaxException {
        String string = reader.readQuotedString();
        RecipeType recipeType = RecipeType.fromString(string);
        if (recipeType == null)
            throw EXCEPTION.createWithContext(reader, string);
        return recipeType;
    }

}