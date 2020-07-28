package io.th0rgal.oraxen.language;

import io.th0rgal.oraxen.utils.general.Placeholder;

public interface IVariable extends ITranslatable {
    
    @Override
    public default TranslationType type() {
        return TranslationType.VARIABLE;
    }

    public default String translate(String language) {
        return Translations.translate(language, this);
    }

    public default String translate(String language, Placeholder... placeholders) {
        return Translations.translate(language, this, placeholders);
    }

}
