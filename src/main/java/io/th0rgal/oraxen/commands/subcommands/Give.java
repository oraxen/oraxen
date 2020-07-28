package io.th0rgal.oraxen.commands.subcommands;

import io.th0rgal.oraxen.commands.CommandInterface;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.settings.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Give implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("oraxen.command.give")) {
            Message.DONT_HAVE_PERMISSION.send(sender, "oraxen.command.give");
            return false;
        }

        if (args.length < 2 || args.length > 4)
            return false;

        if (args.length == 2) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                addItemToInventory(player.getInventory(), OraxenItems.getItemById(args[1]).build());
                Message.ITEM_GAVE.send(sender, "1", args[1], sender.getName());
                return true;
            }
            Message.NOT_A_PLAYER_ERROR.send(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        ItemStack itemStack = OraxenItems.getItemById(args[2]).build();
        if (args.length == 4)
            try {
                itemStack.setAmount(Integer.parseInt(args[3]));
            }catch (NumberFormatException e){
                Message.NOT_A_NUMBER.send(sender, args[3]);
                return true;
            }
        addItemToInventory(target.getInventory(), itemStack);
        if(args.length == 3)
            Message.ITEM_GAVE.send(sender, "1", args[2], sender.getName());
        else
            Message.ITEM_GAVE.send(sender, args[3], args[2], sender.getName());

        return true;
    }

    private void addItemToInventory(PlayerInventory inventory, ItemStack itemStack) {
        inventory.addItem(itemStack);
    }
}
