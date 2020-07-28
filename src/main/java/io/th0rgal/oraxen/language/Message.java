package io.th0rgal.oraxen.language;

import org.bukkit.plugin.Plugin;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageParser;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public enum Message implements IMessage {

    COMMAND_HELP_INFO_SHORT(""), COMMAND_HELP_INFO_DETAILED(""),
    
    COMMAND_NOT_EXIST(""),;

    private final String value;

    Message(String value) {
        this.value = value;
    }

    Message(String value, boolean legacy) {
        this(legacy ? TextComponent.fromLegacyText(value) : MiniMessageParser.parseFormat(value));
    }

    Message(BaseComponent[] components) {
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
        return OraxenPlugin.get();
    }

}
