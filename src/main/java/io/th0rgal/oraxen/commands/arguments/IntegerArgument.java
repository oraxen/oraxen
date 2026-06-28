package io.th0rgal.oraxen.commands.arguments;

import org.bukkit.command.CommandSender;

import java.util.List;

public class IntegerArgument extends OraxenArgument<Integer> {

    public IntegerArgument(String name) {
        super(name);
    }

    @Override
    protected ParseResult<Integer> parseValue(CommandSender sender, List<String> input, int index) {
        try {
            return ParseResult.success(Integer.parseInt(input.get(index)), 1);
        } catch (NumberFormatException e) {
            return ParseResult.failure();
        }
    }
}
