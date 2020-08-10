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
    NOT_PLAYER("$prefix This can &conly &7be done as a &cplayer&7!"),

    //
    // Command Messages
    //
    COMMAND_NOT_EXIST("$prefix The command '&c$name&7' doesn't exist!"),

    // Help
    COMMAND_HELP_INFO_SHORT(""),
    COMMAND_HELP_INFO_DETAILED(""),

    // Recipe
    COMMAND_RECIPE_NO_BUILDER("$prefix Please &ccreate an recipe&7 first!"),
    COMMAND_RECIPE_NO_FURNACE("$prefix This option is only avaiable for &cFurnace Recipes&7!"),
    COMMAND_RECIPE_NO_NAME("$prefix Please &cspecify a name &7for the recipe!"),
    COMMAND_RECIPE_SAVE("$prefix Recipe '$a$name&7' saved &asuccessfully&7!"),

    // Give
    COMMAND_GIVE_PLAYER("$prefix You gave '&a$player&7' &3$amountx $item&7!"),
    COMMAND_GIVE_PLAYERS("$prefix You gave &a$players players &3$amountx $item&7!"),

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
