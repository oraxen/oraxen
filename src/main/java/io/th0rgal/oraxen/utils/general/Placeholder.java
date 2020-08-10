package io.th0rgal.oraxen.utils.general;

import io.th0rgal.oraxen.language.ITranslatable;
import io.th0rgal.oraxen.language.Language;
import io.th0rgal.oraxen.language.LanguageProvider;
import io.th0rgal.oraxen.language.TranslationType;
import io.th0rgal.oraxen.language.Translations;
import io.th0rgal.oraxen.settings.IPlaceable;

public class Placeholder {

    public static Placeholder of(String placeholder, Object value) {
        return new Placeholder(placeholder, value);
    }

    public static Placeholder of(IPlaceable placeable) {
        return placeable.getPlaceholder();
    }

    public static Placeholder of(ITranslatable translatable) {
        return new Placeholder(translatable.type(), translatable.id(), translatable.value());
    }

    /*
     * 
     */

    private final TranslationType translation;
    private final String placeholder;
    private Object value;

    private Stringify stringify;

    public Placeholder(String placeholder) {
        this(placeholder, null);
    }

    public Placeholder(String placeholder, Object value) {
        this(null, placeholder, value);
    }

    public Placeholder(TranslationType translation, String placeholder, Object value) {
        this.translation = translation;
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

    public String getValueAsTranslatedString(Language language) {
        return translation == null ? getValueAsString() : Translations.translate(language, placeholder, translation);
    }

    /*
     * 
     */

    public String replace(String value) {
        return value.replace(getVarPlaceholder(), getValueAsString());
    }

}
