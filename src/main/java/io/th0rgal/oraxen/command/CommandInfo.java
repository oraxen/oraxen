package io.th0rgal.oraxen.command;

import java.util.ArrayList;
import java.util.function.Function;

import org.bukkit.command.CommandSender;

import com.oraxen.chimerate.commons.command.dispatcher.Dispatcher;
import com.oraxen.chimerate.commons.command.tree.nodes.Literal.Builder;

import io.th0rgal.oraxen.event.command.OraxenCommandEvent;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.reflection.JavaTools;

import com.mojang.brigadier.tree.CommandNode;

public class CommandInfo {

    private final boolean mainNode;

    private final String name;
    private final String[] aliases;

    // Description
    private String simple = "";
    private String detailed = "";

    private Builder<CommandSender> builder;
    private CommandNode<CommandSender> node;

    public CommandInfo(String name, Function<CommandInfo, Builder<CommandSender>> function, String... aliases) {
        this(false, name, function, aliases);
    }

    public CommandInfo(boolean mainNode, String name, Function<CommandInfo, Builder<CommandSender>> function,
        String... aliases) {
        this.mainNode = mainNode;
        this.name = name.toLowerCase();
        this.aliases = Utils.toLowercase(aliases);
        this.builder = function.apply(this);
    }

    /*
     * 
     */

    public CommandInfo setDescription(String description) {
        this.simple = description;
        return this;
    }

    public CommandInfo setDetailedDescription(Iterable<String> description) {
        StringBuilder append = new StringBuilder();
        for (String desc : description) {
            append.append(desc);
            append.append('\n');
        }
        String output = append.toString();
        return setDetailedDescription(output.substring(0, output.length() - 1));
    }

    public CommandInfo setDetailedDescription(String... description) {
        StringBuilder append = new StringBuilder();
        for (String desc : description) {
            append.append(desc);
            append.append('\n');
        }
        String output = append.toString();
        return setDetailedDescription(output.substring(0, output.length() - 1));
    }

    public CommandInfo setDetailedDescription(String description) {
        this.detailed = description;
        return this;
    }

    /*
     * 
     */

    public String getSimpleDescriptionId() {
        return name + ".simple";
    }

    public String getDetailedDescriptionId() {
        return name + ".detailed";
    }

    /*
     * 
     */

    public String getSimpleDescription() {
        return simple;
    }

    public String getDetailedDescription() {
        return detailed;
    }

    /*
     * 
     */

    public boolean isMainNode() {
        return mainNode;
    }

    public String getName() {
        return name;
    }

    public String[] getAliases() {
        return aliases.clone();
    }

    public ArrayList<String> getAliasesAsList() {
        return JavaTools.asList(getAliases());
    }

    public Builder<CommandSender> getBuilder() {
        return builder;
    }

    public CommandNode<CommandSender> getNode() {
        return node;
    }

    /*
     * 
     */

    public final CommandInfo register(Dispatcher dispatcher) {
        if (!mainNode || node != null)
            return this;
        node = dispatcher.register(builder);
        return this;
    }

    public final OraxenCommandEvent register(OraxenCommandEvent event) {
        return event.add(this);
    }

}
