package org.playuniverse.snowypine.command.commands;

import org.bukkit.entity.Player;
import org.playuniverse.snowypine.command.Command;
import org.playuniverse.snowypine.command.CommandInfo;
import org.playuniverse.snowypine.command.MinecraftInfo;
import org.playuniverse.snowypine.command.argument.ArgumentHelper;
import org.playuniverse.snowypine.command.argument.CompletionHelper;
import org.playuniverse.snowypine.command.condition.Conditions;
import org.playuniverse.snowypine.command.permission.SnowypinePermissions;
import org.playuniverse.snowypine.language.Language;
import org.playuniverse.snowypine.language.LanguageProvider;
import org.playuniverse.snowypine.language.Message;
import org.playuniverse.snowypine.language.Translations;
import org.playuniverse.snowypine.utils.general.Placeholder;

import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.Arguments;
import com.syntaxphoenix.syntaxapi.command.DefaultCompletion;

public final class LanguageCommand extends Command {

	public static final Command COMMAND = new LanguageCommand();

	public static CommandInfo info() {
		return new CommandInfo("language", COMMAND, "lang", "sprache").setUsage("{<Language>}").setDescription("Language command")
			.setDetailedDescription("/snowypine language <language> - Change your language", "/snowypine language reload - Reload the language files");
	}

	private LanguageCommand() {}

	@Override
	public void execute(MinecraftInfo info, Arguments arguments) {

		if (arguments.count() == 0) {
			Message.COMMAND_LANGUAGE_USAGE.send(info.getSender());
			return;
		}

		if (Conditions.or(Conditions.player(), Conditions.console()).isFalse(info.getSender())) {
			Message.NOT_PLAYER.send(info.getSender());
			return;
		}

		String rawArg = ArgumentHelper.get(arguments, 1, ArgumentType.STRING).map(argument -> argument.asString().getValue()).orElse(null);

		if (rawArg.equalsIgnoreCase("reload") && Conditions.hasPerm(SnowypinePermissions.COMMAND_LANGUAGE).isTrue(info.getSender())) {
			Message.CONFIG_RELOAD_NEEDED.send(info.getSender(), Placeholder.of("name", "Translation"));
			Translations.MANAGER.reloadCatch();
			Message.CONFIG_RELOAD_DONE.send(info.getSender(), Placeholder.of("name", "Translation"));
			return;
		}

		String[] arg = new String[] {
				rawArg,
				rawArg.toLowerCase(),
				rawArg.toUpperCase()
		};

		Language[] languages = Translations.MANAGER.getLanguages();
		Language choosen = null;
		for (Language language : languages) {
			for (String current : arg) {
				if (language.getId().equalsIgnoreCase(current) || language.getName().equalsIgnoreCase(current)) {
					choosen = language;
				}
			}
		}

		if (choosen == null) {
			Message.COMMAND_LANGUAGE_NON222EXISTENT.send(info.getSender(), Placeholder.of("language", rawArg));
			return;
		}

		// Update Console
		if (Conditions.console().isTrue(info.getSender())) {
			LanguageProvider.setConsoleLanguage(choosen);
			Message.COMMAND_LANGUAGE_CHANGED.send(info.getSender(), Placeholder.of("language", choosen.getName()));
			return;
		}
		LanguageProvider.setLanguageOf((Player) info.getSender(), choosen);
		Message.COMMAND_LANGUAGE_CHANGED.send(info.getSender(), Placeholder.of("language", choosen.getName()));

	}

	@Override
	public DefaultCompletion complete(MinecraftInfo info, Arguments arguments) {
		DefaultCompletion completion = new DefaultCompletion();
		CompletionHelper.completeMulti(completion, language -> new String[] {
				language.getId(),
				language.getName()
		}, Translations.MANAGER.getLanguages());
		return completion;
	}

}
