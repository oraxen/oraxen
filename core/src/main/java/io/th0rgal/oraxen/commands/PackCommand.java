package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.entity.Player;

import java.util.Collection;

public class PackCommand {

    CommandAPICommand getPackCommand() {
        return new CommandAPICommand("pack")
                .withPermission("oraxen.command.pack")
                .withSubcommand(sendPackCommand());

    }

    private CommandAPICommand sendPackCommand() {
        return new CommandAPICommand("send")
                .withPermission("oraxen.command.pack.send")
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("targets"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = (Collection<Player>) args.getOptional("targets").orElse(sender instanceof Player ? sender : null);
                    if (targets != null) for (final Player target : targets)
                        OraxenPlugin.get().packServer().sendPack(target);
                });
    }
}
