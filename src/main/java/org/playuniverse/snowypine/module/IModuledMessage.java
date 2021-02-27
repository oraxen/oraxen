package org.playuniverse.snowypine.module;

import org.playuniverse.snowypine.language.TranslationType;
import org.playuniverse.snowypine.utils.general.Placeholder;

public interface IModuledMessage extends IModuledTranslatable {

    @Override
    public default TranslationType type() {
        return TranslationType.MESSAGE;
    }

    public default String translate(String language) {
        return ModuledTranslations.translate(language, this);
    }

    public default String translate(String language, Placeholder... placeholders) {
        return ModuledTranslations.translate(language, this, placeholders);
    }

}
