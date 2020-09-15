package io.th0rgal.oraxen.language;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageParser;
import io.th0rgal.oraxen.utils.minimessage.MiniMessageSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public enum Variable implements IVariable {

    //
    // General Variables
    //
    PREFIX(true, io.th0rgal.oraxen.settings.Plugin.PREFIX.toString()),

    //Time Units
    TIME_UNIT_NANOSECONDS("NanoSeconds"),
    TIME_UNIT_MICROSECONDS("MicroSeconds"),
    TIME_UNIT_MILLISECONDS("MilliSeconds"),
    TIME_UNIT_SECONDS("Seconds"),
    TIME_UNIT_MINUTES("Minutes"),
    TIME_UNIT_HOURS("Hours"),
    TIME_UNIT_DAYS("Days")

    //
    ;

    private final String value;

    Variable(String value) {
        this.value = value;
    }

    Variable(boolean legacy, String value) {
        this(legacy ? TextComponent.fromLegacyText(value.replace('&', ChatColor.COLOR_CHAR))
                : MiniMessageParser.parseFormat(value));
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
        return OraxenPlugin.get();
    }

}
