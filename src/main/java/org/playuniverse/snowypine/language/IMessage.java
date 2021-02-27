package org.playuniverse.snowypine.language;

import org.playuniverse.snowypine.utils.general.Placeholder;

public interface IMessage extends ITranslatable {

    @Override
    public default TranslationType type() {
        return TranslationType.MESSAGE;
    }

    public default String translate(String language) {
        return Translations.translate(language, this);
    }

    public default String translate(String language, Placeholder... placeholders) {
        return Translations.translate(language, this, placeholders);
    }

}
