package io.th0rgal.oraxen.commands.arguments;

import org.bukkit.command.CommandSender;

import java.util.List;

public class StringArgument extends OraxenArgument<String> {

    public StringArgument(String name) {
        super(name);
    }

    @Override
    protected ParseResult<String> parseValue(CommandSender sender, List<String> input, int index) {
        return ParseResult.success(input.get(index), 1);
    }
}
