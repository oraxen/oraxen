package io.th0rgal.oraxen.commands.subcommands;

import io.th0rgal.oraxen.commands.CommandInterface;
import io.th0rgal.oraxen.settings.Message;
import io.th0rgal.oraxen.utils.Logs;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class InventoryVisualizer implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("oraxen.command.inv")) {
            Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.inv");
            return false;
        }

        if (args.length > 1) return false;

        if (sender instanceof Player) {

            Logs.log(Arrays.toString(args));



        } else {
            Message.NOT_A_PLAYER_ERROR.send(sender);
        }

        return false;
    }

}

