
package io.th0rgal.oraxen.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import org.bukkit.command.CommandSender;

import com.syntaxphoenix.syntaxapi.command.CommandManager;
import com.syntaxphoenix.syntaxapi.utils.alias.Alias;

import io.th0rgal.oraxen.event.command.OraxenCommandEvent;
import io.th0rgal.oraxen.language.DescriptionType;
import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.general.Placeholder;
import io.th0rgal.oraxen.utils.reflection.JavaTools;

public class CommandInfo {

    private Alias alias;

    // Description
    private String usage = "";
    private String simple = "";
    private String detailed = "";

    private OraxenCommand command;

    public CommandInfo(String name, Function<CommandInfo, OraxenCommand> function, String... aliases) {
        this.alias = new Alias(name.toLowerCase(), Utils.toLowercase(aliases));
        this.command = function.apply(this);
    }

    public CommandInfo(String name, OraxenCommand command, String... aliases) {
        this.alias = new Alias(name.toLowerCase(), Utils.toLowercase(aliases));
        this.command = command;
    }

    /*
     * Management
     */

    public boolean has(String name) {
        if (!alias.hasAliases())
            return alias.getName().equals(name);
        return alias.getName().equals(name) || Arrays.stream(alias.getAliases()).anyMatch(alias -> alias.equals(name));
    }

    /*
     * Setter
     */

    public CommandInfo setUsage(String usage) {
        this.usage = usage;
        return this;
    }

    public CommandInfo setDescription(String description) {
        this.simple = description;
        return this;
    }

    public CommandInfo setDetailedDescription(Iterable<String> description) {
        return setDetailedDescription(String.join("\n", description));
    }

    public CommandInfo setDetailedDescription(String... description) {
        return setDetailedDescription(String.join("\n", description));
    }

    public CommandInfo setDetailedDescription(String description) {
        this.detailed = description;
        return this;
    }

    /*
     * Getter
     */

    public String getUsageId() {
        return alias.getName() + ".usage";
    }

    public String getSimpleDescriptionId() {
        return alias.getName() + ".simple";
    }

    public String getDetailedDescriptionId() {
        return alias.getName() + ".detailed";
    }

    public String getUsage() {
        return usage;
    }

    public String getSimpleDescription() {
        return simple;
    }

    public String getDetailedDescription() {
        return detailed;
    }

    public String getName() {
        return alias.getName();
    }

    public String[] getAliases() {
        return alias.getAliases();
    }

    public ArrayList<String> getAliasesAsList() {
        return JavaTools.asList(getAliases());
    }

    public OraxenCommand getCommand() {
        return command;
    }

    /*
     * Registration
     */

    public final boolean register(CommandManager manager) {
        ArrayList<String> aliases = manager.getClonedMap().hasConflict(alias);
        if (aliases.isEmpty()) {
            manager.register(command, alias);
            return true;
        }
        if (aliases.contains(alias.getName())) {
            manager.register(command, alias = alias.removeConflicts(aliases));
            return true;
        }
        return false;
    }

    public final OraxenCommandEvent register(OraxenCommandEvent event) {
        return event.add(this);
    }

    /*
     * Send message
     */

    public void sendSimple(CommandSender sender, String label) {
        Message.COMMAND_HELP_INFO_SHORT
            .send(sender, Placeholder.of("label", label), Placeholder.of("usage", this, DescriptionType.USAGE),
                Placeholder.of("description", this, DescriptionType.SIMPLE));
    }

}