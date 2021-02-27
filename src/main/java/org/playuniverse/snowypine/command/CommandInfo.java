
package org.playuniverse.snowypine.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import org.bukkit.command.CommandSender;
import org.playuniverse.snowypine.command.permission.IPermission;
import org.playuniverse.snowypine.helper.ListHelper;
import org.playuniverse.snowypine.language.DescriptionType;
import org.playuniverse.snowypine.language.LanguageProvider;
import org.playuniverse.snowypine.language.Message;
import org.playuniverse.snowypine.utils.general.Placeholder;
import org.playuniverse.snowypine.utils.reflection.JavaTools;

import com.syntaxphoenix.syntaxapi.command.CommandManager;
import com.syntaxphoenix.syntaxapi.utils.alias.Alias;

public class CommandInfo {

	private Alias alias;

	// Description
	private String usage = "";
	private String simple = "";
	private String detailed = "";

	// Permission
	private IPermission permission;

	private final Command command;

	public CommandInfo(String name, Function<CommandInfo, Command> function, String... aliases) {
		this.alias = new Alias(name.toLowerCase(), ListHelper.toLowerCase(aliases));
		this.command = function.apply(this);
	}

	public CommandInfo(String name, Command command, String... aliases) {
		this.alias = new Alias(name.toLowerCase(), ListHelper.toLowerCase(aliases));
		this.command = command;
	}

	/*
	 * Management
	 */

	public boolean has(String name) {
		if (!alias.hasAliases())
			return alias.getName().equals(name);
		return alias.getName().equals(name) || Arrays.asList(alias.getAliases()).contains(name);
	}

	/*
	 * Setter
	 */

	public CommandInfo setUsage(String usage) {
		this.usage = usage;
		return this;
	}

	public CommandInfo setPermission(IPermission permission) {
		this.permission = permission;
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

	public IPermission getPermission() {
		return permission;
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

	public Command getCommand() {
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

	/*
	 * Send message
	 */

	public void sendSimple(CommandSender sender, String label) {
		Message.COMMAND_HELP_INFO_SHORT.send(sender,
			Placeholder.of("content", Message.COMMAND_HELP_INFO_CONTENT.legacyMessage(LanguageProvider.getLanguageOf(sender), getContentPlaceholders(label))));
	}

	public Placeholder[] getContentPlaceholders(String label) {
		return new Placeholder[] {
				Placeholder.of("label", label),
				Placeholder.of("usage", this, DescriptionType.USAGE),
				Placeholder.of("description", this, DescriptionType.SIMPLE)
		};
	}

}