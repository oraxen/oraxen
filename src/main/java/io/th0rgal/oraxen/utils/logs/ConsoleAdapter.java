package io.th0rgal.oraxen.utils.logs;

import java.util.function.BiConsumer;

import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

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
        sender.sendMessage(Message.PREFIX.toString() + ' ' + message);
        return this;
    }

}
