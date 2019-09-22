package io.th0rgal.oraxen.commands.subcommands;

import io.th0rgal.oraxen.commands.CommandInterface;
import io.th0rgal.oraxen.items.Item;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.NMS;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;

public class Debug implements CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;
            ItemStack item = player.getInventory().getItemInMainHand();
            Object nmsItem = ItemUtils.getNMSCopy(item);
            Object itemTag = ItemUtils.getNBTTagCompound(nmsItem);
            Object nbtBase = ItemUtils.getNBTBase(itemTag, "Durability");
            try {
                player.sendMessage("durability:" + NMS.NBT_TAG_INT.toClass().getMethod("asInt").invoke(nbtBase));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

}

