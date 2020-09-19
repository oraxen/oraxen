package io.th0rgal.oraxen.utils.general;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.language.DescriptionType;
import io.th0rgal.oraxen.language.ITranslatable;
import io.th0rgal.oraxen.language.Language;
import io.th0rgal.oraxen.language.LanguageProvider;
import io.th0rgal.oraxen.language.TranslationType;
import io.th0rgal.oraxen.language.Translations;
import io.th0rgal.oraxen.settings.IPlaceable;

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

    /*
     *
     */

    private final TranslationType type;
    private final String id;

    private final String placeholder;
    private final Object value;

    private Stringify stringify;

    public Placeholder(String placeholder) {
        this(placeholder, null);
    }

    public Placeholder(String placeholder, Object value) {
        this(placeholder, null, null, value);
    }

    public Placeholder(ITranslatable translatable) {
        this(translatable.id(), translatable.type(), translatable.translationId(), translatable.value());
    }

    public Placeholder(String placeholder, ITranslatable translatable) {
        this(placeholder, translatable.type(), translatable.translationId(), translatable.value()); //lgtm [java/dereferenced-value-may-be-null]
    }

    public Placeholder(String placeholder, TranslationType type, String id, Object value) {
        this.type = type;
        this.id = id;
        this.placeholder = placeholder;
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

    /*
     *
     */

    public String getPlaceholder() {
        return placeholder;
    }

    public String getVarPlaceholder() {
        return '$' + placeholder;
    }

    public Object getValue() {
        return value;
    }

    public String getValueAsString() {
        if (value == null)
            return LanguageProvider.NULL_VALUE;
        return stringify == null ? value.toString() : stringify.asString(value);
    }

    public String getValueAsTranslatedString(String languageId) {
        return type == null ? getValueAsString() : Translations.translate(languageId, id, type);
    }

    public String getValueAsTranslatedString(Language language) {
        return getValueAsTranslatedString(language.getId());
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
