package io.th0rgal.oraxen.command.commands;

import static io.th0rgal.oraxen.command.argument.ArgumentHelper.*;
import static io.th0rgal.oraxen.command.argument.CompletionHelper.*;

import java.util.Optional;

import io.th0rgal.oraxen.command.argument.ArgumentHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.syntaxphoenix.syntaxapi.command.ArgumentSuperType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;
import com.syntaxphoenix.syntaxapi.command.arguments.StringArgument;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.utils.general.Placeholder;

public class Give extends OraxenCommand {

    public static final OraxenCommand COMMAND = new Give();

    public static CommandInfo info() {
        return new CommandInfo("give", COMMAND);
    }

    private Give() {
    }

    @Override
    public void execute(MinecraftInfo info, Arguments arguments) {

        CommandSender sender = info.getSender();

        if (Conditions.reqPerm(OraxenPermission.COMMAND_GIVE).isFalse(sender))
            return;

        Optional<Player[]> option0 = get(arguments, 1, argument -> players(sender, argument));
        Optional<ItemBuilder> option1 = get(arguments, 2, ArgumentHelper::item);
        if (!(option0.isPresent() && option1.isPresent())) {
            info.getInfo().sendSimple(sender, info.getLabel());
            return;
        }
        Player[] players = option0.get();
        ItemBuilder itemBuilder = option1.get();

        int amount = range(get(arguments, 3, ArgumentSuperType.NUMBER).map(argument -> argument.asNumeric().asNumber()),
                1, 2304).map(Number::intValue).orElse(1);
        int max = itemBuilder.getMaxStackSize();
        int slots = amount / max + (max % amount > 0 ? 1 : 0);
        ItemStack[] items = itemBuilder.buildArray(slots > 36 ? (amount = max * 36) : amount);
        for (Player player : players) player.getInventory().addItem(items);
        if (players.length == 1)
            Message.COMMAND_GIVE_PLAYER
                    .send(sender, Placeholder.of("player", players[0].getName()), Placeholder.of("amount", amount),
                            Placeholder.of("item", OraxenItems.getIdByItem(itemBuilder)));
        else
            Message.COMMAND_GIVE_PLAYERS
                    .send(sender, Placeholder.of("players", players.length), Placeholder.of("amount", amount),
                            Placeholder.of("item", OraxenItems.getIdByItem(itemBuilder)));
    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        DefaultCompletion completion = new DefaultCompletion();

        if (Conditions.hasPerm(OraxenPermission.COMMAND_GIVE).isFalse(info.getSender()))
            return completion;

        int count = arguments.count();

        if (count == 1) {
            baseCommand(info, completion);
        } else if (count == 2) {
            completion(completion, OraxenItems.nameArray());
        } else if (count == 3) {
            Optional<ItemBuilder> item = get(arguments, 2, ArgumentHelper::item);
            item.ifPresent(itemBuilder -> completion
                    .add(new StringArgument("{<amount>} | min = 1 / max = " + (itemBuilder.getMaxStackSize() * 36))));
        }
        return completion;
    }

    static void baseCommand(MinecraftInfo info, DefaultCompletion completion) {
        completion(completion,
                Conditions.player().isTrue(info.getSender()) ? (new String[]{"@a", "@r", "@s", "@p"})
                        : (new String[]{"@a", "@r", "@p"}));
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        for (Player player : players) completion.add(new StringArgument(player.getName()));
    }

}
