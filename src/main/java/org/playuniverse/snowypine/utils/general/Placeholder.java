package org.playuniverse.snowypine.utils.general;

import java.util.function.Function;

import org.playuniverse.snowypine.command.CommandInfo;
import org.playuniverse.snowypine.language.DescriptionType;
import org.playuniverse.snowypine.language.IPlaceable;
import org.playuniverse.snowypine.language.ITranslatable;
import org.playuniverse.snowypine.language.Language;
import org.playuniverse.snowypine.language.LanguageProvider;
import org.playuniverse.snowypine.language.TranslationType;
import org.playuniverse.snowypine.language.Translations;

public class Placeholder {

	public static Placeholder of(IPlaceable placeable) {
		return placeable.getPlaceholder();
	}

	public static Placeholder of(ITranslatable translatable) {
		return new Placeholder(translatable);
	}

	public static Placeholder of(String placeholder, Object value) {
		return new Placeholder(placeholder, value);
	}

	public static Placeholder of(String placeholder, ITranslatable translatable) {
		return new Placeholder(placeholder, translatable);
	}

	public static Placeholder of(String placeholder, CommandInfo info, DescriptionType type) {
		return new Placeholder(placeholder, TranslationType.DESCRIPTION, type.getId(info), type.getMessage(info));
	}

	public static Placeholder of(boolean addition, ITranslatable translatable) {
		return new Placeholder(addition, translatable);
	}

	public static Placeholder of(boolean addition, String placeholder, Object value) {
		return new Placeholder(addition, placeholder, value);
	}

	public static Placeholder of(boolean addition, String placeholder, ITranslatable translatable) {
		return new Placeholder(addition, placeholder, translatable);
	}

	public static Placeholder of(boolean addition, String placeholder, CommandInfo info, DescriptionType type) {
		return new Placeholder(addition, placeholder, TranslationType.DESCRIPTION, type.getId(info), type.getMessage(info));
	}

	/*
	 *
	 */

	private final TranslationType type;
	private final String id;

	private final String placeholder;
	private final String varPlaceholder;
	private final Object value;

	private Function<String, String> modifier;

	private Stringify stringify;

	public Placeholder(String placeholder) {
		this(placeholder, null);
	}

	public Placeholder(String placeholder, Object value) {
		this(placeholder, null, null, value);
	}

	public Placeholder(ITranslatable translatable) {
		this(translatable.id(), translatable.modifier(), translatable.type(), translatable.translationId(), translatable.value());
	}

	public Placeholder(String placeholder, ITranslatable translatable) {
		this(placeholder, translatable.modifier(), translatable.type(), translatable.translationId(), translatable.value());
	}

	public Placeholder(String placeholder, TranslationType type, String id, Object value) {
		this(placeholder, null, type, id, value);
	}

	public Placeholder(String placeholder, Function<String, String> modifier, TranslationType type, String id, Object value) {
		this(true, placeholder, modifier, type, id, value);
	}

	public Placeholder(boolean addition, String placeholder) {
		this(addition, placeholder, null);
	}

	public Placeholder(boolean addition, String placeholder, Object value) {
		this(addition, placeholder, null, null, value);
	}

	public Placeholder(boolean addition, ITranslatable translatable) {
		this(addition, translatable.id(), translatable.modifier(), translatable.type(), translatable.translationId(), translatable.value());
	}

	public Placeholder(boolean addition, String placeholder, ITranslatable translatable) {
		this(addition, placeholder, translatable.modifier(), translatable.type(), translatable.translationId(), translatable.value());
	}

	public Placeholder(boolean addition, String placeholder, TranslationType type, String id, Object value) {
		this(addition, placeholder, null, type, id, value);
	}

	public Placeholder(boolean addition, String placeholder, Function<String, String> modifier, TranslationType type, String id, Object value) {
		this.type = type;
		this.id = id;
		this.placeholder = placeholder;
		this.varPlaceholder = addition ? '$' + placeholder : placeholder;
		this.modifier = modifier;
		this.value = value;
	}

	/*
	 *
	 */

	public Stringify getStringify() {
		return stringify;
	}

	public Placeholder setStringify(Stringify stringify) {
		this.stringify = stringify;
		return this;
	}

	public Function<String, String> getModifier() {
		return modifier;
	}

	public Placeholder setModifier(Function<String, String> modifier) {
		this.modifier = modifier;
		return this;
	}

	/*
	 *
	 */

	public String getPlaceholder() {
		return placeholder;
	}

	public String getVarPlaceholder() {
		return varPlaceholder;
	}

	public Object getValue() {
		return value;
	}

	public String getValueAsString() {
		if (value == null)
			return LanguageProvider.NULL_VALUE;
		return modify(stringify == null ? value.toString() : stringify.asString(value));
	}

	public String getValueAsTranslatedString(String languageId) {
		return modify(type == null ? getValueAsString() : Translations.translate(languageId, id, type));
	}

	public String getValueAsTranslatedString(Language language) {
		return modify(getValueAsTranslatedString(language.getId()));
	}

	private String modify(String value) {
		return modifier == null ? value : modifier.apply(value);
	}

	/*
	 *
	 */

	public String replace(String value) {
		return value.replace(getVarPlaceholder(), getValueAsString());
	}

	public String replaceTranslated(String languageId, String value) {
		return value.replace(getVarPlaceholder(), getValueAsTranslatedString(languageId));
	}

	public String replaceTranslated(Language language, String value) {
		return replaceTranslated(language.getId(), value);
	}

}
