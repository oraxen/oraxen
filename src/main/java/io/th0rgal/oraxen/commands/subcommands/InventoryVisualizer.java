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

        //We don't have to check if the args length is equal to one, but you will have to check if it is greater than 1.
        if (args.length > 1) return false;

        if (sender instanceof Player) {

            Logs.log(Arrays.toString(args));



        } else {
            Message.NOT_A_PLAYER_ERROR.send(sender);
        }

        return false;
    }

}

