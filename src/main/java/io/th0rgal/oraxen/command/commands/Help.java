package io.th0rgal.oraxen.command.commands;

import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.language.Message;
import org.bukkit.command.CommandSender;

public class Help extends OraxenCommand {

    public static final OraxenCommand COMMAND = new Help();

    public static CommandInfo info() {
        return new CommandInfo("help", COMMAND, "?")
                .setUsage("{<command> / <page>}")
                .setDescription("Oraxen help command")
                .setDetailedDescription("/oraxen help {<page>} - List all commands with their short description",
                        "/oraxen help <command> {<page>} - Show a command's detailed description");
    }

    private Help() {
    }

    @Override
    public void execute(MinecraftInfo info, Arguments arguments) {
        CommandSender sender = info.getSender();
        if (Conditions.reqPerm(OraxenPermission.COMMAND_HELP).isFalse(sender))
            return;
        int count = arguments.count();
        Message.WORK_IN_PROGRESS.send(info.getSender());
    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        return new DefaultCompletion();
    }

}
