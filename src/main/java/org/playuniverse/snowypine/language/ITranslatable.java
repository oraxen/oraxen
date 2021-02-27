package org.playuniverse.snowypine.language;

import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.playuniverse.snowypine.utils.general.IEnum;
import org.playuniverse.snowypine.utils.general.Placeholder;

import com.syntaxphoenix.syntaxapi.utils.java.Arrays;

import net.md_5.bungee.api.ChatColor;

public interface ITranslatable extends IEnum {

	public Plugin getOwner();

	public String id();

	public String value();

	public TranslationType type();

	public default String translationId() {
		return getOwner().getName().replace(' ', '_').toLowerCase() + '.' + id();
	}

	/*
	 * Placeholder
	 */

	public default Placeholder placeholder() {
		return Placeholder.of(this);
	}

	public default Function<String, String> modifier() {
		return null;
	}

	/*
	 * Translate
	 */

	public default String translate(Language language) {
		return translate(language.getId());
	}

	public default String translate(Language language, Placeholder... placeholders) {
		return translate(language.getId(), placeholders);
	}

	public String translate(String language);

	public String translate(String language, Placeholder... placeholders);

	/*
	 * Convert to message
	 */

	public default String legacyMessage() {
		return color(value());
	}

	public default String legacyMessage(Language language) {
		return legacyMessage(language.getId());
	}

	public default String legacyMessage(Language language, Placeholder... placeholders) {
		return legacyMessage(language.getId(), placeholders);
	}

	public default String legacyMessage(String language) {
		return color(translate(language));
	}

	public default String legacyMessage(String language, Placeholder... placeholders) {
		return color(translate(language, placeholders));
	}

	default String color(String value) {
		return ChatColor.translateAlternateColorCodes('&', value);
	}

	/*
	 * Send to Console
	 */

	public default void sendConsole() {
		send(Bukkit.getConsoleSender(), LanguageProvider.getConsoleLanguage());
	}

	public default void sendConsole(Placeholder... placeholders) {
		send(Bukkit.getConsoleSender(), LanguageProvider.getConsoleLanguage(), placeholders);
	}

	public default void sendConsole(Placeholder[] placeholders, Placeholder... additional) {
		send(Bukkit.getConsoleSender(), LanguageProvider.getConsoleLanguage(), placeholders, additional);
	}

	public default void sendConsole(Language language, Placeholder... placeholders) {
		send(Bukkit.getConsoleSender(), language, placeholders);
	}

	public default void sendConsole(Language language, Placeholder[] placeholders, Placeholder... additional) {
		send(Bukkit.getConsoleSender(), language, placeholders, additional);
	}

	/*
	 * Send to CommandSender
	 */

	public default void send(CommandSender sender) {
		send(sender, LanguageProvider.getLanguageOf(sender));
	}

	public default void send(CommandSender sender, Language language) {
		sender.sendMessage(legacyMessage(language));
	}

	public default void send(CommandSender sender, Placeholder... placeholders) {
		send(sender, LanguageProvider.getLanguageOf(sender), placeholders);
	}

	public default void send(CommandSender sender, Placeholder[] placeholders, Placeholder... additional) {
		send(sender, LanguageProvider.getLanguageOf(sender), placeholders, additional);
	}

	public default void send(CommandSender sender, Language language, Placeholder[] placeholders, Placeholder... additional) {
		send(sender, language, Arrays.merge(Placeholder[]::new, placeholders, additional));
	}

	public default void send(CommandSender sender, Language language, Placeholder... placeholders) {
		sender.sendMessage(legacyMessage(language, placeholders));
	}

}
