package io.th0rgal.oraxen.commands.subcommands;

import io.th0rgal.oraxen.commands.CommandInterface;
import io.th0rgal.oraxen.pack.dispatch.PackDispatcher;
import io.th0rgal.oraxen.settings.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Pack implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        Player target;
        if (args.length == 3) {

            if (!sender.hasPermission("oraxen.command.pack")) {
                Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.pack");
                return true;
            }

            target = Bukkit.getPlayer(args[2]);

            switch (args[1]) {

                case "sendmenu":
                    PackDispatcher.sendMenu(target);
                    return true;

                case "sendpack":
                    PackDispatcher.sendPack(target);
                    return true;

                default:
                    return false;
            }

        } else {
            target = (Player) sender;

            if (args.length == 2) {
                switch (args[1]) {

                    case "getmenu":
                        PackDispatcher.sendMenu(target);
                        return true;

                    case "getpack":
                        PackDispatcher.sendPack(target);
                        return true;

                    default:
                        return false;
                }
            }

            PackDispatcher.sendMenu(target);
            return true;
        }
    }

}