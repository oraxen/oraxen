package io.th0rgal.oraxen.language;

import org.bukkit.plugin.Plugin;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageParser;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public enum Message implements IMessage {

    //
    // General Messages
    //
    NO_PERMISSION("$prefix You're lacking the permission &c$permission &7to do this!"),
    WORK_IN_PROGRESS("$prefix This feature is &dwork in progress&7!"),
    NOT_PLAYER(""),

    //
    // Command Messages
    //
    COMMAND_NOT_EXIST(""),

    // Help
    COMMAND_HELP_INFO_SHORT(""),
    COMMAND_HELP_INFO_DETAILED(""),
    
    // Recipe
    COMMAND_RECIPE_NO_BUILDER(""),
    COMMAND_RECIPE_NO_FURNACE(""),
    COMMAND_RECIPE_NO_NAME(""),
    COMMAND_RECIPE_SAVE(""),

    //
    ;

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
