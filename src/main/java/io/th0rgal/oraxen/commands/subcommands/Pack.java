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
        if (args.length == 1)
            return false;

        if (args[1].equalsIgnoreCase("sendmenu") || args[1].equalsIgnoreCase("sendpack")) {

            if (args.length < 3) {
                Message.PLAYER_NOT_GIVEN.send(sender);
                return true;
            }

            if (!sender.hasPermission("oraxen.command.pack")) {
                Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.pack");
                return true;
            }

            target = Bukkit.getPlayer(args[2]);

            if (target == null) {
                Message.PLAYER_NOT_FOUND.send(sender, args[2]);
                return true;
            }

            switch (args[1]) {

                case "sendmenu":
                    PackDispatcher.sendWelcomeMessage(target);
                    return true;

                case "sendpack":
                    PackDispatcher.sendPack(target);
                    return true;

                default:
                    return false;
            }

        } else if (args[1].equalsIgnoreCase("getmenu") || args[1].equalsIgnoreCase("getpack")) {

            if (!(sender instanceof Player)) {
                Message.NOT_A_PLAYER_ERROR.send(sender);
                return true;
            }

            target = (Player) sender;

            if (args.length == 2) {
                switch (args[1]) {

                    case "getmenu":
                        PackDispatcher.sendWelcomeMessage(target);
                        return true;

                    case "getpack":
                        PackDispatcher.sendPack(target);
                        return true;

                    default:
                        return false;
                }
            }

            PackDispatcher.sendWelcomeMessage(target);
            return true;
        }
        return false;
    }


}