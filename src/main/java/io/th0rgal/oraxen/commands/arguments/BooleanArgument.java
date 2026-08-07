package io.th0rgal.oraxen.commands.arguments;

import org.bukkit.command.CommandSender;

import java.util.List;

public class BooleanArgument extends OraxenArgument<Boolean> {

    public BooleanArgument(String name) {
        super(name);
    }

    @Override
    protected ParseResult<Boolean> parseValue(CommandSender sender, List<String> input, int index) {
        String value = input.get(index).toLowerCase(java.util.Locale.ROOT);
        if (value.equals("true")) return ParseResult.success(true, 1);
        if (value.equals("false")) return ParseResult.success(false, 1);
        return ParseResult.failure();
    }

    @Override
    protected List<String> defaultSuggestions(CommandSender sender) {
        return List.of("true", "false");
    }
}
