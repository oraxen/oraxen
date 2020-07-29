package io.th0rgal.oraxen.event.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.oraxen.chimerate.commons.command.dispatcher.Dispatcher;

import io.th0rgal.oraxen.command.CommandInfo;

public class OraxenCommandEvent extends Event {

    public static final HandlerList HANDLERS = new HandlerList();

    private final Dispatcher dispatcher;
    private final ArrayList<CommandInfo> infos = new ArrayList<>();
    private final ArrayList<String> aliases = new ArrayList<>();

    private final List<String> forbidden;

    public OraxenCommandEvent(Dispatcher dispatcher, String... forbidden) {
        this.dispatcher = dispatcher;
        this.forbidden = Arrays.asList(forbidden);
    }

    /*
     * 
     */

    public final OraxenCommandEvent add(CommandInfo info) {
        if (infos.contains(info))
            return this;
        if (!apply(info))
            return this;
        return this;
    }

    public final ArrayList<CommandInfo> getCommandInfos() {
        return infos;
    }

    public final ArrayList<String> getAliases() {
        return aliases;
    }

    /*
     * 
     */

    public boolean apply(CommandInfo info) {

        List<String> aliases = info.getAliasesAsList();
        aliases.add(info.getName());

        String[] copy = aliases.toArray(new String[0]);

        for (String alias : copy)
            if (this.aliases.contains(alias) || this.forbidden.contains(alias))
                aliases.remove(alias);

        if (!aliases.contains(info.getName()))
            return false;

        this.aliases.addAll(aliases);

        aliases.remove(info.getName());
        if (!aliases.isEmpty())
            info.getBuilder().alias(aliases.toArray(new String[0]));

        infos.add(info.register(dispatcher));

        return true;
    }

    /*
     * 
     */

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
