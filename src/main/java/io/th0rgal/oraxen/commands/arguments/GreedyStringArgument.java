package io.th0rgal.oraxen.commands.arguments;

import org.bukkit.command.CommandSender;

import java.util.List;

public class GreedyStringArgument extends OraxenArgument<String> {

    public GreedyStringArgument(String name) {
        super(name);
    }

    @Override
    protected ParseResult<String> parseValue(CommandSender sender, List<String> input, int index) {
        if (index >= input.size()) return ParseResult.failure();
        return ParseResult.success(String.join(" ", input.subList(index, input.size())), input.size() - index);
    }
}
