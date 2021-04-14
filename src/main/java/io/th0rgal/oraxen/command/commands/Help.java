package io.th0rgal.oraxen.command.commands;

import com.syntaxphoenix.syntaxapi.command.ArgumentSuperType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.MinecraftInfo;
import io.th0rgal.oraxen.command.OraxenCommand;
import io.th0rgal.oraxen.command.argument.ArgumentHelper;
import io.th0rgal.oraxen.command.condition.Conditions;
import io.th0rgal.oraxen.command.permission.OraxenPermission;
import io.th0rgal.oraxen.language.Language;
import io.th0rgal.oraxen.language.LanguageProvider;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.utils.general.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.List;

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
        int page = ArgumentHelper.get(arguments, 1, ArgumentSuperType.NUMBER).map(number -> number.asNumeric().asNumber().intValue()).orElse(1);
        if (page < 1) {
            page = 1;
        }
        int count = OraxenPlugin.get().getCommandProvider().getPageCount();
        if (page > count) {
            page = count;
        }
        CommandSender sender = info.getSender();
        Language language = LanguageProvider.getLanguageOf(sender);
        List<CommandInfo> infos = OraxenPlugin.get().getCommandProvider().getInfos(page);
        String pageHeader = Message.COMMAND_HELP_INFO_PAGE.legacyMessage(language, Placeholder.of("current", page), Placeholder.of("total", count));
        String header = Message.COMMAND_HELP_INFO_HEADER.legacyMessage(language, Placeholder.of("label", "Commands"), Placeholder.of("page", pageHeader));
        sender.sendMessage(header);
        for (CommandInfo command : infos) {
            Message.COMMAND_HELP_INFO_CONTENT.send(sender, language,
                    Placeholder.of("content", Message.COMMAND_HELP_INFO_PAGE.legacyMessage(language, command.getContentPlaceholders(command.getName()))));
        }
        sender.sendMessage(header);
    }

    @Override
    public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
        return new DefaultCompletion();
    }

}
