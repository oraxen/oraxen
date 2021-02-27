package org.playuniverse.snowypine.module;

import org.playuniverse.snowypine.language.Language;
import org.playuniverse.snowypine.language.Translations;
import org.playuniverse.snowypine.utils.general.Placeholder;

public final class ModuledTranslations {

	private ModuledTranslations() {}

	public static String translate(Language language, IModuledMessage message) {
		return translate(language.getId(), message);
	}

	public static String translate(Language language, IModuledMessage message, Placeholder... placeholders) {
		return translate(language.getId(), message, placeholders);
	}

	public static String translate(Language language, IModuledVariable variable) {
		return translate(language.getId(), variable);
	}

	public static String translate(Language language, IModuledVariable variable, Placeholder... placeholders) {
		return translate(language.getId(), variable, placeholders);
	}

	public static String translate(String language, IModuledMessage message) {
		return Translations.message(language, message.translationId());
	}

	public static String translate(String language, IModuledMessage message, Placeholder... placeholders) {
		return Translations.message(language, message.translationId(), placeholders);
	}

	public static String translate(String language, IModuledVariable variable) {
		return Translations.variable(language, variable.translationId());
	}

	public static String translate(String language, IModuledVariable variable, Placeholder... placeholders) {
		return Translations.variable(language, variable.translationId());
	}
	
}
