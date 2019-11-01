package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.settings.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class BaseCommand implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                             String commandLabel, String[] args) {

        if (!sender.hasPermission("oraxen.command.base")) {
            Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.base");
            return false;
        }

        Message.CMD_HELP.send(sender);

        return false;
    }

}
