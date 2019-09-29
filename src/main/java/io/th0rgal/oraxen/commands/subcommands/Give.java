package io.th0rgal.oraxen.commands.subcommands;

import io.th0rgal.oraxen.commands.CommandInterface;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.settings.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Give implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length != 2) return false;

        if (sender instanceof Player) {

            Player player = (Player)sender;
            player.getInventory().addItem(OraxenItems.getItemById(args[1]).getItem());


        } else {
            Message.NOT_A_PLAYER_ERROR.send(sender);
        }

        return true;
    }

}

