package io.th0rgal.oraxen.command.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.oraxen.chimerate.commons.command.tree.nodes.Argument;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;
import com.oraxen.chimerate.commons.command.types.PlayersType;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.types.OraxenItemType;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.utils.general.Placeholder;

public class Give {

    @SuppressWarnings("unchecked")
    public static CommandInfo build() {
        return new CommandInfo("give", info -> {
            Builder<CommandSender> builder = Literal.of(info.getName()).alias(info.getAliases());
            builder
                .then(Argument
                    .of("players", PlayersType.STRING)
                    .then(Argument.of("item", OraxenItemType.TYPE))
                    .optionally(Argument.of("amount", IntegerArgumentType.integer(1, 2304)))
                    .executes((sender, context) -> {
                        List<Player> players = context.getArgument("players", List.class);
                        ItemBuilder itemBuilder = context.getArgument("item", ItemBuilder.class);
                        int amount = context.getOptionalArgument("amount", int.class, 1);
                        int max = itemBuilder.getMaxStackSize();
                        int slots = max / amount + (max % amount > 0 ? 1 : 0);
                        ItemStack[] items = itemBuilder.buildArray(slots > 36 ? (amount = max * 36) : amount);
                        players.forEach(player -> {
                            player.getInventory().addItem(items);
                        });
                        int size = players.size();
                        if(size == 1)
                            Message.COMMAND_GIVE_PLAYER.send(sender, Placeholder.of("player", players.get(0).getName()), Placeholder.of("amount", amount), Placeholder.of("item", OraxenItems.getIdByItem(itemBuilder)));
                        else
                            Message.COMMAND_GIVE_PLAYERS.send(sender, Placeholder.of("players", size), Placeholder.of("amount", amount), Placeholder.of("item", OraxenItems.getIdByItem(itemBuilder)));
                    }));
            return builder;
        });
    }

}
