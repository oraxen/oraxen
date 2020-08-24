package io.th0rgal.oraxen.command.commands;

import static io.th0rgal.oraxen.command.argument.ArgumentHelper.*;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import com.syntaxphoenix.syntaxapi.command.arguments.StringArgument;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.utils.itemsvisualizer.AllItemsInventory;
import io.th0rgal.oraxen.utils.itemsvisualizer.FileInventory;

public class ItemPanel extends OraxenCommand {

    public static final OraxenCommand COMMAND = new ItemPanel();

    public static CommandInfo info() {
        return new CommandInfo("inventory", COMMAND, "inv");
    }

    private ItemPanel() {
    }

    @Override
    public void execute(MinecraftInfo info, Arguments arguments) {
        CommandSender sender = info.getSender();

        if (Conditions
            .mixed(Conditions.reqPerm(OraxenPermission.COMMAND_INVENTORY), Conditions.player(Message.NOT_PLAYER))
            .isFalse(sender)) {
            return;
        }

        Player player = (Player) sender;

        if (get(arguments, 1, ArgumentType.STRING)
            .map(argument -> argument.asString().getValue().equals("all"))
            .orElse(false)) {
            new FileInventory(0).open(player);
        } else {
            new AllItemsInventory(0).open(player);
        }
    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        DefaultCompletion completion = new DefaultCompletion();

        if (Conditions.hasPerm(OraxenPermission.COMMAND_RELOAD).isFalse(info.getSender()))
            return completion;

        if (arguments.count() == 1) {
            completion.add(new StringArgument("all"));
        }

        return completion;
    }

}
