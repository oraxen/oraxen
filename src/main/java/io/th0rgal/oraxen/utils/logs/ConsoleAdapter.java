package io.th0rgal.oraxen.utils.logs;

import java.util.function.BiConsumer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

import io.th0rgal.oraxen.language.Variable;

public final class ConsoleAdapter implements BiConsumer<Boolean, String> {
    
    public static final ConsoleAdapter INSTANCE = new ConsoleAdapter();

    private final ConsoleCommandSender sender = Bukkit.getConsoleSender();
    
    private ConsoleAdapter() { }

    /*
     * 
     */

    @Override
    public void accept(Boolean flag, String message) {
        send(message);
    }

    public ConsoleAdapter send(String message) {
        sender.sendMessage(Variable.PREFIX.translate("EN_UK") + ChatColor.translateAlternateColorCodes('&', message));
        return this;
    }

}
