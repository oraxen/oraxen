package io.th0rgal.oraxen.commands.arguments;

import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collection;

public class ArgumentSuggestions {

    private final SuggestionProvider provider;

    private ArgumentSuggestions(SuggestionProvider provider) {
        this.provider = provider;
    }

    public static ArgumentSuggestions strings(String... suggestions) {
        return new ArgumentSuggestions(info -> suggestions);
    }

    public static ArgumentSuggestions strings(Collection<String> suggestions) {
        return new ArgumentSuggestions(info -> suggestions.toArray(String[]::new));
    }

    public static ArgumentSuggestions strings(SuggestionProvider provider) {
        return new ArgumentSuggestions(provider);
    }

    public String[] suggest(CommandSender sender) {
        String[] suggestions = provider.suggest(new SuggestionInfo(sender));
        return suggestions == null ? new String[0] : Arrays.stream(suggestions).filter(s -> s != null).toArray(String[]::new);
    }

    @FunctionalInterface
    public interface SuggestionProvider {
        String[] suggest(SuggestionInfo info);
    }
}
