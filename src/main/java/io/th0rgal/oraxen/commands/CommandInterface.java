package io.th0rgal.oraxen.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public interface CommandInterface {

    boolean onCommand(CommandSender sender, Command cmd, String label, String[] args);

}
