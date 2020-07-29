package io.th0rgal.oraxen.language;

import org.bukkit.plugin.Plugin;

import io.th0rgal.oraxen.Oraxen;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageParser;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public enum Variable implements IVariable {

    //
    // General Variables
    //
    PREFIX("&bOraxen &8| &7", true),

    //
    ;

    private final String value;

    Variable(String value) {
        this.value = value;
    }

    Variable(String value, boolean legacy) {
        this(legacy ? TextComponent.fromLegacyText(value) : MiniMessageParser.parseFormat(value));
    }

    Variable(BaseComponent[] components) {
        this(MiniMessageSerializer.serialize(components));
    }

    /*
     * 
     */

    @Override
    public String id() {
        return id(name());
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Plugin getOwner() {
        return Oraxen.get();
    }

}
