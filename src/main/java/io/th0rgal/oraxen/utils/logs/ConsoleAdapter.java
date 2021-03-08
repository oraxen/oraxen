package io.th0rgal.oraxen.utils.logs;

import java.util.function.BiConsumer;

import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import io.th0rgal.oraxen.language.LanguageProvider;
import io.th0rgal.oraxen.language.Variable;

public final class ConsoleAdapter implements BiConsumer<Boolean, String> {

    public static final ConsoleAdapter INSTANCE = new ConsoleAdapter();

    private final ConsoleCommandSender sender = Bukkit.getConsoleSender();

    private ConsoleAdapter() {
    }

    /*
     * 
     */

    @Override
    public void accept(Boolean flag, String message) {
        send(message);
    }

    public ConsoleAdapter send(String message) {
        sender
            .sendMessage(
                Variable.PREFIX.legacyMessage(LanguageProvider.DEFAULT_LANGUAGE) + ' ' + Utils.handleColors(message));
        return this;
    }

}
