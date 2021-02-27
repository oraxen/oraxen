package org.playuniverse.snowypine.command.argument;

import java.util.Optional;

import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.playuniverse.snowypine.Snowypine;

import com.syntaxphoenix.syntaxapi.command.ArgumentType;
import com.syntaxphoenix.syntaxapi.command.BaseArgument;

import net.md_5.bungee.api.ChatColor;

public class ModuleHelper {

	private ModuleHelper() {}

	public static Optional<PluginWrapper> getModule(String name) {
		if (name == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(Snowypine.getPlugin().getPluginManager().getPlugin(name));
	}

	public static Optional<PluginWrapper> getModule(BaseArgument argument) {
		if (argument == null || argument.getType() != ArgumentType.STRING) {
			return Optional.empty();
		}
		return getModule(argument.asString().getValue());
	}

	public static ChatColor colorByState(PluginState state) {
		switch (state) {
		case STARTED:
			return ChatColor.GREEN;
		case RESOLVED:
		case CREATED:
			return ChatColor.YELLOW;
		case DISABLED:
		case STOPPED:
			return ChatColor.RED;
		case FAILED:
			return ChatColor.DARK_RED;
		default:
			return ChatColor.DARK_RED;
		}
	}

}
