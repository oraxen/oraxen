package io.th0rgal.oraxen.command.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.oraxen.chimerate.commons.command.tree.nodes.Argument;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.command.types.SpecificWordType;
import io.th0rgal.oraxen.settings.MessageOld;
import io.th0rgal.oraxen.utils.itemsvisualizer.AllItemsInventory;
import io.th0rgal.oraxen.utils.itemsvisualizer.FileInventory;

public class InventoryVisualizer {

    public static CommandInfo build() {
        return new CommandInfo("inventory", info -> {
            Builder<CommandSender> builder = Literal.of(info.getName()).alias(info.getAliases());

            builder.optionally(Argument.of("all", SpecificWordType.of("all")).executes((sender, context) -> {

                if (!OraxenPermission.COMMAND_INVENTORY.required(sender))
                    return;

                if (!(sender instanceof Player)) {
                    MessageOld.NOT_A_PLAYER_ERROR.send(sender);
                    return;
                }

                Player player = (Player) sender;
                if (context.getOptionalArgument("all", String.class) != null) {
                    new AllItemsInventory(0).open(player);
                } else {
                    new FileInventory(0).open(player);
                }

            }));

            return builder;
        }, "inv");
    }

}
