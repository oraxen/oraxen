package org.playuniverse.snowypine.command.permission;

import org.bukkit.command.CommandSender;
import org.playuniverse.snowypine.language.Message;
import org.playuniverse.snowypine.language.Variable;
import org.playuniverse.snowypine.utils.general.Placeholder;

public enum SnowypinePermissions implements ISnowyPermission {

	//
	// General Permissions
	//
	ALL,
	USE(ALL),
	IGNORE(ALL),
	
	// Command
	COMMAND_ALL(ALL),
	COMMAND_LANGUAGE(COMMAND_ALL),
	COMMAND_MODULE(COMMAND_ALL),

	//
	// END
	//
	;

	public static final String PERMISSION_FORMAT = "%s.%s";

	private final String module;
	private final SnowypinePermissions parent;

	private SnowypinePermissions() {
		this.module = "default";
		this.parent = null;
	}

	private SnowypinePermissions(SnowypinePermissions parent) {
		this.module = "default";
		this.parent = parent;
	}

	private SnowypinePermissions(String prefix) {
		this.module = prefix;
		this.parent = null;
	}

	private SnowypinePermissions(String prefix, SnowypinePermissions parent) {
		this.module = prefix;
		this.parent = parent;
	}

	/*
	 * IPermission implementation
	 */

	@Override
	public String module() {
		return module;
	}

	@Override
	public SnowypinePermissions parent() {
		return parent;
	}

	/*
	 * Permission check
	 */

	public boolean required(CommandSender sender) {
		if (!has(sender)) {
			Message.NO_PERMISSION.send(sender, Variable.PREFIX.placeholder(), getPlaceholder());
			return false;
		}
		return true;
	}

	public boolean required(CommandSender sender, Placeholder... placeholders) {
		if (!has(sender)) {
			Message.NO_PERMISSION.send(sender, placeholders, Variable.PREFIX.placeholder(), getPlaceholder());
			return false;
		}
		return true;
	}

	/*
	 * String functions
	 */

	@Override
	public String toString() {
		return asString();
	}

}
