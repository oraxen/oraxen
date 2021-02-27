package org.playuniverse.snowypine.language;

import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.playuniverse.snowypine.Snowypine;

import com.syntaxphoenix.syntaxapi.utils.java.tools.Container;

public class LanguageProvider {

	public static final String NULL_VALUE = "<null>";
	public static final Language DEFAULT_LANGUAGE = new Language("default", "Default");

	public static final NamespacedKey LANGUAGE_KEY = new NamespacedKey(Snowypine.getPlugin(), "language");

	private static final Container<Language> CONSOLE = Container.of(DEFAULT_LANGUAGE);

	public static Language getLanguageOf(CommandSender sender) {
		return sender instanceof Player ? getLanguageOf((Player) sender) : DEFAULT_LANGUAGE;
	}

	public static Language getLanguageOf(Player player) {
		PersistentDataContainer container = player.getPersistentDataContainer();
		if (!container.has(LANGUAGE_KEY, Language.LanguageType.INSTANCE))
			return DEFAULT_LANGUAGE;
		return container.get(LANGUAGE_KEY, Language.LanguageType.INSTANCE);
	}

	public static void setLanguageOf(Player player, Language language) {
		player.getPersistentDataContainer().set(LANGUAGE_KEY, Language.LanguageType.INSTANCE, language);
	}

	public static void setLanguageOfIfNotExists(Player player, Language language) {
		PersistentDataContainer container = player.getPersistentDataContainer();
		if (container.has(LANGUAGE_KEY, Language.LanguageType.INSTANCE))
			return;
		container.set(LANGUAGE_KEY, Language.LanguageType.INSTANCE, language);
	}

	public static void updateLanguageOf(Player player) {
		updateLanguageOf(player, player.getLocale());
	}

	public static void updateLanguageOf(Player player, String locale) {
		if (!Translations.MANAGER.hasTranslation(locale)) {
			setLanguageOfIfNotExists(player, DEFAULT_LANGUAGE);
			return;
		}
		setLanguageOfIfNotExists(player, Translations.MANAGER.getLanguage(locale, RequestType.ID));
	}

	public static void setConsoleLanguage(Language language) {
		if (language == null) {
			return;
		}
		CONSOLE.replace(language);
	}

	public static Language getConsoleLanguage() {
		return CONSOLE.get();
	}

}
