package io.th0rgal.oraxen.commands.subcommands;

import io.th0rgal.oraxen.commands.CommandInterface;
import io.th0rgal.oraxen.settings.Message;
import io.th0rgal.oraxen.utils.itemsvisualizer.AllItemsInventory;
import io.th0rgal.oraxen.utils.itemsvisualizer.FileInventory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InventoryVisualizer implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("oraxen.command.inv")) {
            Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.inv");
            return false;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("oraxen.command.inv.view"))
                if (args.length > 1 && args[1].equalsIgnoreCase("all"))
                    new AllItemsInventory(0).open(player);
                else
                    new FileInventory(0).open(player);
            else
                Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.inv.view");

        } else {
            Message.NOT_A_PLAYER_ERROR.send(sender);
        }

        return false;
    }

}

