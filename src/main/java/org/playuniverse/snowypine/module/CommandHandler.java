package org.playuniverse.snowypine.module;

import java.util.Arrays;

import org.pf4j.PluginWrapper;
import org.playuniverse.snowypine.command.CommandInfo;
import org.playuniverse.snowypine.command.CommandProvider;

import com.syntaxphoenix.syntaxapi.command.CommandManager;

public class CommandHandler {

	private final CommandProvider provider;
	private final CommandManager manager;

	public CommandHandler(CommandProvider provider) {
		this.provider = provider;
		this.manager = provider.getManager();
	}

	/*
	 * Getter
	 */

	public CommandProvider getProvider() {
		return provider;
	}

	public CommandManager getManager() {
		return manager;
	}

	/*
	 * Command Loading
	 */

	public boolean register(Module module, ModuleCommand command) {
		ModuleBoundCommand boundCommand = new ModuleBoundCommand(module.getWrapper(), command);
		CommandInfo info;
		if ((info = command.buildInfo(boundCommand)) == null || info.getCommand() != boundCommand) {
			return false;
		}
		return provider.register(info);
	}

	/*
	 * Command Management
	 */

	public ModuleBoundCommand[] getCommands(PluginWrapper wrapper) {
		return getCommands(wrapper == null ? null : wrapper.getPluginId());
	}

	public ModuleBoundCommand[] getCommands(String pluginId) {
		if (pluginId == null) {
			return new ModuleBoundCommand[0];
		}
		return Arrays.stream(manager.getCommands()).filter(command -> command instanceof ModuleBoundCommand).map(command -> (ModuleBoundCommand) command)
			.filter(command -> command.getWrapper().getPluginId().equals(pluginId)).toArray(size -> new ModuleBoundCommand[size]);
	}

}
