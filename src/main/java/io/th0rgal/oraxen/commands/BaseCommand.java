package io.th0rgal.oraxen.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

//This class implements the Command Interface.
public class BaseCommand implements CommandInterface {

    //The command should be automatically created.
    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                             String commandLabel, String[] args) {

        sender.sendMessage("help");

        return false;
    }

}
