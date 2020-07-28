package io.th0rgal.oraxen.language;

import io.th0rgal.oraxen.utils.general.Placeholder;

public class TranslationHolder extends Placeholder {

    public TranslationHolder(String placeholder, Translation translation) {
        super(placeholder, translation);
    }

    public TranslationHolder(String placeholder, Language language, ITranslatable translatable) {
        super(placeholder, new Translation(language, translatable));
    }

    public TranslationHolder(String placeholder, Language language, ITranslatable translatable,
            Placeholder... placeholders) {
        super(placeholder, new Translation(language, translatable));
    }

    public TranslationHolder(String placeholder, String language, ITranslatable translatable) {
        super(placeholder, new Translation(language, translatable));
    }

    public TranslationHolder(String placeholder, String language, ITranslatable translatable,
            Placeholder... placeholders) {
        super(placeholder, new Translation(language, translatable));
    }

    public static class Translation {

        private ITranslatable translatable;
        private String language;

        private Placeholder[] placeholders;

        public Translation(Language language, ITranslatable translatable) {
            this(language.getId(), translatable);
        }

        public Translation(Language language, ITranslatable translatable, Placeholder... placeholders) {
            this(language.getId(), translatable, placeholders);
        }

        public Translation(String language, ITranslatable translatable) {
            this.translatable = translatable;
            this.language = language;
        }

        public Translation(String language, ITranslatable translatable, Placeholder... placeholders) {
            this.translatable = translatable;
            this.language = language;
            this.placeholders = placeholders;
        }

        public String getLanguage() {
            return language;
        }

        public Translation setLanguage(Language language) {
            this.language = language.getId();
            return this;
        }

        public Translation setLanguage(String language) {
            this.language = language;
            return this;
        }

        public ITranslatable getTranslatable() {
            return translatable;
        }

        public Translation setTranslatable(ITranslatable translatable) {
            this.translatable = translatable;
            return this;
        }

        @Override
        public String toString() {
            return placeholders == null ? Translations.translate(language, translatable.id(), translatable.type())
                    : Translations.translate(language, translatable.id(), translatable.type(), placeholders);
        }

    }

}
