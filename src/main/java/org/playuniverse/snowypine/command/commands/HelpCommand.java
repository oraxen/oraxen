package org.playuniverse.snowypine.command.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.playuniverse.snowypine.command.Command;
import org.playuniverse.snowypine.command.CommandInfo;
import org.playuniverse.snowypine.command.MinecraftInfo;
import org.playuniverse.snowypine.command.argument.ArgumentHelper;
import org.playuniverse.snowypine.language.Language;
import org.playuniverse.snowypine.language.LanguageProvider;
import org.playuniverse.snowypine.language.Message;
import org.playuniverse.snowypine.utils.general.Placeholder;

import com.syntaxphoenix.syntaxapi.command.ArgumentSuperType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;

public final class HelpCommand extends Command {

	public static final Command COMMAND = new HelpCommand();

	public static CommandInfo info() {
		return new CommandInfo("help", COMMAND, "?").setUsage("{<command> / <page>}").setDescription("Snowypine help command").setDetailedDescription(
			"/snowypine help {<page>} - List all commands with their short description",
			"/snowypine help <Command> {<Page>} - Show a command's detailed description");
	}

	private HelpCommand() {}

	@Override
	public void execute(MinecraftInfo info, Arguments arguments) {
		int page = ArgumentHelper.get(arguments, 1, ArgumentSuperType.NUMBER).map(number -> number.asNumeric().asNumber().intValue()).orElse(1);
		if (page < 1) {
			page = 1;
		}
		int count = info.getProvider().getPageCount();
		if (page > count) {
			page = count;
		}
		CommandSender sender = info.getSender();
		Language language = LanguageProvider.getLanguageOf(sender);
		List<CommandInfo> infos = info.getProvider().getInfos(page);
		String pageHeader = Message.COMMAND_HELP_INFO_PAGE.legacyMessage(language, Placeholder.of("current", page), Placeholder.of("total", count));
		String header = Message.COMMAND_HELP_INFO_HEADER.legacyMessage(language, Placeholder.of("label", "Commands"), Placeholder.of("page", pageHeader));
		sender.sendMessage(header);
		for (CommandInfo command : infos) {
			Message.COMMAND_HELP_INFO_LINE.send(sender, language,
				Placeholder.of("content", Message.COMMAND_HELP_INFO_CONTENT.legacyMessage(language, command.getContentPlaceholders(command.getName()))));
		}
		sender.sendMessage(header);
	}

	@Override
	public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
		return new DefaultCompletion();
	}

}
