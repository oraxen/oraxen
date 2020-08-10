package io.th0rgal.oraxen.command.types;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.oraxen.chimerate.commons.command.types.WordType;

import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;

public class OraxenItemType implements WordType<ItemBuilder> {

    public static OraxenItemType TYPE = new OraxenItemType();

    private static final DynamicCommandExceptionType EXCEPTION = new DynamicCommandExceptionType(
            type -> new LiteralMessage("Unknown oraxen item: " + type));

    private OraxenItemType() {
    }

    @Override
    public ItemBuilder parse(StringReader reader) throws CommandSyntaxException {
        String string = reader.readUnquotedString();
        ItemBuilder item = OraxenItems.getItemById(string);
        if (item == null)
            throw EXCEPTION.createWithContext(reader, string);
        return item;
    }

}