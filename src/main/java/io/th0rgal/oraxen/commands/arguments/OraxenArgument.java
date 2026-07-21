package io.th0rgal.oraxen.commands.arguments;

import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public abstract class OraxenArgument<T> {

    private final String name;
    private boolean optional;
    private ArgumentSuggestions suggestions;

    protected OraxenArgument(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isOptional() {
        return optional;
    }

    public OraxenArgument<T> setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    public OraxenArgument<T> replaceSuggestions(ArgumentSuggestions suggestions) {
        this.suggestions = suggestions;
        return this;
    }

    public ParseResult<T> parse(CommandSender sender, List<String> input, int index) {
        if (index >= input.size()) return ParseResult.failure();
        return parseValue(sender, input, index);
    }

    protected abstract ParseResult<T> parseValue(CommandSender sender, List<String> input, int index);

    public List<String> suggestions(CommandSender sender) {
        return suggestions == null ? defaultSuggestions(sender) : Arrays.asList(suggestions.suggest(sender));
    }

    protected List<String> defaultSuggestions(CommandSender sender) {
        return List.of();
    }

    public record ParseResult<T>(boolean success, T value, int consumed) {
        public static <T> ParseResult<T> success(T value, int consumed) {
            return new ParseResult<>(true, value, consumed);
        }

        public static <T> ParseResult<T> failure() {
            return new ParseResult<>(false, null, 0);
        }
    }
}
