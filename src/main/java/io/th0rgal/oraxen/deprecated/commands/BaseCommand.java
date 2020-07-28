package io.th0rgal.oraxen.deprecated.commands;

import io.th0rgal.oraxen.settings.MessageOld;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class BaseCommand implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                             String commandLabel, String[] args) {

        if (!sender.hasPermission("oraxen.command.base")) {
            MessageOld.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.base");
            return false;
        }

        MessageOld.CMD_HELP.send(sender);

        return false;
    }

}
