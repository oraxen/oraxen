package io.th0rgal.oraxen.language;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.ChatColor;
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
    NO_PERMISSION(true, "$prefix &7You're lacking the permission &c$permission &7to do this!"),
    WORK_IN_PROGRESS(true, "$prefix &7This feature is &dwork in progress&7!"),
    NOT_PLAYER(true, "$prefix &7This can &conly &7be done as a &cplayer&7!"),
    COOL_DOWN(true, "&aWait Another $time $unit!"),
    RELOAD(true, "$prefix $reloaded successfully reloaded"),
    PACK_REGENERATED(true, "$prefix resourcepack successfully regenerated"),
    INVALID_NBT_VALUE(true, "$prefix the provided NBT cannot be parsed"),

    //
    // Command Messages
    //
    COMMAND_NOT_EXIST(true, "$prefix &7The command '&c$name&7' doesn't exist!"),

    // Help
    COMMAND_HELP_INFO_PAGE(true, "&8[&b$current &7/ &3$total&8]"),
    COMMAND_HELP_INFO_LINE(true, "&8- &7$content"),
    COMMAND_HELP_INFO_SHORT(true, "$prefix &3/oraxen &b$label $usage &8- &7$description"),
    COMMAND_HELP_INFO_HEADER(true, "$prefix &7Info => &3$label $page"),
    COMMAND_HELP_INFO_DETAILED(true, "$header", "", "$line1", "$line2", "$line3", "$line4", "$line5", "$line6", "",
        "$header"),

    // Recipe
    COMMAND_RECIPE_NO_BUILDER(true, "$prefix &7Please &ccreate an recipe&7 first!"),
    COMMAND_RECIPE_NO_FURNACE(true, "$prefix &7This option is only avaiable for &cFurnace Recipes&7!"),
    COMMAND_RECIPE_NO_NAME(true, "$prefix &7Please &cspecify a name &7for the recipe!"),
    COMMAND_RECIPE_NO_RECIPES(true, "$prefix &7There are no recipes to show!"),
    COMMAND_RECIPE_NO_ITEM(true, "$prefix &7Please specify an item!"),
    COMMAND_RECIPE_SAVE(true, "$prefix &7Recipe '&a$name&7' saved &asuccessfully&7!"),

    // Give
    COMMAND_GIVE_PLAYER(true, "$prefix &7You gave '&a$player&7' &3$amountx $item&7!"),
    COMMAND_GIVE_PLAYERS(true, "$prefix &7You gave &a$players players &3$amountx $item&7!"),

    // Mechanics
    NOT_ENOUGH_EXP(true, "$prefix &aYou need more experience to do this"),

    //
    ;

    private final String value;

    Message(String value) {
        this.value = value;
    }

    Message(boolean legacy, String... values) {
        ArrayList<BaseComponent[]> list = new ArrayList<>();
        BaseComponent[] line = new BaseComponent[] { new TextComponent("\n") };
        int length = values.length - 1;
        for (int index = 0; index < values.length; index++) {
            list
                .add(legacy ? TextComponent.fromLegacyText(values[index].replace('&', ChatColor.COLOR_CHAR))
                    : MiniMessageParser.parseFormat(values[index]));
            if (index != length)
                list.add(line);
        }
        this.value = MiniMessageSerializer
            .serialize(list.stream().flatMap(Arrays::stream).toArray(BaseComponent[]::new));
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
