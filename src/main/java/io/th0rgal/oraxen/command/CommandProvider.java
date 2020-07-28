package io.th0rgal.oraxen.command;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.oraxen.chimerate.commons.command.dispatcher.Dispatcher;
import com.oraxen.chimerate.commons.command.tree.nodes.Argument;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;

import io.th0rgal.oraxen.OraxenPlugin;

public class CommandProvider {

    public static final InfoProvider INFO_PROVIDER = new InfoProvider();

    /*
     * 
     */

    public static void register() {

        Dispatcher dispatcher = Dispatcher.of(OraxenPlugin.get());

        OraxenCommandEvent event = new OraxenCommandEvent(dispatcher, "oraxen", "orax");
        
        Bukkit.getPluginManager().callEvent(event);

        neuanfangCommand(dispatcher, event.getAliases(), event.getCommandInfos());

    }

    /*
     * 
     */

    @SuppressWarnings("unchecked")
    private static CommandNode<CommandSender>[] neuanfangCommand(Dispatcher dispatcher, List<String> aliases,
            List<CommandInfo> commandInfos) {

        //
        // Create Neuanfang main command

        Builder<CommandSender> oraxenNode = Literal.of("neuanfang");
        oraxenNode.alias("neu");

        //
        // Loop through infos

        for (CommandInfo info : commandInfos) {

            //
            // Add nodes to commands

            oraxenNode.then(info.getNode());

        }

        //
        // Create help command

        Builder<CommandSender> help = Literal.of("help");
        help.alias("?", "hilfe");

        INFO_PROVIDER.addAll(commandInfos);

        help.optionally(Argument.of("page", IntegerArgumentType.integer(1, INFO_PROVIDER.getPageCount()))
                .executes((sender, context) -> {

                    int page = context.getOptionalArgument("page", int.class, 1);

                    List<CommandInfo> infos = INFO_PROVIDER.getInfos(page);

                    for (CommandInfo info : infos) {
                        
                        
                        
                    }

                }));

        help.optionally(Argument.of("command", StringArgumentType.word()).executes((sender, context) -> {

            String command = context.getOptionalArgument("command", String.class, "");

            CommandInfo info = INFO_PROVIDER.getInfo(command.toLowerCase());
            if (info == null) {
                return;
            }

        }));

        //
        // Register help command

        CommandNode<CommandSender> helpNode = dispatcher.register(help);
        oraxenNode.redirect(helpNode);

        //
        // Register neuanfang command and push result

        return new CommandNode[] { helpNode, dispatcher.register(oraxenNode) };

    }

    /*
     * 
     */

}
