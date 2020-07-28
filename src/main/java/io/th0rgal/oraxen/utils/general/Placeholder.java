package io.th0rgal.oraxen.utils.general;

import io.th0rgal.oraxen.language.LanguageProvider;

public class Placeholder {

    public static Placeholder of(String placeholder, Object value) {
        return new Placeholder(placeholder, value);
    }

    /*
     * 
     */

    private final String placeholder;
    private Object value;

    private Stringify stringify;

    public Placeholder(String placeholder) {
        this(placeholder, null);
    }

    public Placeholder(String placeholder, Object value) {
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
        return '%' + placeholder + '%';
    }

    public Object getValue() {
        return value;
    }

    public String getValueAsString() {
        if (value == null)
            return LanguageProvider.NULL_VALUE;
        return stringify == null ? value.toString() : stringify.asString(value);
    }

    /*
     * 
     */

    public String replace(String value) {
        return value.replace(getVarPlaceholder(), getValueAsString());
    }

}
