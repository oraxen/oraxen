package io.th0rgal.oraxen.command.commands;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.oraxen.chimerate.commons.command.tree.nodes.Argument;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;
import com.oraxen.chimerate.commons.command.types.PlayersType;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.types.SpecificWordType;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.pack.dispatch.PackDispatcher;

public class Pack {

    @SuppressWarnings("unchecked")
    public static CommandInfo build() {
        return new CommandInfo("pack", info -> {
            Builder<CommandSender> builder = Literal.of(info.getName()).alias(info.getAliases());
            builder
                .then(Argument
                    .of("type", SpecificWordType.of("menu", "pack"))
                    .optionally(Argument.of("players", PlayersType.STRING))
                    .executes((sender, context) -> {
                        Consumer<Player> send = context.getArgument("type", String.class).equals("menu")
                            ? player -> PackDispatcher.sendWelcomeMessage(player)
                            : player -> PackDispatcher.sendPack(player);
                        List<Player> players;
                        if ((players = context.getOptionalArgument("players", List.class)) == null
                            || players.isEmpty()) {
                            if (!Conditions.player(Message.NOT_PLAYER).isTrue(sender))
                                return;
                            send.accept((Player) sender);
                            return;
                        }
                        players.forEach(send);
                    }));
            return builder;
        }, "menu");
    }

}
