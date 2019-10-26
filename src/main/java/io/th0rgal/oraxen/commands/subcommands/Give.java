package io.th0rgal.oraxen.commands.subcommands;

import io.th0rgal.oraxen.commands.CommandInterface;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.settings.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Give implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("oraxen.command.give")) {
            Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.give");
            return false;
        }

        if (args.length < 2)
            return false;

        else if (args.length == 2)
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.getInventory().addItem(OraxenItems.getItemById(args[1]).build());
            } else
                Message.NOT_A_PLAYER_ERROR.send(sender);

        else {
            Player target = Bukkit.getPlayer(args[1]);
            ItemStack itemStack = OraxenItems.getItemById(args[2]).build();
            if (args.length == 4)
                itemStack.setAmount(Integer.parseInt(args[3]));
            target.getInventory().addItem(itemStack);
        }

        return true;
    }

}

