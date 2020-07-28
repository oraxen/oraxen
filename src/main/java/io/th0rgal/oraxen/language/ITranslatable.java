package io.th0rgal.oraxen.language;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import io.th0rgal.oraxen.utils.general.IEnum;
import io.th0rgal.oraxen.utils.general.Placeholder;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageParser;
import net.md_5.bungee.api.chat.BaseComponent;

public interface ITranslatable extends IEnum {

    public Plugin getOwner();

    public String id();

    public String value();
    
    public TranslationType type();

    public default String translationId() {
        return getOwner().getName().replace(' ', '_') + '.' + id();
    }

    /*
     * Translate
     */

    public default String translate(Language language) {
        return translate(language.getId());
    }

    public default String translate(Language language, Placeholder... placeholders) {
        return translate(language.getId(), placeholders);
    }

    public String translate(String language);

    public String translate(String language, Placeholder... placeholders);

    /*
     * Convert to message
     */

    public default BaseComponent[] message() {
        return MiniMessageParser.parseFormat(value());
    }

    public default BaseComponent[] message(Language language) {
        return message(language.getId());
    }

    public default BaseComponent[] message(Language language, Placeholder... placeholders) {
        return message(language.getId(), placeholders);
    }

    public default BaseComponent[] message(String language) {
        return MiniMessageParser.parseFormat(translate(language));
    }

    public default BaseComponent[] message(String language, Placeholder... placeholders) {
        return MiniMessageParser.parseFormat(translate(language, placeholders));
    }

    /*
     * Send to CommandSender
     */

    public default void send(CommandSender sender) {
        send(sender, LanguageProvider.getLanguageOf(sender));
    }

    public default void send(CommandSender sender, Language language) {
        sender.spigot().sendMessage(message(language));
    }

    public default void send(CommandSender sender, Placeholder... placeholders) {
        send(sender, LanguageProvider.getLanguageOf(sender));
    }

    public default void send(CommandSender sender, Language language, Placeholder... placeholders) {
        sender.spigot().sendMessage(message(language));
    }

}
